/*
 * Copyright 2024. dongobi soft inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.pocat.platform.util.executor;

import java.util.Map;
import java.util.concurrent.*;

public class ContextAwareExecutorService extends ThreadPoolExecutor {
    public ContextAwareExecutorService(int poolSize, BlockingQueue<Runnable> workQueue) {
        super(poolSize, poolSize, 0, TimeUnit.MILLISECONDS, workQueue);
        this.setThreadFactory(new ContextAwareThreadFactory(this));
    }

    @Override
    public void execute(Runnable runnable) {
        if (!(runnable instanceof ContextAwareFutureTask) &&!(runnable instanceof ContextAwareRunnableTask)) {
            runnable = new ContextAwareRunnableTask( runnable, ExecutorContext.getContextCopy());
        }

        super.execute(runnable);
    }

    public boolean isInExecutor(Thread thread) {
        if (thread instanceof ContextAwareWorkerThread) {
            return this == ((ContextAwareWorkerThread) thread).ownerExecutor;
        }
        return false;
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ContextAwareFutureTask<T>(runnable, value, ExecutorContext.getContextCopy());
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ContextAwareFutureTask<T>(callable, ExecutorContext.getContextCopy());
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        if (r instanceof ContextAwareFutureTask) {
            Map<String, Object> attrs = ((ContextAwareFutureTask<?>) r).getAttributes();
            ExecutorContext.setAttributes(attrs);
        } else if (r instanceof ContextAwareRunnableTask) {
            Map<String, Object> attrs = ((ContextAwareRunnableTask) r).getAttributes();
            ExecutorContext.setAttributes(attrs);
        } else {
            ExecutorContext.destroy();
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            super.afterExecute(r, t);
        } finally {
            ExecutorContext.destroy();
        }
    }

    private static class ContextAwareThreadFactory implements ThreadFactory {
        private final ExecutorService executor;

        public ContextAwareThreadFactory(ExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new ContextAwareWorkerThread(executor, r);
        }
    }

    private static class ContextAwareWorkerThread extends Thread {
        private final ExecutorService ownerExecutor;

        public ContextAwareWorkerThread(ExecutorService ownerExecutor, Runnable r) {
            super(r);
            this.ownerExecutor = ownerExecutor;
        }
    }

    private static class ContextAwareRunnableTask implements Runnable {
        private final Runnable command;
        private final Map<String, Object> attrs;

        public ContextAwareRunnableTask(Runnable command, Map<String, Object> attrs) {
            this.command = command;
            this.attrs = attrs;
        }

        @Override
        public void run() {
            command.run();
        }

        public Map<String, Object> getAttributes() {
            return attrs;
        }
    }

    private static class ContextAwareFutureTask<T> extends FutureTask<T> {
        private final Map<String, Object> attrs;

        public ContextAwareFutureTask(Runnable runnable, T value, Map<String, Object> attrs) {
            super(runnable, value);
            this.attrs = attrs;
        }

        public ContextAwareFutureTask(Callable<T> callable, Map<String, Object> attrs) {
            super(callable);
            this.attrs = attrs;
        }

        public Map<String, Object> getAttributes() {
            return this.attrs;
        }
    }
}
