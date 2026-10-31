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

package io.pocat.platform.util.trie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class Trie<T> {
    private static final String BASE_ALLOWED_CHARS = "a-zA-Z0-9\\-_";

    private static final String DEFAULT_SEPARATOR = "/";
    private static final String DEFAULT_SINGLE_WILD_CARD = "*";
    private static final String DEFAULT_MULTI_WILD_CARD = "#";

    private static final String INTERNAL_SINGLE_WILD_CARD = "\u0000*\u0000";
    private static final String INTERNAL_MULTI_WILD_CARD = "\u0000#\u0000";
    private static final int MAX_PATTERN_LENGTH = 255;

    private final TrieNode<T> root = new TrieNode<>();

    private final String separator;
    private final String singleWild;
    private final String multiWild;
    private final Pattern allowedCharsPattern;


    public Trie() {
        this(DEFAULT_SEPARATOR, DEFAULT_SINGLE_WILD_CARD, DEFAULT_MULTI_WILD_CARD);
    }

    public Trie(String separator) {
        this(separator, DEFAULT_SINGLE_WILD_CARD, DEFAULT_MULTI_WILD_CARD);
    }

    public Trie(String separator, String singleWild, String multiWild) {
        this.separator = separator;
        this.singleWild = singleWild;
        this.multiWild = multiWild;
        String escapedSingle = Pattern.quote(this.singleWild);
        String escapedMulti = Pattern.quote(this.multiWild);

        this.allowedCharsPattern = Pattern.compile("^[" + BASE_ALLOWED_CHARS + escapedSingle + escapedMulti + "]+$");
    }

    public void insert(String pattern, T data) throws DuplicatePathException, InvalidPatternException {
        checkInvalidPattern(pattern);
        if(data == null) {
            throw new InvalidPatternException("Data must not be null.");
        }

        TrieNode<T> current = this.root;

        int start = 0;
        int next;
        int end;
        int position = 0;
        while (start < pattern.length()) {
            position++;
            while (start < pattern.length() && pattern.startsWith(this.separator, start)) {
                start += this.separator.length();
            }
            if(start >= pattern.length()) {
                break;
            }
            next = pattern.indexOf(separator, start);
            end = (next == -1)? pattern.length():next;
            String token = pattern.substring(start, end);

            if(token.isBlank()) {
                throw new InvalidPatternException(String.format("Level at position [%d] is empty. Check for consecutive delimiters ('.').", position));
            }
            if(!this.allowedCharsPattern.matcher(token).matches()) {
                throw new InvalidPatternException(String.format("Level at position [%d] ('%s') contains invalid characters or spaces. Only alphanumeric, '-', '_', '*', and '#' are allowed.", position, token));
            }
            if(token.contains(this.singleWild)) {
                if(token.length() > 1) {
                    throw new InvalidPatternException(String.format("Single-wildcard '*' at position [%d] ('%s') must be used as a standalone level.", position, token));
                } else {
                    token = INTERNAL_SINGLE_WILD_CARD;
                }
            }
            if(token.contains(this.multiWild)) {
                if(token.length() > 1) {
                    throw new InvalidPatternException(String.format("Multi-wildcard '#' at position [%d] ('%s') must be used as a standalone level.", position, token));
                } else {
                    token = INTERNAL_MULTI_WILD_CARD;
                }
            }

            current = current.children.computeIfAbsent(token, k -> new TrieNode<>());
            start = end;
        }
        if (!current.setData(data)) {
            throw new DuplicatePathException(pattern, current.getData());
        }
    }

    public T find(String pattern) throws InvalidPatternException {
        checkInvalidPattern(pattern);

        return find(this.root, pattern, 0);
    }

    private T find(TrieNode<T> current, String pattern, int start) {
        while (start < pattern.length() && pattern.startsWith(this.separator, start)) {
            start += this.separator.length();
        }

        if(start >= pattern.length()) {
            return current.getData();
        }

        int next = pattern.indexOf(this.separator, start);
        int end = (next == -1)? pattern.length() : next;
        String token = pattern.substring(start, end);

        TrieNode<T> child = current.getChildren().get(token);
        T result;
        if(child != null) {
            result = find(child, pattern, end);
            if(result != null) {
                return result;
            }
        }

        child = current.getChildren().get(INTERNAL_SINGLE_WILD_CARD);
        if(child != null) {
            result = find(child, pattern, end);
            if(result != null) {
                return result;
            }
        }

        child = current.getChildren().get(INTERNAL_MULTI_WILD_CARD);
        if(child != null) {
            if(child.getData() != null) {
                result = current.getChildren().get(INTERNAL_MULTI_WILD_CARD).getData();
                return result;
            }
        }

        return null;
    }

    public List<T> findAll(String pattern) throws InvalidPatternException {
        checkInvalidPattern(pattern);

        List<T> results = new ArrayList<>();
        findAll(this.root, pattern, 0, results);
        return results;
    }

    private void findAll(TrieNode<T> current, String pattern, int start, List<T> results) {
        while (start < pattern.length() && pattern.startsWith(this.separator, start)) {
            start += this.separator.length();
        }

        if(start >= pattern.length()) {
            if (current.getData() != null) {
                results.add(current.getData());
            }
            return;
        }

        int next = pattern.indexOf(this.separator, start);
        int end = (next == -1)? pattern.length() : next;
        String token = pattern.substring(start, end);

        TrieNode<T> child = current.getChildren().get(token);
        if(child != null) {
            findAll(child, pattern, end, results);
        }

        child = current.getChildren().get(INTERNAL_SINGLE_WILD_CARD);
        if(child != null) {
            findAll(child, pattern, end, results);
        }

        child = current.getChildren().get(INTERNAL_MULTI_WILD_CARD);
        if(child != null) {
            if(child.getData() != null) {
                results.add(child.getData());
            }
        }
    }

    public void remove(String pattern) throws InvalidPatternException {
        checkInvalidPattern(pattern);

        remove(this.root, pattern, 0);
    }

    private boolean remove(TrieNode<T> current, String pattern, int start) {
        while (start < pattern.length() && pattern.startsWith(separator, start)) {
            start += separator.length();
        }

        if (start >= pattern.length()) {
            current.clearData();
        } else {
            int next = pattern.indexOf(separator, start);
            int end = (next == -1) ? pattern.length() : next;
            String token = pattern.substring(start, end);

            if (token.equals(this.singleWild)) {
                token = INTERNAL_SINGLE_WILD_CARD;
            } else if (token.equals(this.multiWild)) {
                token = INTERNAL_MULTI_WILD_CARD;
            }

            TrieNode<T> child = current.children.get(token);
            if (child != null) {
                if (remove(child, pattern, end)) {
                    current.getChildren().remove(token, child);
                }
            }
        }
        return current.getChildren().isEmpty() && current.getData() == null;
    }

    private void checkInvalidPattern(String pattern) throws InvalidPatternException {
        if(pattern == null || pattern.isBlank()) {
            throw new InvalidPatternException("The topic is null or empty.");
        }

        if(pattern.length() > MAX_PATTERN_LENGTH) {
            throw new InvalidPatternException(String.format("The total topic length (%d bytes) exceeds the limit of %d bytes.", pattern.length(), MAX_PATTERN_LENGTH));
        }

        int idx = pattern.indexOf(this.multiWild);

        if (idx != -1 && idx < pattern.lastIndexOf(this.separator)) {
            throw new InvalidPatternException(String.format("Multi-wildcard '#' must be at the end of the pattern: [%s]", pattern));
        }
    }

    private static class TrieNode<T> {
        private static final int DEFAULT_MAP_SIZE = 4;

        private final Map<String, TrieNode<T>> children = new ConcurrentHashMap<>(DEFAULT_MAP_SIZE);
        private final AtomicReference<T> data = new AtomicReference<>(null);

        public T getData() {
            return this.data.get();
        }
        public boolean setData(T data) {
            return this.data.compareAndSet(null, data);
        }

        public Map<String, TrieNode<T>> getChildren() {
            return this.children;
        }

        public void clearData() {
            this.data.set(null);
        }
    }
}