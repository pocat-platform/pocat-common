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
import java.io.InputStream;
import java.util.Optional;

public interface EnvironmentProvider {
    EnvContext lookup(String path);

    default InputStream openDataStream(String path) throws IOException {
        EnvContext ctx = lookup(path);
        if (ctx == null || !ctx.hasData()) {
            throw new IOException("Data not found [" + path + "]");
        }
        return ctx.openDataStream();
    }

    String getProperty(String propKey);
    default String getProperty(String propKey, String defaultValue) {
        return Optional.ofNullable(getProperty(propKey)).orElse(defaultValue);
    }

    void watch(String path, EnvContextEventHandler watcher) throws IOException;
    void unwatch(String path, EnvContextEventHandler watcher) throws IOException;

    void close();
}
