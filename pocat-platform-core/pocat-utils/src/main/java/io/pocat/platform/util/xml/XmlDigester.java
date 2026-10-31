/*
 * Copyright 2024. dongobi soft inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.pocat.platform.util.xml;

import io.pocat.platform.util.xml.annotation.XmlAttribute;
import io.pocat.platform.util.xml.annotation.XmlElement;
import io.pocat.platform.util.xml.annotation.XmlElementWrapper;
import io.pocat.platform.util.xml.annotation.XmlTransient;
import io.pocat.platform.util.xml.exception.XmlDigesterException;
import io.pocat.platform.util.xml.exception.XmlMappingException;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XmlDigester {
    private final Map<Class<?>, List<TypeResolver<?>>> resolvers = new ConcurrentHashMap<>();
    private final Map<Class<?>, TypeConverter<?>> converters = new ConcurrentHashMap<>();
    private final Map<String, String> kebabCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    public static Builder newBuilder() {
        return new Builder();
    }

    private XmlDigester() {
        registerDefaultConverters();
    }

    public <T> T digest(String str, Class<T> type) {
        return digest(new InputSource(new StringReader(str)), type);
    }

    public <T> T digest(InputStream is, Class<T> type) {
        return digest(new InputSource(is), type);
    }

    public <T> T digest(InputSource source, Class<T> type) throws XmlDigesterException {
        Document doc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // prevent XXE Attack
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            doc = dbf.newDocumentBuilder().parse(source);
        } catch (Exception e) {
            throw new XmlDigesterException("XML Parsing Error: " + e.getMessage(), e);
        }

        try {
            //noinspection unchecked
            return (T) mapNode(doc.getDocumentElement(), type, "");
        } catch (Exception e) {
            throw new XmlDigesterException("XML Mapping Error: " + e.getMessage(), e);
        }
    }

    private void registerResolver(TypeResolver<?> resolver) {
        List<TypeResolver<?>> list = this.resolvers.computeIfAbsent(resolver.getTargetType(), k -> new ArrayList<>());
        if (list.contains(resolver)) return;
        list.add(resolver);
        list.sort(Comparator.<TypeResolver<?>>comparingInt(TypeResolver::getPriority)
                .thenComparing(r -> r.getClass().getName()));
    }

    private void registerConverter(TypeConverter<?> converter) {
        this.converters.put(wrapPrimitive(converter.getType()), converter);
    }

    private Object mapNode(Node node, Class<?> type, String path) {
        if (node == null) {
            return null;
        }

        Class<?> resolvedType = wrapPrimitive(resolveType(node, type));

        if(isSystemType(resolvedType)) {
            return null;
        }

        String currentPath = path + ">" + node.getNodeName();
        try {
            TypeConverter<?> converter = this.converters.get(resolvedType);
            if (converter != null) {
                return converter.convert(node);
            }

            if (resolvedType.isEnum()) {
                String text = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
                //noinspection unchecked,rawtypes
                return text.isEmpty() ? null : Enum.valueOf((Class<Enum>) resolvedType, text);
            }
        } catch (Exception e) {
            throw new XmlMappingException("Failed to convert ", currentPath, e);
        }

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            // Attribute node cannot be a complex type. So, map to null
            return null;
        }

        Object instance;
        try {
            Constructor<?> constructor = resolvedType.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (Exception e) {
            throw new XmlMappingException("Failed to instantiate class", currentPath, e);
        }

        for (Field f : getAllFields(resolvedType)) {
            if (Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(XmlTransient.class)) {
                continue;
            }

            f.setAccessible(true);

            try {
                if (f.isAnnotationPresent(XmlAttribute.class)) {
                    XmlAttribute anno = f.getAnnotation(XmlAttribute.class);
                    Node attrNode = node.getAttributes().getNamedItem(anno.value().isEmpty() ? f.getName() : anno.value());
                    if (attrNode != null) {
                        f.set(instance, mapNode(attrNode, f.getType(), currentPath));
                    }
                } else if (List.class.isAssignableFrom(f.getType())) {
                    f.set(instance, parseList(node, f, currentPath));
                } else if (Set.class.isAssignableFrom(f.getType())) {
                    f.set(instance, parseSet(node, f, currentPath));
                } else if (Map.class.isAssignableFrom(f.getType())) {
                    f.set(instance, parseMap(node, f, currentPath));
                } else if (f.getType().isArray()) {
                    f.set(instance, parseArray(node, f, currentPath));
                } else {
                    String name = getElementName(f);
                    Node target = findChild(node, name);
                    if (target != null) {
                        f.set(instance, mapNode(target, f.getType(), currentPath));
                    }
                }
            } catch (XmlMappingException e) {
                throw e;
            } catch (Exception e) {
                throw new XmlMappingException("Field mapping failed", currentPath, e);
            }
        }
        return instance;
    }

    private List<Object> parseList(Node node, Field f, String path) {
        Class<?> genericType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];

        XmlElementWrapper wrapperAnno = f.getAnnotation(XmlElementWrapper.class);
        Node wrapper = (wrapperAnno == null) ? node : findChild(node, wrapperAnno.value());

        if (wrapper == null) {
            return Collections.emptyList();
        }

        String name = getElementName(f);
        List<Object> list = new ArrayList<>();
        for (Node child : getChildElements(wrapper)) {
            if (child.getNodeName().equals(name) || child.getNodeName().equals(toKebabCase(name))) {
                list.add(mapNode(child, genericType, path));
            }
        }
        return list;
    }

    private Map<String, Object> parseMap(Node node, Field f, String path) {
        Class<?> valueType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[1];
        XmlElementWrapper wrapperAnno = f.getAnnotation(XmlElementWrapper.class);
        Node wrapper;
        if (wrapperAnno == null) {
            wrapper = findChild(node, f.getName());
            //throw new XmlMappingException("Map field must have @XmlElementWrapper", path);
        } else {
            wrapper = findChild(node, wrapperAnno.value());
        }

        if (wrapper == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        for (Node child : getChildElements(wrapper)) {
            map.put(child.getNodeName(), mapNode(child, valueType, path + " > " + child.getNodeName()));
        }
        return map;
    }

    private Set<Object> parseSet(Node node, Field f, String path) {
        Class<?> genericType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];

        XmlElementWrapper wrapperAnno = f.getAnnotation(XmlElementWrapper.class);
        Node wrapper = (wrapperAnno == null) ? node : findChild(node, wrapperAnno.value());

        if (wrapper == null) {
            return Collections.emptySet();
        }

        String name = getElementName(f);
        Set<Object> set = new HashSet<>();
        for (Node child : getChildElements(wrapper)) {
            if (child.getNodeName().equals(name) || child.getNodeName().equals(toKebabCase(name))) {
                set.add(mapNode(child, genericType, path));
            }
        }
        return set;
    }

    private Object parseArray(Node node, Field f, String path) {
        Class<?> componentType = f.getType().getComponentType();

        XmlElementWrapper wrapperAnno = f.getAnnotation(XmlElementWrapper.class);
        Node wrapper = (wrapperAnno == null) ? node : findChild(node, wrapperAnno.value());

        if (wrapper == null) {
            return Collections.emptyList();
        }

        String name = getElementName(f);
        List<Object> list = new ArrayList<>();
        for (Node child : getChildElements(wrapper)) {
            if (child.getNodeName().equals(name) || child.getNodeName().equals(toKebabCase(name))) {
                list.add(mapNode(child, componentType, path));
            }
        }

        if (list.isEmpty()) {
            return java.lang.reflect.Array.newInstance(componentType, 0);
        }

        Object array = java.lang.reflect.Array.newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            java.lang.reflect.Array.set(array, i, list.get(i));
        }
        return array;
    }

    private String getElementName(Field f) {
        XmlElement elemAnno = f.getAnnotation(XmlElement.class);
        String annoName = (elemAnno != null) ? elemAnno.value() : "";
        return annoName.isEmpty() ? f.getName() : annoName;
    }

    private Node findChild(Node parent, String name) {
        List<Node> children = getChildElements(parent);
        String kebab = toKebabCase(name);
        return children.stream()
                .filter(e -> e.getNodeName().equals(name) || e.getNodeName().equals(kebab))
                .findFirst().orElse(null);
    }

    private List<Node> getChildElements(Node parent) {
        List<Node> list = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                list.add(nl.item(i));
            }
        }
        return list;
    }

    private List<Field> getAllFields(Class<?> type) {
        return fieldCache.computeIfAbsent(type, t -> {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                fields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
            return fields;
        });
    }

    private Class<?> resolveType(Node node, Class<?> declaredType) {
        List<TypeResolver<?>> typeResolvers = this.resolvers.getOrDefault(declaredType, Collections.emptyList());
        for (TypeResolver<?> r : typeResolvers) {
            Class<?> resolved = r.resolve(node);
            if (resolved != null) return resolved;
        }
        return declaredType;
    }

    private String toKebabCase(String name) {
        return kebabCache.computeIfAbsent(name, k -> k.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase());
    }

    private boolean isSystemType(Class<?> t) {
        if(converters.containsKey(t)) {
            return false;
        }
        if (t.isArray()) {
            return false;
        }

        if (Collection.class.isAssignableFrom(t) || Map.class.isAssignableFrom(t)) {
            return false;
        }

        String pkg = t.getPackageName();
        return t.isPrimitive() || pkg.startsWith("java.") || pkg.startsWith("javax.");
    }

    private Class<?> wrapPrimitive(Class<?> t) {
        if (t == int.class) return Integer.class;
        if (t == boolean.class) return Boolean.class;
        if (t == long.class) return Long.class;
        if (t == double.class) return Double.class;
        if (t == float.class) return Float.class;
        if (t == byte.class) return Byte.class;
        if (t == char.class) return Character.class;
        if (t == short.class) return Short.class;
        return t;
    }

    private void registerDefaultConverters() {
        for (TypeConverter<?> converter : DefaultConverters.getConverters()) {
            registerConverter(converter);
        }
    }

    public static class Builder {
        private final List<TypeConverter<?>> customConverters = new ArrayList<>();
        private final List<TypeResolver<?>> customResolvers = new ArrayList<>();

        public Builder registerTypeConverter(TypeConverter<?> converter) {
            this.customConverters.add(converter);
            return this;
        }

        public Builder registerTypeResolver(TypeResolver<?> resolver) {
            this.customResolvers.add(resolver);
            return this;
        }

        public XmlDigester build() {
            XmlDigester digester = new XmlDigester();

            customConverters.forEach(digester::registerConverter);
            customResolvers.forEach(digester::registerResolver);

            return digester;
        }
    }
}
