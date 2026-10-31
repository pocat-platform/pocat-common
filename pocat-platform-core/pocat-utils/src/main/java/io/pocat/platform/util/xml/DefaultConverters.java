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

import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

class DefaultConverters {
    private static final TypeConverter<?>[] CONVERTERS = {
            new StringConverter(),
            new IntegerConverter(),
            new LongConverter(),
            new DoubleConverter(),
            new BigDecimalConverter(),
            new BooleanConverter(),
            new DateConverter(),
            new LocalDateTimeConverter(),
            new UuidConverter()
    };

    public static TypeConverter<?>[] getConverters() {
        return CONVERTERS;
    }

    private DefaultConverters() {

    }

    static class StringConverter implements TypeConverter<String> {
        @Override
        public String convert(Node node) {
            return Optional.ofNullable(node.getTextContent())
                    .map(String::trim)
                    .orElse("");
        }

        @Override
        public Class<String> getType() { return String.class; }
    }

    static class IntegerConverter implements TypeConverter<Integer> {
        @Override
        public Integer convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            return s.isEmpty() ? null : Integer.parseInt(s);
        }
        @Override
        public Class<Integer> getType() { return Integer.class; }
    }

    static class LongConverter implements TypeConverter<Long> {
        @Override
        public Long convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            return s.isEmpty() ? null : Long.parseLong(s);
        }
        @Override
        public Class<Long> getType() { return Long.class; }
    }

    static class DoubleConverter implements TypeConverter<Double> {
        @Override
        public Double convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            return s.isEmpty() ? null : Double.parseDouble(s);
        }
        @Override
        public Class<Double> getType() { return Double.class; }
    }

    static class BigDecimalConverter implements TypeConverter<BigDecimal> {
        @Override
        public BigDecimal convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            return s.isEmpty() ? null : new BigDecimal(s);
        }
        @Override
        public Class<BigDecimal> getType() { return BigDecimal.class; }
    }

    static class BooleanConverter implements TypeConverter<Boolean> {
        @Override public Boolean convert(Node node) {
            if(node == null) {
                return Boolean.FALSE;
            }

            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            if(s.isBlank()) {
                return Boolean.TRUE;
            }
            s = s.toLowerCase();
            if (s.matches("true|1|yes|on")) return Boolean.TRUE;
            if (s.matches("false|0|no|off")) return Boolean.FALSE;

            return Boolean.FALSE;
        }
        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }

    static class DateConverter implements TypeConverter<Date> {
        private static final String[] PATTERNS = {
                "yyyy-MM-dd['T'[HH:mm[:ss]]]", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd", "yyyyMMdd"
        };
        @Override
        public Date convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            if (s.isEmpty()) return null;
            if (s.matches("\\d+")) return new Date(Long.parseLong(s));
            for (String p : PATTERNS) {
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(p);
                    TemporalAccessor ta = dtf.parseBest(s, LocalDateTime::from, LocalDate::from);
                    if (ta instanceof LocalDateTime) {
                        return Date.from(((LocalDateTime) ta).atZone(ZoneId.systemDefault()).toInstant());
                    } else {
                        return Date.from(((LocalDate) ta).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    }
                } catch (Exception ignored) {}
            }
            throw new IllegalArgumentException("Unsupported date format: " + s);
        }
        @Override public Class<Date> getType() { return Date.class; }
    }

    static class LocalDateTimeConverter implements TypeConverter<LocalDateTime> {
        @Override
        public LocalDateTime convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            if (s.isEmpty()) return null;
            if (!s.contains("T") && s.contains(" ")) s = s.replace(" ", "T");
            return LocalDateTime.parse(s);
        }
        @Override public Class<LocalDateTime> getType() { return LocalDateTime.class; }
    }

    static class UuidConverter implements TypeConverter<UUID> {
        @Override
        public UUID convert(Node node) {
            String s = Optional.ofNullable(node.getTextContent()).map(String::trim).orElse("");
            return s.isEmpty() ? null : UUID.fromString(s);
        }
        @Override public Class<UUID> getType() { return UUID.class; }
    }
}

