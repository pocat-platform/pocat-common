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

package io.pocat.platform.util.clazz;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public final class ClassUtil {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    static {
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
    }

    private ClassUtil() {}
    public static Object createInstance(String className, Object... initArgs) {
        return createInstance(Thread.currentThread().getContextClassLoader(), className, initArgs);
    }

    public static Object createInstance(ClassLoader classLoader, String className, Object... initArgs) {
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);

            for (Constructor<?> constructor : clazz.getConstructors()) {
                Object[] convertedArgs = tryConvertArgs(constructor, initArgs);
                if (convertedArgs != null) {
                    return constructor.newInstance(convertedArgs);
                }
            }
        } catch (ClassNotFoundException | LinkageError e) {
            throw new InstantiationFailedException("Class not found: [" + className + "]. Please check for typos or missing dependencies.", e);
        } catch (InvocationTargetException e) {
            throw new InstantiationFailedException("An error occurred inside the constructor of class [" + className + "]: " + e.getCause().getMessage(), e.getCause());

        } catch (InstantiationException e) {
            throw new InstantiationFailedException("Cannot instantiate abstract class or interface: [" + className + "]. This class must be a concrete class.", e);

        } catch (IllegalAccessException e) {
            throw new InstantiationFailedException("Access denied to the constructor of class [" + className + "]. Please ensure the constructor is public.", e);

        } catch (Exception e) {
            throw new InstantiationFailedException("Unexpected reflection error while creating instance of [" + className + "].", e);
        }

        throw new InstantiationFailedException("No suitable constructor found for class [" + className + "] with the provided arguments.", null);
    }

    private static Object[] tryConvertArgs(Constructor<?> constructor, Object[] args) {
        Class<?>[] pTypes = constructor.getParameterTypes();

        if (!constructor.isVarArgs()) {
            if (pTypes.length != args.length) return null;
            for (int i = 0; i < pTypes.length; i++) {
                if (!isAssignable(pTypes[i], args[i])) return null;
            }
            return args;
        }

        if (args.length < pTypes.length - 1) return null;

        Object[] newArgs = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length - 1; i++) {
            if (!isAssignable(pTypes[i], args[i])) return null;
            newArgs[i] = args[i];
        }

        // 가변인자 부분(나머지 인자들)을 배열로 통합
        Class<?> varArgType = pTypes[pTypes.length - 1].getComponentType();
        int varArgLen = args.length - (pTypes.length - 1);
        if (varArgLen == 1 && args[args.length - 1] != null && args[args.length - 1].getClass().isArray()
                           && pTypes[pTypes.length - 1].isAssignableFrom(args[args.length - 1].getClass())) {
            newArgs[pTypes.length - 1] = args[args.length - 1];
        } else {
            Object varArgsArray = Array.newInstance(varArgType, varArgLen);

            for (int i = 0; i < varArgLen; i++) {
                Object val = args[pTypes.length - 1 + i];
                if (!isAssignable(varArgType, val)) {
                    return null;
                }
                Array.set(varArgsArray, i, val);
            }
            newArgs[pTypes.length - 1] = varArgsArray;
        }
        return newArgs;
    }

    private static boolean isAssignable(Class<?> target, Object arg) {
        if (arg == null) return !target.isPrimitive();
        if (target.isAssignableFrom(arg.getClass())) return true;
        return target.isPrimitive() && PRIMITIVE_TO_WRAPPER.get(target) == arg.getClass();
    }
}
