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

package io.pocat.platform.messagebus;

import io.pocat.platform.util.xml.XmlDigester;
import io.pocat.platform.util.xml.annotation.XmlElementWrapper;

import java.io.InputStream;
import java.util.Map;

public class ClusterConnectionProfile {
    private static final XmlDigester XML_DIGESTER = XmlDigester.newBuilder().build();

    private String name;
    private String type;
    @XmlElementWrapper("args")
    private Map<String, String> args;

    public static ClusterConnectionProfile parse(InputStream conStream) {
        return XML_DIGESTER.digest(conStream, ClusterConnectionProfile.class);
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, String> getArgs() {
        return this.args;
    }
}
