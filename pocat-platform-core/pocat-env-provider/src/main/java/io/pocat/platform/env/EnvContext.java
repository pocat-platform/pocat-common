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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

public interface EnvContext {
    String getName();
    String getPath();

    boolean hasData();
    URL getDataUrl();
    default InputStream openDataStream() throws IOException {
        URL url = getDataUrl();
        if(url == null) {
            throw new IOException("Data not exist");
        }
        return url.openStream();
    }

    boolean hasChildren();
    Set<String> getChildrenNames();
    EnvContext getChild(String name);
    List<EnvContext> getDescendants();

    URL toURL();
}
