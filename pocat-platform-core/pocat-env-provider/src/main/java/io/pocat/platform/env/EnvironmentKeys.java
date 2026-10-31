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

public final class EnvironmentKeys {
    public static final String ENVIRONMENT_FACTORY_CLASS_NAME_KEY = "io.pocat.env.provider.factory";
    public static final String NODE_TYPE_KEY = "io.pocat.platform.node.type";
    public static final String NODE_GROUP_NAME_KEY = "io.pocat.platform.node.group";

    public static final String NODE_CONTEXT_HOME_PATH = "/env/nodes";
    public static final String LOGGING_CONTEXT_HOME_PATH = "/env/logging";
    public static final String RESOURCE_CONTEXT_HOME_PATH = "/env/context/resources";
    public static final String SERVICE_CONTEXT_HOME_PATH = "/env/services";
    public static final String MESSAGE_BUS_CONTEXT_HOME_PATH = "/env/context/messagebus";
    public static final String XML_MIME_TYPE = "application/xml";
    public static final String LIBS_CONTEXT_HOME_PATH = "/env/libs";
    public static final String FILE_CONTEXT_HOME_PATH = "/env/files";

    private EnvironmentKeys() {

    }
}

