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

import io.pocat.resources.Resource;
import io.pocat.resources.ResourceConfig;
import io.pocat.resources.ResourceFactory;
import io.pocat.platform.util.provider.ServiceProvider;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceProvider {
    private static final ResourceProvider INSTANCE = new ResourceProvider();
    private final Map<ClassLoader, ServiceProvider<ResourceFactory>> providerCache = new ConcurrentHashMap<>();

    public static ResourceProvider getInstance() {
        return INSTANCE;
    }

    private ResourceProvider() {

    }

    public Resource<?> provideResource(ClassLoader providerClassLoader, ResourceConfig config) {
        Objects.requireNonNull(providerClassLoader, "Provider ClassLoader must not be null");
        Objects.requireNonNull(config, "Resource config must not  be null");

        ServiceProvider<ResourceFactory> factoryProvider = this.providerCache.computeIfAbsent(providerClassLoader, classLoader -> ServiceProvider.of(ResourceFactory.class, classLoader));

        ResourceFactory factory = factoryProvider.findFirst(resourceFactory -> resourceFactory.supports(config.getType()))
                .orElseThrow(() -> new ResourceNotFoundException("No ResourceFactory found for type [" + config.getType() + "]"));

        return factory.createResource(config);
    }

    public Resource<?> provideResource(ResourceConfig config) {
        return provideResource(Thread.currentThread().getContextClassLoader(), config);
    }

    public void evict(ClassLoader loader) {
        if (loader != null) {
            this.providerCache.remove(loader);
        }
    }
}
