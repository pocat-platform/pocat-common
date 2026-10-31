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

package io.pocat.platform.context.resource;

import io.pocat.platform.context.PlatformComponent;
import io.pocat.resources.Resource;
import io.pocat.resources.ResourceConfig;
import io.pocat.platform.env.EnvContext;
import io.pocat.platform.env.EnvironmentProvider;
import io.pocat.platform.util.xml.XmlDigester;
import io.pocat.platform.util.xml.annotation.XmlElementWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ResourceManager implements PlatformComponent {
    private static final String RESOURCE_HOME_PATH = "/env/context/resources";

    private final Map<String, ResourceNode<Resource<?>>> resourceRegistry = new ConcurrentHashMap<>();

    private final EnvironmentProvider provider;
    private final ClassLoader defaultClassLoader;
    private final XmlDigester digester;
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public ResourceManager(EnvironmentProvider provider, ClassLoader defaultClassLoader) {
        this.provider = provider;
        this.defaultClassLoader = defaultClassLoader;

        this.digester = XmlDigester.newBuilder().build();
    }

    public Resource<?> getResource(String resourceName) {
        if(!this.isDestroyed.get()) {
            this.readLock.lock();
            try {
                if (this.isDestroyed.get()) {
                    throw new IllegalStateException("Already destroyed");
                }

                ResourceNode<Resource<?>> candidate = new ResourceNode<>(new CompletableFuture<>());
                ResourceNode<Resource<?>> resourceNode = this.resourceRegistry.compute(resourceName, (name, current) ->{
                    if(current == null) {
                        return candidate;
                    } else {
                        current.refCount.incrementAndGet();
                        return current;
                    }
                });
                if(resourceNode == candidate) {
                    try {
                        resourceNode.future.complete(createResource(resourceName));
                    } catch (Exception e) {
                        resourceNode.future.completeExceptionally(e);
                    }
                }

                try {
                    Resource<?> resource = resourceNode.future.join();

                    return RefCountedResource.wrap(resource, () -> release(resourceName, resourceNode, false));
                } catch (CompletionException e) {
                    this.resourceRegistry.remove(resourceName, resourceNode);
                    filterException(e);
                }
            } finally {
                this.readLock.unlock();
            }
        } else {
            throw new IllegalStateException("Already destroyed");
        }
        throw new ResourceNotFoundException(resourceName);
    }

    private void filterException(CompletionException e) {
        if(e.getCause() != null) {
            if(e.getCause() instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException)e.getCause();
            }
            if(e.getCause() instanceof UncheckedIOException) {
                throw (UncheckedIOException)e.getCause();
                //throw ((UncheckedIOException)e.getCause()).getCause();
            }
            if(e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
        }
        throw e;
    }

    private Resource<?> createResource(String resourceName) throws ResourceNotFoundException {
        if(resourceName.startsWith("/")) {
            resourceName = resourceName.substring(1);
        }
        EnvContext resourceContext = this.provider.lookup(RESOURCE_HOME_PATH + "/" + resourceName);
        if(resourceContext == null) {
            throw new ResourceNotFoundException("Resource [" + resourceName + "] not found.");
        }
        ResourceConfig resourceConfig;
        try (InputStream resourceDescriptorStream = resourceContext.openDataStream()){
            resourceConfig = this.digester.digest(resourceDescriptorStream, ResourceConfigImpl.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResourceProvider.getInstance().provideResource(this.defaultClassLoader, resourceConfig);
    }

    private void release(String resourceName, final ResourceNode<Resource<?>> resourceNode, boolean forced) {
        AtomicReference<CompletableFuture<Resource<?>>> closeFutureRef = new AtomicReference<>();
        resourceRegistry.computeIfPresent(resourceName, (name, node) -> {
            if(node == resourceNode) {
                if (forced || node.refCount.decrementAndGet() == 0) {
                    closeFutureRef.set(node.future);
                    return null;
                }
            }
            return node;
        });

        CompletableFuture<Resource<?>> closeFuture = closeFutureRef.get();
        if(closeFuture != null) {
            closeFuture.thenAccept(Resource::close);
        }
    }

    @Override
    public void destroy() {
        if(this.isDestroyed.compareAndSet(false, true)) {
            this.writeLock.lock();
            try {
                for (String resourceName : resourceRegistry.keySet()) {
                    release(resourceName, resourceRegistry.get(resourceName), true);
                }
                this.resourceRegistry.clear();
            } finally {
                this.writeLock.unlock();
            }

        }
    }

    private static final class ResourceNode<R> {
        private final CompletableFuture<R> future;
        private final AtomicInteger refCount = new AtomicInteger(1);

        private ResourceNode(CompletableFuture<R> future) {
            this.future = future;
        }
    }

    private static final class RefCountedResource<T> implements Resource<T> {
        private final Resource<T> resource;
        private final Runnable onClose;
        private final AtomicBoolean isClosed = new AtomicBoolean(false);

        private RefCountedResource(Resource<T> resource, Runnable onClose) {
            this.resource = resource;
            this.onClose = onClose;
        }

        public static <T> Resource<T> wrap(Resource<T> resource, Runnable onClose) {
            return new RefCountedResource<>(resource, onClose);
        }

        @Override
        public String getName() {
            return this.resource.getName();
        }

        @Override
        public T getInstance() {
            if(this.isClosed.get()) {
                throw new IllegalStateException("Already closed");
            }
            return this.resource.getInstance();
        }

        @Override
        public void close() {
            if(this.isClosed.compareAndSet(false, true)) {
                this.onClose.run();
            }
        }
    }

    private static class ResourceConfigImpl implements ResourceConfig {
        private String name;
        private String type;
        @XmlElementWrapper(value ="properties")
        private Map<String, String> properties;

        @Override
        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public Map<String, String> getProperties() {
            return this.properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
}
