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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileEnvProviderFactory implements EnvironmentProviderFactory {
    private static final String CONFIG_DIR_PATH_KEY = "io.pocat.env.config.dir";
    private static final String CONTEXT_DIR_PATH_KEY = "io.pocat.env.context.dir";
    private static final String LIBS_DIR_PATH_KEY = "io.pocat.env.libs.dir";
    private static final String SERVICE_HOME_DIR_PATH_KEY = "io.pocat.env.services.home";
    private static final String LOGGING_DIR_PATH_KEY = "io.pocat.env.logging.dir";
    private static final String ASSETS_HOME_DIR_PATH_KEY = "io.pocat.env.assets.home";

    private static final String ENV_ROOT = "/env";
    private static final String DEFAULT_CONFIG_DIR_NAME = "config";
    private static final String DEFAULT_CONTEXT_DIR_NAME = "contexts";
    private static final String DEFAULT_LIBS_DIR_NAME = "libs";
    private static final String DEFAULT_SERVICE_DIR_NAME = "services";
    private static final String DEFAULT_LOGGING_DIR_NAME = "logging";
    private static final String DEFAULT_ASSETS_DIR_NAME = "assets";

    private static final String PROP_NODE_NAME = "/env/config/props";

    private static final String HOME_PATH_ARG_KEY = "io.pocat.home";
    private static final String PATH_DELIMITER = "/";

    public FileEnvProviderFactory() {

    }

    @Override
    public EnvironmentProvider createProvider(Map<String, String> initEnv) {
        String homePathStr = initEnv.get(HOME_PATH_ARG_KEY);
        if(homePathStr == null) {
            throw new IllegalArgumentException(HOME_PATH_ARG_KEY + " is not set.");
        }


        Map<String, URL> nodes = loadEnvNodes(homePathStr, initEnv);
        Properties properties = new Properties();
        if(nodes.containsKey(PROP_NODE_NAME)) {
            URL url = nodes.remove(PROP_NODE_NAME);
            try (InputStream is = url.openStream()) {
                properties.load(is);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load properties");
            }
        }

        return new EnvironmentProviderImpl(nodes, properties, new FileWatcher());
    }

    private Map<String, URL> loadEnvNodes(String homePathStr, Map<String, String> initEnv) {
        // ${HOME_DIR}/config/node
        String configPathStr = initEnv.getOrDefault(CONFIG_DIR_PATH_KEY,
                homePathStr + File.separator + DEFAULT_CONFIG_DIR_NAME);
        // /env/config/gateway/${gateway_name}
        // /env/config/service/${service_node_name}
        // /env/config/gateway/${gateway_name}/deploy/${deploy_name}
        // /env/config/service/${service_node_name}/deploy/${deploy_name}
        Map<String, URL> configUrls = walkDir(ENV_ROOT + PATH_DELIMITER + DEFAULT_CONFIG_DIR_NAME, Path.of(configPathStr), true);

        Map<String, URL> nodes = new HashMap<>(configUrls);

        // ${HOME_DIR}/logging
        String logPathStr = initEnv.getOrDefault(LOGGING_DIR_PATH_KEY,
                homePathStr + File.separator + DEFAULT_LOGGING_DIR_NAME);

        // /env/logging/logger/${logger_name}
        // /env/logging/handler/${handle_rname}
        nodes.putAll(walkDir(ENV_ROOT + PATH_DELIMITER + DEFAULT_LOGGING_DIR_NAME, Path.of(logPathStr), true));

        // ${HOME_DIR}/context
        String contextPathStr = initEnv.getOrDefault(CONTEXT_DIR_PATH_KEY,
                homePathStr + File.separator + DEFAULT_CONTEXT_DIR_NAME);
        // /env/context/${context_path+}/${context_name}
        nodes.putAll(walkDir(ENV_ROOT + PATH_DELIMITER + DEFAULT_CONTEXT_DIR_NAME, Path.of(contextPathStr), true));

        // /env/libs/${lib_path+}/${lib_name}
        String libPathStr = initEnv.getOrDefault(LIBS_DIR_PATH_KEY, homePathStr + File.separator + DEFAULT_LIBS_DIR_NAME);
        nodes.putAll(walkDir(ENV_ROOT + PATH_DELIMITER + DEFAULT_LIBS_DIR_NAME, Path.of(libPathStr), false));

        // /env/assets/${asset_path+}/${asset_name}
        String assetsPathStr = initEnv.getOrDefault(ASSETS_HOME_DIR_PATH_KEY, homePathStr + File.separator + DEFAULT_ASSETS_DIR_NAME);
        nodes.putAll(walkDir(ENV_ROOT + PATH_DELIMITER + DEFAULT_ASSETS_DIR_NAME, Path.of(assetsPathStr), false));

        // /env/services/${service_path+}/${service_name}
        String servicePathStr = initEnv.getOrDefault(SERVICE_HOME_DIR_PATH_KEY, homePathStr + File.separator + DEFAULT_SERVICE_DIR_NAME);
        nodes.putAll(walkDir(ENV_ROOT + PATH_DELIMITER + DEFAULT_SERVICE_DIR_NAME, Path.of(servicePathStr), false));

        return nodes;
    }

    private Map<String, URL> walkDir(String rootNodeName, Path rootDirPath, boolean isRemoveExt) {
        if(!Files.isDirectory(rootDirPath)) {
            return Collections.emptyMap();
        }
        Map<String, URL> urlMap = new HashMap<>();
        try(Stream<Path> fileStream = Files.walk(rootDirPath)) {
            fileStream.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    URL url;
                    String nodePath = rootNodeName + PATH_DELIMITER + createFileNodePath(rootDirPath, path, isRemoveExt);

                    try {
                        url = path.toUri().toURL();
                    } catch (MalformedURLException e) {
                        // maybe not thrown.
                        throw new IllegalArgumentException("Invalid file path [" + path + "]");
                    }

                    urlMap.put(nodePath, url);
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read file tree [" + rootDirPath.toAbsolutePath() + "]", e);
        }
        return urlMap;
    }

    private String createFileNodePath(Path rootDirPath, Path path, boolean isRemoveExt) {
        Path relativePath = rootDirPath.relativize(path);
        String nodeName = path.getFileName().toString();
        if (isRemoveExt) {
            int index = nodeName.lastIndexOf(".");
            if (index != -1) {
                nodeName = nodeName.substring(0, index);
            }
        }

        if(relativePath.getParent() != null) {
            return relativePath.getParent().toString().replace(File.separator, PATH_DELIMITER) + PATH_DELIMITER + nodeName;
        } else {
            return nodeName;
        }
    }

    private static class FileWatcher implements ContextEventWatcher {

        @Override
        public void start() {

        }


        @Override
        public void stop() {

        }

        @Override
        public void setEventConsumer(Consumer<EnvContextEvent> eventConsumer) {

        }

        @Override
        public void watch(String path) {

        }

        @Override
        public void unwatch(String path) {

        }
    }
}
