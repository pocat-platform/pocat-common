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

import java.io.IOException;
import java.util.Map;

public final class InitialEnvProvider implements EnvironmentProvider {
    private final EnvironmentProvider defaultProvider;
    public InitialEnvProvider(Map<String, String> initEnv) {
        this.defaultProvider = Environments.getInstance().getDefaultProvider(initEnv);
        if(this.defaultProvider == null) {
            throw new IllegalStateException("Failed to create default provider");
        }
    }

    @Override
    public EnvContext lookup(String path) {
        return this.defaultProvider.lookup(path);
    }

    @Override
    public String getProperty(String propKey) {
        return this.defaultProvider.getProperty(propKey);
    }

    @Override
    public void watch(String path, EnvContextEventHandler watcher) throws IOException {
        this.defaultProvider.watch(path, watcher);
    }

    @Override
    public void unwatch(String path, EnvContextEventHandler watcher) throws IOException {
        this.defaultProvider.unwatch(path, watcher);
    }

    @Override
    public void close() {
        this.defaultProvider.close();
    }
}
