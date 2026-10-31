/*
 * Copyright 2024. dongobi soft inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.pocat.platform.logger;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LoggerFactory {
    private static final LoggerFactory INSTANCE = new LoggerFactory();

    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private LoggerContext loggerContext;

    private LoggerFactory() {

    }

    public static void configure(LoggerContext loggerContext) {
        if(loggerContext == null) {
            throw new IllegalArgumentException("Logger context must not be null.");
        }
        if (INSTANCE.loggerContext != null) {
            throw new IllegalStateException("LoggerFactory is already configured.");
        }
        INSTANCE.loggerContext = loggerContext;
        INSTANCE.loggerContext.watch(new LoggerContextWatcher(INSTANCE));
    }

    public static Logger getAnonymousLogger() {
        return new LoggerImpl(java.util.logging.Logger.getAnonymousLogger());
    }

    public static Logger getLogger(String loggerName) {
        return INSTANCE.loggers.computeIfAbsent(loggerName, INSTANCE::createLogger);
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    private Logger createLogger(String loggerName) {
        java.util.logging.Logger julLogger =
                java.util.logging.Logger.getLogger(loggerName);

        configureLogger(julLogger, loggerName);

        return new LoggerImpl(loggerName);
    }

    private void configureLogger(java.util.logging.Logger logger, String loggerName) {

        LoggerConfig config = this.loggerContext.getLoggerConfig(loggerName);

        if (config == null) {
            return;
        }

        logger.setLevel(parseLevel(config.getLevel()));
        logger.setUseParentHandlers(config.isUseParentHandler());

        for (String handlerName : config.getHandlers()) {
            HandlerConfig handlerConfig = this.loggerContext.getHandlerConfig(handlerName);

            if (handlerConfig == null) {
                continue;
            }

            java.util.logging.Handler handler = createHandler(handlerConfig);
            logger.addHandler(handler);
        }
    }

    private Level parseLevel(String level) {
        if (level == null) {
            return null;
        }

        switch (level.toUpperCase(Locale.ROOT)) {
            case "ERROR":
                return LogLevel.ERROR;
            case "WARN":
                return LogLevel.WARN;
            case "INFO":
                return LogLevel.INFO;
            case "DEBUG":
                return LogLevel.DEBUG;
            case "TRACE":
                return LogLevel.TRACE;
            default:
                return Level.parse(level);
        }
    }

    private java.util.logging.Handler createHandler(
            HandlerConfig config) {

        // 여기서 config.getType()에 따라 Handler 생성
        // config.getArgs()를 생성자/설정값으로 전달
        throw new UnsupportedOperationException("Unsupported handler type: " + config.getType());
    }
}
