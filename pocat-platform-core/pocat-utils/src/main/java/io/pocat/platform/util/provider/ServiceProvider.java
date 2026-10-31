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

package io.pocat.platform.util.provider;

import java.util.*;
import java.util.function.Predicate;

public class ServiceProvider<T> {
    private final Class<T> type;
    private final ClassLoader classLoader;
    private volatile List<T> services;

    public static <T> ServiceProvider<T> of(Class<T> type) {
        return new ServiceProvider<>(type, Thread.currentThread().getContextClassLoader());
    }

    public static <T> ServiceProvider<T> of(Class<T> type, ClassLoader classLoader) {
        return new ServiceProvider<>(type, classLoader);
    }

    private ServiceProvider(Class<T> type, ClassLoader classLoader) {
        this.type = Objects.requireNonNull(type, "Type must not be null.");
        this.classLoader = classLoader;
    }

    public synchronized void reload() {
        ClassLoader loader = (this.classLoader != null) ? this.classLoader :
                             (Thread.currentThread().getContextClassLoader() != null) ? Thread.currentThread().getContextClassLoader() :
                             (this.type.getClassLoader() != null) ? this.type.getClassLoader() : ClassLoader.getSystemClassLoader();

        List<T> list = new ArrayList<>();

        ServiceLoader<T> serviceLoader = ServiceLoader.load(this.type, loader);
        for (T service : serviceLoader) {
            list.add(service);
        }

        this.services = Collections.unmodifiableList(list);
    }

    public List<T> getServices() {
        List<T> result = this.services;
        if(result == null) {
            synchronized (this) {
                result = this.services;
                if(result == null) {
                    reload();
                    result = this.services;
                }
            }
        }
        return result;
    }

    public Optional<T> findFirst(Predicate<T> predicate) {
        return getServices().stream().filter(Objects::nonNull).filter(predicate).findFirst();
    }
}

