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
import java.net.URL;
import java.util.*;

class EnvironmentProviderImpl implements EnvironmentProvider {
    private final Map<String, EnvContextImpl> contexts;
    private final Properties properties;
    private final ContextEventWatcher watcher;

    public EnvironmentProviderImpl(Map<String, URL> contexts, Properties properties, ContextEventWatcher watcher) {
        this.contexts = buildContexts(contexts);
        this.properties = properties;
        this.watcher = watcher;
        this.watcher.setEventConsumer(this::handleEvent);
    }

    private void handleEvent(EnvContextEvent event) {
        event.getPath();
    }

    @Override
    public EnvContext lookup(String path) {
        return this.contexts.get(path);
    }

    @Override
    public String getProperty(String propKey) {
        return this.properties.getProperty(propKey);
    }

    @Override
    public void watch(String path, EnvContextEventHandler handler) throws IOException {
        if(this.contexts.get(path) == null) {
            throw new IllegalArgumentException("Context [" + path + "] does noe exist.");
        }
        this.watcher.watch(path);
    }

    @Override
    public void unwatch(String path, EnvContextEventHandler handler) throws IOException {
        this.watcher.unwatch(path);
    }

    @Override
    public void close() {
        this.watcher.stop();
    }

    private Map<String, EnvContextImpl> buildContexts(Map<String, URL> nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes must not be null.");
        }

        Map<String, EnvContextImpl> contexts = new HashMap<>();
        contexts.put("/", new EnvContextImpl("/", null));
        for(Map.Entry<String, URL> entry : nodes.entrySet()) {
            String path = normalizePath(entry.getKey());
            URL url = entry.getValue();

            EnvContextImpl context = contexts.get(path);
            if(context == null) {
                addNode(contexts, path, url);
            } else{
                context.setDataUrl(url);
            }
        }
        return contexts;
    }

    private String normalizePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null.");
        }

        path = path.trim();

        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be empty.");
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Path must start with '/': " + path);
        }

        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String[] segments = path.substring(1).split("/", -1);

        for (String segment : segments) {
            if (segment.isEmpty()) {
                throw new IllegalArgumentException(
                        "Path must not contain empty segments: " + path);
            }

            if (".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException(
                        "Path must not contain '.' or '..': " + path);
            }
        }

        return path;
    }

    private void addNode(Map<String, EnvContextImpl> contexts, String path, URL url) {
        EnvContextImpl node = contexts.get(path);
        if (node != null) {
            if (url != null) {
                node.setDataUrl(url);
            }
            return;
        }

        String parentPath = getParentPath(path);
        EnvContextImpl parent;
        if(parentPath == null) {
            parent = contexts.get("/");
        } else {
            parent = contexts.get(parentPath);
            if(parent == null) {
                addNode(contexts, parentPath, null);
                parent = contexts.get(parentPath);
            }
        }
        if(parent == null) {
            throw new IllegalStateException("Parent context not found for path: " + path);
        }
        node = new EnvContextImpl(path, url);
        parent.addChild(node);
        contexts.put(path, node);

    }

    private String getParentPath(String path) {
        if(path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or empty.");
        }

        if(path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length()-1);
        }

        int index = path.lastIndexOf("/");
        if(index == -1 || index == 0 || path.equals("/") ) {
            // root
            return null;
        }
        return path.substring(0, index);
    }

    private static class EnvContextImpl implements EnvContext {
        private final String path;
        private final String name;
        private final Map<String, EnvContextImpl> children = new HashMap<>();
        private final Set<String> unmodifiableChildren = Collections.unmodifiableSet(children.keySet());
        private URL url;

        public EnvContextImpl(String path, URL url) {
            this.path = path;
            this.url = url;
            this.name = createName(path);
        }

        private String createName(String path) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Path must not be null or empty.");
            }

            if ("/".equals(path)) {
                return "";
            }

            int index = path.lastIndexOf('/');
            return path.substring(index + 1);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getPath() {
            return this.path;
        }

        @Override
        public boolean hasData() {
            return this.url != null;
        }

        public void setDataUrl(URL url) {
            this.url = url;
        }

        @Override
        public URL getDataUrl() {
            return this.url;
        }

        @Override
        public boolean hasChildren() {
            return !this.children.isEmpty();
        }

        @Override
        public EnvContext getChild(String name) {
            return this.children.get(name);
        }

        @Override
        public Set<String> getChildrenNames() {
            return this.unmodifiableChildren;
        }

        @Override
        public List<EnvContext> getDescendants() {
            List<EnvContext> result = new ArrayList<>();
            for (EnvContextImpl child : this.children.values()) {
                collectChild(child, result);
            }
            return result;
        }

        @Override
        public URL toURL() {
            return null;
        }

        public void addChild(EnvContextImpl child) {
            this.children.put(child.getName(), child);
        }

        private void collectChild(EnvContextImpl context, List<EnvContext> result) {
            result.add(context);
            for (EnvContextImpl child : context.children.values()) {
                collectChild(child, result);
            }
        }


    }
}
