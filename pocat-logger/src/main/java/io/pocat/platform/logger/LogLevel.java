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

import java.util.logging.Level;

public class LogLevel extends Level {

    public static final LogLevel ERROR =
            new LogLevel("ERROR", Level.SEVERE.intValue() - 1);

    public static final LogLevel WARN =
            new LogLevel("WARN", Level.WARNING.intValue() - 1);

    public static final LogLevel INFO =
            new LogLevel("INFO", Level.INFO.intValue());

    public static final LogLevel DEBUG =
            new LogLevel("DEBUG", Level.CONFIG.intValue() - 1);

    public static final LogLevel TRACE =
            new LogLevel("TRACE", Level.FINEST.intValue());

    protected LogLevel(String name, int value) {
        super(name, value);
    }
}