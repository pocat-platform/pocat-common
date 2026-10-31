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

package io.pocat.platform.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class PlatformLauncher {
    private static final String BOOTSTRAP_CLASS_NAME = "io.pocat.platform.Bootstrapper";
    private static final String BOOTSTRAP_METHOD_NAME = "main";
    private static final String POCAT_HOME_PROP_NAME = "io.pocat.home";
    private static final String LIBS_DIR_PROP_NAME = "io.pocat.libs";
    private static final String DEFAULT_LIBS_DIR_NAME = "libs";

    private static final String JAR_FILE_EXTENSION = ".jar";

    public static void main(String[] args) throws Exception {

        try (URLClassLoader launcherClassLoader = new URLClassLoader(buildClassPathUrls())){
            Class<?> bootstrapClazz = Class.forName(BOOTSTRAP_CLASS_NAME, true, launcherClassLoader);
            Method bootMethod = bootstrapClazz.getMethod(BOOTSTRAP_METHOD_NAME, String[].class);

            bootMethod.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw new LaunchFailedException("Failed to bootstrap", e.getCause());
        } catch (Exception e) {
            throw new LaunchFailedException("Failed to launch platform", e);
        }
    }

    private static URL[] buildClassPathUrls() {
        File libDir = buildAbsolutePath(System.getProperties().getProperty(LIBS_DIR_PROP_NAME, DEFAULT_LIBS_DIR_NAME)).toFile();

        if(!libDir.isDirectory()) {
            throw new LaunchFailedException("Invalid library directory [" + libDir.getAbsolutePath() + "]");
        }
        File[] libFiles = libDir.listFiles(file -> file.isDirectory() || file.getName().toLowerCase(Locale.ROOT).endsWith(JAR_FILE_EXTENSION));
        if(libFiles == null || libFiles.length == 0) {
            throw new LaunchFailedException("No library files found in [" + libDir.getAbsolutePath() + "]");
        }

        return Arrays.stream(libFiles).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new LaunchFailedException("Invalid library file [" + file.getAbsolutePath() + "]", e);
            }
        }).toArray(URL[]::new);
    }

    private static Path buildAbsolutePath(String libPathStr) {
        Path libPath = Path.of(libPathStr);
        if(libPath.isAbsolute()) {
            return libPath;
        }
        return Path.of(System.getProperty(POCAT_HOME_PROP_NAME, findHome()), libPathStr).toAbsolutePath().normalize();
    }

    private static String findHome() {
        try {
            URL location = PlatformLauncher.class.getProtectionDomain().getCodeSource().getLocation();

            Path launcherPath = Path.of(location.toURI());
            // launcher.jar must be in ${POCAT_HOME}/bin directory
            return launcherPath.getParent().getParent().toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            throw new LaunchFailedException("Failed to determine platform home directory.", e);
        }
    }
}
