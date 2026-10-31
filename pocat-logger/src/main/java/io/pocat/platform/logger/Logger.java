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

public interface Logger {

    boolean isTraceEnabled();
    boolean isDebugEnabled();
    boolean isInfoEnabled();
    boolean isWarnEnabled();
    boolean isErrorEnabled();

    void trace(String message);
    void trace(Supplier<String> message);
    void trace(String message, Throwable throwable);
    void trace(Supplier<String> message, Throwable throwable);

    void debug(String message);
    void debug(Supplier<String> message);
    void debug(String message, Throwable throwable);
    void debug(Supplier<String> message, Throwable throwable);

    void info(String message);
    void info(Supplier<String> message);
    void info(String message, Throwable throwable);
    void info(Supplier<String> message, Throwable throwable);

    void warn(String message);
    void warn(Supplier<String> message);
    void warn(String message, Throwable throwable);
    void warn(Supplier<String> message, Throwable throwable);

    void error(String message);
    void error(Supplier<String> message);
    void error(String message, Throwable throwable);
    void error(Supplier<String> message, Throwable throwable);
}
