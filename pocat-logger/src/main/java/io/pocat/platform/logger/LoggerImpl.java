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

import java.util.function.Supplier;

class LoggerImpl implements Logger {
    private final java.util.logging.Logger julLogger;

    public LoggerImpl(String loggerName) {
        this.julLogger = java.util.logging.Logger.getLogger(loggerName);
    }

    public LoggerImpl(java.util.logging.Logger julLogger) {
        this.julLogger = julLogger;
    }

    @Override
    public boolean isTraceEnabled() {
        return julLogger.isLoggable(LogLevel.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return julLogger.isLoggable(LogLevel.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return julLogger.isLoggable(LogLevel.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return julLogger.isLoggable(LogLevel.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return julLogger.isLoggable(LogLevel.ERROR);
    }

    @Override
    public void trace(String message) {
        log(LogLevel.TRACE, message);
    }

    @Override
    public void trace(Supplier<String> supplier) {
        log(LogLevel.TRACE, supplier);
    }

    @Override
    public void trace(String message, Throwable e) {
        log(LogLevel.TRACE, message, e);
    }

    @Override
    public void trace(Supplier<String> supplier, Throwable e) {
        log(LogLevel.TRACE, supplier, e);
    }

    @Override
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    @Override
    public void debug(Supplier<String> supplier) {
        log(LogLevel.DEBUG, supplier);
    }

    @Override
    public void debug(String message, Throwable e) {
        log(LogLevel.DEBUG, message, e);
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable e) {
        log(LogLevel.DEBUG, supplier, e);
    }

    @Override
    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    @Override
    public void info(Supplier<String> supplier) {
        log(LogLevel.INFO, supplier);
    }

    @Override
    public void info(String message, Throwable e) {
        log(LogLevel.INFO, message, e);
    }

    @Override
    public void info(Supplier<String> supplier, Throwable e) {
        log(LogLevel.INFO, supplier, e);
    }

    @Override
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    @Override
    public void warn(Supplier<String> supplier) {
        log(LogLevel.WARN, supplier);
    }

    @Override
    public void warn(String message, Throwable e) {
        log(LogLevel.WARN, message, e);
    }

    @Override
    public void warn(Supplier<String> supplier, Throwable e) {
        log(LogLevel.WARN, supplier, e);
    }

    @Override
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    @Override
    public void error(Supplier<String> supplier) {
        log(LogLevel.ERROR, supplier);
    }

    @Override
    public void error(String message, Throwable e) {
        log(LogLevel.ERROR, message, e);
    }

    @Override
    public void error(Supplier<String> supplier, Throwable e) {
        log(LogLevel.ERROR, supplier, e);
    }

    private void log(LogLevel level, String message) {
        this.julLogger.log(level, message);
    }

    private void log(LogLevel level, String message, Throwable e) {
        this.julLogger.log(level, message, e);
    }

    public void log(LogLevel level, Supplier<String> supplier) {
        this.julLogger.log(level, supplier);
    }

    public void log(LogLevel level, Supplier<String> supplier, Throwable e) {
        this.julLogger.log(level, e, supplier);
    }
}

