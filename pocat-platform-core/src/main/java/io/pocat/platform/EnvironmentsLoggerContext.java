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

package io.pocat.platform;

import io.pocat.platform.env.EnvironmentProvider;
import io.pocat.platform.logger.HandlerConfig;
import io.pocat.platform.logger.LoggerConfig;
import io.pocat.platform.logger.LoggerContext;
import io.pocat.platform.logger.LoggerContextWatcher;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentsLoggerContext implements LoggerContext {
    private EnvironmentProvider provider;
    private Map<String, LoggerConfig> loggerConfigs = new HashMap<>();
    private Map<String, HandlerConfig> handlerConfigs = new HashMap<>();

    public EnvironmentsLoggerContext(EnvironmentProvider provider) {
        this.provider = provider;
    }

    @Override
    public LoggerConfig getLoggerConfig(String loggerName) {
        return null;
    }

    @Override
    public HandlerConfig getHandlerConfig(String handlerName) {
        return null;
    }

    @Override
    public void watch(LoggerContextWatcher loggerContextWatcher) {

    }
}
