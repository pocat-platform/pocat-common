/*
 * Copyright 2024. dongobi soft inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pocat.platform.util.timer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public final class ExpiringMap<K, V> {
    private static final AtomicInteger INSTANCE_ID = new AtomicInteger(0);

    private final ConcurrentMap<K, Entry<K, V>> entries = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor scheduler;
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lifecycleLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lifecycleLock.writeLock();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final BiConsumer<? super K, ? super V> defaultTimeoutHandler;

    public ExpiringMap() {
        this(null);
    }

    public ExpiringMap(BiConsumer<? super K, ? super V> defaultTimeoutHandler) {
        this.defaultTimeoutHandler = defaultTimeoutHandler == null? (k, v) -> { } : defaultTimeoutHandler;
        this.scheduler = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "expiring-map-timeout-" + (INSTANCE_ID.getAndIncrement()%Integer.MAX_VALUE));
            thread.setDaemon(true);
            return thread;
        });

        this.scheduler.setRemoveOnCancelPolicy(true);
        this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public void put(K key, V value, Duration expiredIn) {
        put(key, value, expiredIn, this.defaultTimeoutHandler);
    }

    public void put(K key, V value, Duration expiredIn, BiConsumer<? super K, ? super V> timeoutHandler) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expiredIn, "expiredIn");
        Objects.requireNonNull(timeoutHandler, "timeoutHandler");

        this.readLock.lock();
        try {
            checkClosed();

            final long timeoutNanos;

            try {
                timeoutNanos = expiredIn.toNanos();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Timeout is too large", e);
            }

            if (timeoutNanos <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }

            this.entries.compute(key, (k, previous) -> {
                Entry<K, V> next = new Entry<>(value, System.nanoTime(), timeoutNanos, timeoutHandler);

                ScheduledFuture<?> future = this.scheduler.schedule(() -> expire(k, next), timeoutNanos, TimeUnit.NANOSECONDS);

                next.setTimeout(future);

                if (previous != null) {
                    previous.cancel();
                }

                return next;
            });
        } finally {
            this.readLock.unlock();
        }
    }


    public V get(K key) {
        this.readLock.lock();
        try {
            checkClosed();
            Entry<K, V> entry = this.entries.get(key);

            if (entry == null) {
                return null;
            }

            if (entry.isExpired()) {
                expire(key, entry);
                return null;
            }

            return entry.value;
        } finally {
            this.readLock.unlock();
        }
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V remove(K key) {
        this.readLock.lock();
        try {
            checkClosed();
            Objects.requireNonNull(key, "key");

            Entry<K, V> entry = this.entries.remove(key);

            if (entry == null) {
                return null;
            }

            entry.cancel();
            return entry.value;
        } finally {
            this.readLock.unlock();
        }
    }

    public boolean remove(K key, V expectedValue) {
        this.readLock.lock();
        try {
            checkClosed();
            Objects.requireNonNull(key, "key");

            AtomicReference<Entry<K, V>> removed = new AtomicReference<>();

            this.entries.computeIfPresent(key, (k, entry) -> {
                if (Objects.equals(entry.value, expectedValue)) {
                    removed.set(entry);
                    return null;
                }

                return entry;
            });

            Entry<K, V> entry = removed.get();

            if (entry == null) {
                return false;
            }

            entry.cancel();
            return true;
        } finally {
            this.readLock.unlock();
        }
    }

    public void close() {
        this.writeLock.lock();
        try {
            if (this.isClosed.compareAndSet(false, true)) {
                for (Entry<K, V> entry : this.entries.values()) {
                    entry.cancel();
                }

                this.entries.clear();
                this.scheduler.shutdownNow();
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    private void expire(K key, Entry<K, V> entry) {
        this.readLock.lock();
        try {
            if (!this.entries.remove(key, entry)) {
                entry.cancel();
                return;
            }

            entry.cancel();
            entry.onExpire.accept(key, entry.value);
        } finally {
            this.readLock.unlock();
        }
    }

    private void checkClosed() {
        if(this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }
    }

    private static final class Entry<K, V> {
        private final V value;
        private final long createdAtNanos;
        private final long timeoutNanos;
        private final BiConsumer<? super K, ? super V> onExpire;

        private final AtomicBoolean active = new AtomicBoolean(true);

        private volatile ScheduledFuture<?> timeoutFuture;

        private Entry(V value, long createdAtNanos, long timeoutNanos, BiConsumer<? super K, ? super V> onExpire) {
            this.value = value;
            this.createdAtNanos = createdAtNanos;
            this.timeoutNanos = timeoutNanos;
            this.onExpire = onExpire;
        }

        private boolean isExpired() {
            return System.nanoTime() - this.createdAtNanos >= this.timeoutNanos;
        }

        private void setTimeout(ScheduledFuture<?> future) {
            this.timeoutFuture = future;

            if (!this.active.get()) {
                future.cancel(false);
            }
        }

        private void cancel() {
            this.active.set(false);

            ScheduledFuture<?> future = timeoutFuture;

            if (future != null) {
                future.cancel(false);
            }
        }
    }
}
