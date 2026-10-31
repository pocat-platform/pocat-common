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

package io.pocat.platform.context.assets;

import io.pocat.platform.context.PlatformComponent;
import io.pocat.platform.env.EnvContext;
import io.pocat.platform.env.EnvironmentProvider;

import java.io.IOException;
import java.io.InputStream;

public class PlatformAssetManager implements PlatformComponent {
    private final EnvironmentProvider envProvider;
    private final String assetHomePath;

    public PlatformAssetManager(EnvironmentProvider envProvider, String assetHomePath) {
        this.envProvider = envProvider;
        EnvContext assetHomeCtx = envProvider.lookup(assetHomePath);
        if(assetHomeCtx == null) {
            throw new IllegalArgumentException("Invalid asset home [" + assetHomePath + "]");
        }
        this.assetHomePath = assetHomePath;
    }

    public InputStream openAssetStream(String path) throws IOException {
        String assetPath = path;
        if(assetPath.startsWith("/")) {
            assetPath = this.assetHomePath.substring(1);
        }
        EnvContext assetCtx = this.envProvider.lookup(assetPath);
        if(assetCtx == null || !assetCtx.hasData()) {
            throw new IllegalArgumentException("Asset [" + assetPath + "] not found");
        }
        return assetCtx.openDataStream();
    }

    @Override
    public void destroy() {

    }
}
