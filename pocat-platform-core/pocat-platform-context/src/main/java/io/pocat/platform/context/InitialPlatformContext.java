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

package io.pocat.platform.context;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class InitialPlatformContext implements PlatformContext {
    private final PlatformContext defaultPlatformCtx;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public InitialPlatformContext() {
        this.defaultPlatformCtx = PlatformEnvironments.getPlatformContext();
    }

    @Override
    public <T extends PlatformComponent> T lookup(Class<T> type) {
        checkClosed();
        return this.defaultPlatformCtx.lookup(type);
    }

    @Override
    public Map<String, String> getContextProps() {
        checkClosed();
        return this.defaultPlatformCtx.getContextProps();
    }

    @Override
    public void destroy() {
        this.isClosed.compareAndSet(false, true);
    }

    private void checkClosed() {
        if(this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }
    }


}
