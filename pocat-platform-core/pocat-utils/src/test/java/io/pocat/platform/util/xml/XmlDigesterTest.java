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
import io.pocat.platform.util.xml.exception.XmlDigesterException;
import org.junit.Before;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Node;

public class XmlDigesterTest {

    private XmlDigester digester;

    @Before
    public void setUp() {
        // 빌더 패턴을 사용하여 생성
        digester = XmlDigester.newBuilder().build();
    }

    // --- [테스트용 DTO 및 지원 클래스들 (Inner Classes)] ---

    public static class UserDTO {
        public String name;
        public int age;
        public String description;
    }

    public static class ProjectDTO {
        @XmlElement(value = "display-name")
        public String displayName;
    }

    public static class VersionDTO {
        @XmlAttribute
        public int id;
        @XmlAttribute
        public String version;
    }

    public enum Status { ACTIVE, INACTIVE }
    public static class EnumDTO {
        public Status status;
    }

    public static class ListDTO {
        @XmlElementWrapper(value = "tags")
        @XmlElement(value = "tag")
        public List<String> tags;

        @XmlElement(value = "item")
        public List<String> items;
    }

    public static class MapDTO {
        @XmlElementWrapper(value = "props")
        public Map<String, String> props;
    }

    // 다형성 테스트를 위한 클래스 구조
    public static class ShapeContainer { public Shape shape; }
    public static abstract class Shape {}
    public static class Circle extends Shape { public int radius; }

    public static class ShapeResolver implements TypeResolver<Shape> {
        @Override
        public Class<? extends Shape> resolve(Node node) {
            String type = node.getAttributes().getNamedItem("type").getNodeValue();
            if ("circle".equals(type)) return Circle.class;
            return null;
        }
        @Override public Class<Shape> getTargetType() { return Shape.class; }
        @Override public int getPriority() { return 0; }
    }

    // 커스텀 변환기
    public static class MoneyDTO { public BigDecimal price; }
    public static class MoneyConverter implements TypeConverter<BigDecimal> {
        @Override
        public BigDecimal convert(Node node) {
            return new BigDecimal(node.getTextContent().trim());
        }
        @Override public Class<BigDecimal> getType() { return BigDecimal.class; }
    }

    public static class ParentDTO { @XmlElement(value = "parent-name") public String parentName; }
    public static class ChildDTO extends ParentDTO {
        @XmlAttribute
        public int id;
        @XmlElement(value = "child-name") public String childName;
    }

    public static class NodeDTO {
        @XmlAttribute
        public String name;
        public NodeDTO child;
        public Status status; // 경로 테스트용
    }

    public static class UserNameDTO {
        @XmlAttribute public int id;
        @XmlElement(value = "user-name") public String userName;
    }

    public static class ConflictDTO {
        @XmlAttribute(value = "code") public String attrCode;
        @XmlElement(value = "code-val") public String elemCode;
    }

    public static class InvalidAttrListDTO { @XmlAttribute public List<String> items; }
    public static class InvalidAttrComplexDTO { @XmlAttribute public UserDTO complex; }

    public static class DuplicatePropertyDTO {
        @XmlElement(value = "value") public String first;
        @XmlElement(value = "value") public String second;
    }

    // --- [테스트 케이스 시작] ---

    @Test
    public void testBasicElementMapping() throws Exception {
        String xml = "<root><name>guest</name><age>25</age></root>";
        UserDTO result = digester.digest(xml, UserDTO.class);
        assertEquals("guest", result.name);
        assertEquals(25, result.age);
    }

    @Test
    public void testKebabCaseMapping() throws Exception {
        String xml = "<root><display-name>My Project</display-name></root>";
        ProjectDTO result = digester.digest(xml, ProjectDTO.class);
        assertEquals("My Project", result.displayName);
    }

    @Test
    public void testAttributeMapping() throws Exception {
        String xml = "<root id=\"100\" version=\"1.2.0\" />";
        VersionDTO result = digester.digest(xml, VersionDTO.class);
        assertEquals(100, result.id);
        assertEquals("1.2.0", result.version);
    }

    @Test
    public void testEnumMapping() throws Exception {
        String xml = "<root><status>ACTIVE</status></root>";
        EnumDTO result = digester.digest(xml, EnumDTO.class);
        assertEquals(Status.ACTIVE, result.status);
    }

    @Test
    public void testListWithWrapper() throws Exception {
        String xml = "<root><tags><tag>Java</tag><tag>XML</tag></tags></root>";
        ListDTO result = digester.digest(xml, ListDTO.class);
        assertEquals(2, result.tags.size());
        assertEquals("Java", result.tags.get(0));
    }

    @Test
    public void testListWithoutWrapper() throws Exception {
        String xml = "<root><item>A</item><item>B</item></root>";
        ListDTO result = digester.digest(xml, ListDTO.class);
        assertEquals(2, result.items.size());
    }

    @Test
    public void testMapMapping() throws Exception {
        String xml = "<root><props><key1>val1</key1><key2>val2</key2></props></root>";
        MapDTO result = digester.digest(xml, MapDTO.class);
        assertEquals("val1", result.props.get("key1"));
        assertEquals("val2", result.props.get("key2"));
    }

    @Test
    public void testTypeResolverPolymorphism() throws Exception {
        // 빌더를 통해 리졸버 등록 테스트
        digester = XmlDigester.newBuilder().registerTypeResolver(new ShapeResolver()).build();
        String xml = "<root><shape type=\"circle\"><radius>5</radius></shape></root>";
        ShapeContainer result = digester.digest(xml, ShapeContainer.class);
        assertTrue(result.shape instanceof Circle);
        assertEquals(5, ((Circle)result.shape).radius);
    }

    @Test
    public void testCustomTypeConverter() throws Exception {
        digester = XmlDigester.newBuilder().registerTypeConverter(new MoneyConverter()).build();
        String xml = "<root><price>1000.50</price></root>";
        MoneyDTO result = digester.digest(xml, MoneyDTO.class);
        assertEquals(new BigDecimal("1000.50"), result.price);
    }

    @Test
    public void testInheritance() throws Exception {
        String xml = "<child id=\"1\"><parent-name>P</parent-name><child-name>C</child-name></child>";
        ChildDTO result = digester.digest(xml, ChildDTO.class);
        assertEquals(1, result.id);
        assertEquals("P", result.parentName);
        assertEquals("C", result.childName);
    }

    @Test
    public void testRecursiveTree() throws Exception {
        String xml = "<node name=\"root\"><child name=\"leaf\"/></node>";
        NodeDTO result = digester.digest(xml, NodeDTO.class);
        assertEquals("root", result.name);
        assertNotNull(result.child);
        assertEquals("leaf", result.child.name);
    }

    @Test
    public void testEmptyTagVsMissingTag() throws Exception {
        String xml = "<root><name></name></root>";
        UserDTO result = digester.digest(xml, UserDTO.class);
        assertEquals("", result.name);
        assertNull(result.description);
    }

    @Test
    public void testWhitespaceTrimming() throws Exception {
        String xml = "<root><name> guest </name></root>";
        UserDTO result = digester.digest(xml, UserDTO.class);
        assertEquals("guest", result.name);
    }

    @Test
    public void testCDataProcessing() throws Exception {
        String xml = "<root><name><![CDATA[ <guest> ]]></name></root>";
        UserDTO result = digester.digest(xml, UserDTO.class);
        assertEquals("<guest>", result.name);
    }

    @Test
    public void testCommentsIgnored() throws Exception {
        String xml = "<root><!-- comment --><name>guest</name></root>";
        UserDTO result = digester.digest(xml, UserDTO.class);
        assertEquals("guest", result.name);
    }

    @Test
    public void testDuplicateTagsToSingleField() throws Exception {
        String xml = "<root><name>First</name><name>Second</name></root>";
        UserDTO result = digester.digest(xml, UserDTO.class);
        assertEquals("First", result.name); // 첫 번째 값을 취함
    }

    @Test
    public void testNameConflictAttrVsElem() throws Exception {
        String xml = "<root code=\"ATTR\"><code-val>ELEM</code-val></root>";
        ConflictDTO result = digester.digest(xml, ConflictDTO.class);
        assertEquals("ATTR", result.attrCode);
        assertEquals("ELEM", result.elemCode);
    }

    @Test
    public void testErrorAttributeWithList() throws Exception {
        // Attribute 노드는 ELEMENT_NODE가 아니므로 mapNode에서 null을 반환함
        InvalidAttrListDTO result = digester.digest("<root items=\"A,B\" />", InvalidAttrListDTO.class);

        // 예외가 발생하는 것이 아니라, 매핑이 되지 않아 null이어야 함
        assertNull(result.items);
    }

    @Test(expected = XmlDigesterException.class)
    public void testErrorInvalidEnum() throws Exception {
        digester.digest("<root><status>WRONG</status></root>", EnumDTO.class);
    }

    @Test(expected = XmlDigesterException.class)
    public void testTypeMismatchError() throws Exception {
        String xml = "<user><age>abc</age></user>";
        digester.digest(xml, UserDTO.class);
    }

    @Test(expected = XmlDigesterException.class)
    public void testMalformedXml() throws Exception {
        String xml = "<user><name>guest</user>";
        digester.digest(xml, UserDTO.class);
    }

    @Test
    public void testMultiThreadedDigest() throws InterruptedException {
        final int threadCount = 10;
        final int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        String xml = "<user id=\"1\"><user-name>guest</user-name></user>";

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        UserNameDTO result = digester.digest(xml, UserNameDTO.class);
                        if ("guest".equals(result.userName)) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertEquals(threadCount * iterationsPerThread, successCount.get());
    }

    @Test
    public void testDuplicatePropertyBehavior() throws Exception {
        String xml = "<root><value>Hello</value></root>";
        DuplicatePropertyDTO result = digester.digest(xml, DuplicatePropertyDTO.class);
        assertEquals("Hello", result.first);
        assertEquals("Hello", result.second);
    }

    @Test
    public void testErrorPathAccuracy() {
        String xml = "<root><child><status>INVALID</status></child></root>";
        try {
            digester.digest(xml, NodeDTO.class);
            fail("Should have thrown an exception");
        } catch (XmlDigesterException e) {
            // mapNode에서 던지는 XmlMappingException이 XmlDigesterException으로 래핑됨
            assertTrue(e.getMessage().contains(">root>child>status"));
        }
    }
}



