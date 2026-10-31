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

package io.pocat.platform.env;

import io.pocat.platform.util.clazz.ClassUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class Environments {
    private static final Environments INSTANCE = new Environments();
    private static final String FACTORY_CLASS_NAME_KEY = "io.pocat.env.provider.factory";

    private final Map<Map<String, String>, EnvironmentProviderRef> providers = new HashMap<>();

    public static Environments getInstance() {
        return INSTANCE;
    }

    private Environments() {

    }

    public EnvironmentProvider getDefaultProvider(Map<String, String> initEnv) {
        return this.providers.compute(initEnv, (map, ref)->{
            if(ref == null) {
                return new EnvironmentProviderRef(createDefaultProvider(initEnv));
            } else {
                ref.refCount.incrementAndGet();
                return ref;
            }
        });
    }

    private EnvironmentProvider createDefaultProvider(Map<String, String> initEnv) {
        String providerFactoryClassName = initEnv.getOrDefault(FACTORY_CLASS_NAME_KEY, FileEnvProviderFactory.class.getName());
        if(providerFactoryClassName == null) {
            throw new IllegalArgumentException("Environment provider factory is not set. Set -D" + FACTORY_CLASS_NAME_KEY + ".");
        }
        EnvironmentProviderFactory factory = (EnvironmentProviderFactory) ClassUtil.createInstance(providerFactoryClassName);
        return factory.createProvider(initEnv);
    }

    private static class EnvironmentProviderRef implements EnvironmentProvider {
        private final AtomicInteger refCount = new AtomicInteger(1);
        private final EnvironmentProvider provider;

        public EnvironmentProviderRef(EnvironmentProvider provider) {
            this.provider = provider;
        }

        @Override
        public EnvContext lookup(String path) {
            return this.provider.lookup(path);
        }

        @Override
        public String getProperty(String propKey) {
            return this.provider.getProperty(propKey);
        }

        @Override
        public void watch(String path, EnvContextEventHandler watcher) throws IOException {
            this.provider.watch(path, watcher);
        }

        @Override
        public void unwatch(String path, EnvContextEventHandler watcher) throws IOException {
            this.provider.unwatch(path, watcher);
        }

        @Override
        public void close() {
            if(0 == this.refCount.decrementAndGet()) {
                this.provider.close();
            }
        }
    }
}