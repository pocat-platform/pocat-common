/*
 * Copyright 2024. dongobi soft inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.pocat.platform.util.executor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExecutorContext {
    private ExecutorContext() {

    }

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_ATTRS = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static void setAttribute(String attrName, Object attribute) {
        THREAD_LOCAL_ATTRS.get().put(attrName, attribute);
    }

    public static <T> void setAttribute(Class<T> attrType, T attribute) {
        THREAD_LOCAL_ATTRS.get().put(attrType.getName(), attribute);
    }

    public static void setAttributes(Map<String, Object> attributes) {
        THREAD_LOCAL_ATTRS.set(attributes);
    }

    public static Object getAttribute(String attrName) {
        return THREAD_LOCAL_ATTRS.get().get(attrName);
    }

    public static <T> T getAttribute(Class<T> type) {
        return type.cast(THREAD_LOCAL_ATTRS.get().get(type.getName()));
    }

    public static Object removeAttribute(String key) {
        return THREAD_LOCAL_ATTRS.get().remove(key);
    }

    public static void removeAttributes() {
        THREAD_LOCAL_ATTRS.get().clear();
    }

    public static Map<String, Object> getContextCopy() {
        return new ConcurrentHashMap<>(THREAD_LOCAL_ATTRS.get());
    }

    public static void destroy() {
        THREAD_LOCAL_ATTRS.remove();
    }
}
