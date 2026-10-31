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

import io.pocat.platform.util.xml.XmlDigester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConfigParserTest {
    @Test
    public void parseHandlerConfig() {
        XmlDigester digester = XmlDigester.newBuilder().build();
        HandlerConfig handlerConfig = digester.digest(getClass().getResourceAsStream("/handler.xml"), HandlerConfig.class);
        assertEquals("console", handlerConfig.getName());
        assertEquals("CONSOLE", handlerConfig.getType());
        assertEquals("value", handlerConfig.getArgs().get("key"));
    }

    @Test
    public void parseLoggerConfig() {
        XmlDigester digester = XmlDigester.newBuilder().build();
        LoggerConfig loggerConfig = digester.digest(getClass().getResourceAsStream("/logger.xml"), LoggerConfig.class);
        assertEquals("io.pocat", loggerConfig.getName());
        assertEquals("INFO", loggerConfig.getLevel());
        assertFalse(loggerConfig.isUseParentHandler());
        assertEquals("console", loggerConfig.getHandlers().get(0));
    }
}
