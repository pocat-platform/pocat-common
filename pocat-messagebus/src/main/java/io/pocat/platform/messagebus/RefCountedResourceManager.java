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

package io.pocat.platform.messagebus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class RefCountedResourceManager<C extends AutoCloseable> {
    private final Map<String, ResourceRef<C>> resources = new ConcurrentHashMap<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
    private final BiFunction<C, Runnable, C> resourceWrapper;

    protected RefCountedResourceManager(BiFunction<C, Runnable, C> resourceWrapper) {
        this.resourceWrapper = Objects.requireNonNull(resourceWrapper, "Resource wrapper must not be null.");
    }

    public C get(String resourceName, Supplier<C> resourceFactory) {
        Objects.requireNonNull(resourceFactory, "Resource factory must not be null.");
        this.readLock.lock();
        try {
            checkClosed();
            ResourceRef<C> resourceRef = this.resources.computeIfPresent(resourceName, (name, ref) -> {
                ref.getRefCount().incrementAndGet();
                return ref;
            });
            if (resourceRef == null) {
                checkClosed();
                ResourceRef<C> candidate = new ResourceRef<>();
                resourceRef = this.resources.compute(resourceName, (name, current) -> {
                    if (current == null) {
                        return candidate;
                    } else {
                        current.refCount.incrementAndGet();
                        return current;
                    }
                });

                if (candidate == resourceRef) {
                    try {
                        resourceRef.getFuture().complete(resourceFactory.get());
                    } catch (Throwable t) {
                        this.resources.remove(resourceName, resourceRef);
                        resourceRef.getFuture().completeExceptionally(t);
                    }
                }
            }

            C resource = resourceRef.getFuture().join();
            return this.resourceWrapper.apply(resource, () -> release(resourceName));

        } finally {
            this.readLock.unlock();
        }
    }

    public void release(String resourceName) {
        AtomicReference<ResourceRef<C>> closeRef = new AtomicReference<>();
        this.resources.computeIfPresent(resourceName, (name, currentRef) -> {
            if (!this.isClosed.get()) {
                if (currentRef.getRefCount().decrementAndGet() == 0) {
                    this.writeLock.lock();
                    try {
                        if (!this.isClosed.get() && currentRef.getRefCount().get() == 0) {
                            closeRef.set(currentRef);
                            return null;
                        }
                    } finally {
                        this.writeLock.unlock();
                    }
                }
            }
            return currentRef;
        });

        ResourceRef<C> currentRef = closeRef.get();
        if (currentRef != null) {
            try {
                currentRef.getFuture().join().close();
            } catch (Exception e) {
                // ignored. will not throw
            }
        }
    }

    public void close() {
        if (this.isClosed.compareAndSet(false, true)) {
            List<ResourceRef<C>> refs;
            this.writeLock.lock();
            try {
                refs = new ArrayList<>(this.resources.values());
                this.resources.clear();
            } finally {
                this.writeLock.unlock();
            }

            for (ResourceRef<C> ref : refs) {
                try {
                    ref.getFuture().join().close();
                } catch (Exception e) {
                    // ignored. not throw
                }
            }
        }
    }

    private void checkClosed() {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }
    }

    private static class ResourceRef<C> {
        private final CompletableFuture<C> future = new CompletableFuture<>();
        private final AtomicInteger refCount = new AtomicInteger(1);

        public CompletableFuture<C> getFuture() {
            return this.future;
        }

        public AtomicInteger getRefCount() {
            return this.refCount;
        }
    }

    protected static abstract class ResourceWrapper<C> implements AutoCloseable {
        private final C resource;
        private final Runnable releaser;
        private final AtomicBoolean isClosed = new AtomicBoolean(false);

        public ResourceWrapper(C resource, Runnable releaser) {
            this.resource = resource;
            this.releaser = releaser;
        }

        protected final C getResource() {
            return this.resource;
        }

        @Override
        public final void close() {
            if (this.isClosed.compareAndSet(false, true)) {
                this.releaser.run();
            }
        }
    }
}
