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

import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class TrieTest {
    private Trie<String> trie;

    @Before
    public void setUp() {
        trie = new Trie<>();
    }

    /**
     * [SC-01] 기본 CRUD: 단일 경로 저장 및 정확한 일치 조회
     */
    @Test
    public void testSC01_BasicInsertAndFind() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b/c", "Data1");
        assertEquals("Data1", trie.find("/a/b/c"));
    }

    /**
     * [SC-02] 기본 CRUD: 중복 경로 삽입 시 DuplicatePathException 발생 확인
     */
    @Test(expected = DuplicatePathException.class)
    public void testSC02_DuplicatePathException() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b", "First");
        trie.insert("/a/b", "Second");
    }

    /**
     * [SC-03] 기본 CRUD: 존재하지 않는 경로 조회 시 null 반환
     */
    @Test
    public void testSC03_FindNonExistentPath() throws InvalidPatternException {
        assertNull(trie.find("/no/match"));
    }

    /**
     * [SC-04] 기본 CRUD: 경로 삭제(remove) 후 조회 시 null 반환
     */
    @Test
    public void testSC04_RemovePath() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b", "Data");
        trie.remove("/a/b");
        assertNull(trie.find("/a/b"));
    }

    /**
     * [SC-05] 싱글 와일드카드: 중간 경로에 '*'가 포함된 패턴 매칭 확인
     */
    @Test
    public void testSC05_SingleWildcardMiddle() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/user/*/profile", "UserProfile");
        assertEquals("UserProfile", trie.find("/user/123/profile"));
    }

    /**
     * [SC-06] 우선순위: 정확한 일치(Exact)가 와일드카드(*)보다 우선 매칭됨을 확인
     */
    @Test
    public void testSC06_ExactPriorityOverWildcard() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b", "Exact");
        trie.insert("/a/*", "Wild");
        assertEquals("Exact", trie.find("/a/b"));
    }

    /**
     * [SC-07] 다중 검색: findAll 사용 시 일치하는 모든 데이터(Exact, *, #) 반환
     */
    @Test
    public void testSC07_FindAllMatches() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b", "Data1");
        trie.insert("/a/*", "Data2");
        trie.insert("/a/#", "Data3");
        List<String> results = trie.findAll("/a/b");
        assertEquals(3, results.size());
        assertTrue(results.contains("Data1") && results.contains("Data2") && results.contains("Data3"));
    }

    /**
     * [SC-08] 멀티 와일드카드: 하위 모든 깊이의 경로를 매칭하는지 확인
     */
    @Test
    public void testSC08_MultiWildcardDeepMatch() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/logs/#", "Handler");
        assertEquals("Handler", trie.find("/logs/2024/05/01/error.log"));
    }

    /**
     * [SC-09] 멀티 와일드카드: 루트 '#' 설정 시 모든 경로 Catch-All 처리 확인
     */
    @Test
    public void testSC09_RootCatchAll() throws DuplicatePathException, InvalidPatternException {
        trie.insert("#", "CatchAll");
        assertEquals("CatchAll", trie.find("/any/unregistered/path"));
    }

    /**
     * [SC-10] 복합 패턴: '*'와 '#'이 혼합된 복잡한 경로 매칭 확인
     */
    @Test
    public void testSC10_MixedWildcards() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/app/*/res/#", "Mixed");
        assertEquals("Mixed", trie.find("/app/v1/res/img/logo.png"));
    }

    /**
     * [SC-11] 견고성: 구분자 중복 입력 처리 확인 (///a//b/ -> /a/b)
     */
    @Test
    public void testSC11_SloppyPathHandling() throws DuplicatePathException, InvalidPatternException {
        trie.insert("///a//b/", "Sloppy");
        assertEquals("Sloppy", trie.find("/a/b"));
    }

    /**
     * [SC-12] 예외 처리: '#'이 패턴 마지막이 아닌 중간에 위치할 경우 예외 발생 확인
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSC12_MultiWildcardInvalidPosition() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/#/middle", "Error");
    }

    /**
     * [SC-13] 예외 처리: null 또는 빈 문자열 입력 시 안전한 처리
     */
    @Test
    public void testSC13_NullAndEmptyHandling() throws InvalidPatternException {
        assertNull(trie.find(null));
        assertNull(trie.find("   "));
    }

    /**
     * [SC-14] 커스텀 설정: 구분자와 와일드카드 기호 변경 동작 확인
     */
    @Test
    public void testSC14_CustomConfiguration() throws DuplicatePathException, InvalidPatternException {
        Trie<String> custom = new Trie<>(".", "!", "?");
        custom.insert("sys.cpu.!", "Single");
        assertEquals("Single", custom.find("sys.cpu.usage"));
    }

    /**
     * [SC-15] 동시성: 여러 스레드에서 동시에 다른 경로 삽입 시 안정성 확인
     */
    @Test
    public void testSC15_ConcurrentInsertion() throws InterruptedException {
        final int THREADS = 20;
        ExecutorService service = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final int tid = i;
            service.execute(() -> {
                try { trie.insert("/p/" + tid, "V"); } catch (Exception ignored) {}
                finally { latch.countDown(); }
            });
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        service.shutdown();
    }

    /**
     * [SC-16] 동시성: 읽기/쓰기가 동시에 일어날 때 ConcurrentModification 방지 확인
     */
    @Test
    public void testSC16_ReadWriteContention() throws Exception {
        trie.insert("/a/b", "V");
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            for(int i=0; i<100; i++) {
                try {
                    trie.remove("/a/b");
                } catch (InvalidPatternException ignored) {

                }
                try { trie.insert("/a/b", "V"); } catch(Exception ignored){} }
        });
        for(int i=0; i<100; i++) { trie.find("/a/b"); }
        service.shutdown();
    }

    /**
     * [SC-17] 삭제 심화: 하위 노드 삭제 시 데이터 없는 상위 노드 정리(Cleanup) 확인
     */
    @Test
    public void testSC17_NodeCleanup() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b/c", "Data");
        trie.remove("/a/b/c");
        // 내부 정리는 직접 검증이 어려우나 재삽입 시 에러 없는 것으로 무결성 확인
        trie.insert("/a/b/c", "New");
        assertEquals("New", trie.find("/a/b/c"));
    }

    /**
     * [SC-18] 백트래킹: 일반 노드 진입 실패 시 와일드카드 노드로 되돌아와 재탐색 성공 확인
     */
    @Test
    public void testSC18_BacktrackingSuccess() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a/b/c", "Target1");
        trie.insert("/a/*/c/d", "Target2");
        // /a/b/c/d 조회 시 b로 갔다가 실패 후 다시 나와서 * 경로로 Target2를 찾아야 함
        assertEquals("Target2", trie.find("/a/b/c/d"));
    }

    /**
     * [SC-19] 구조적 무결성: 부모 데이터 삭제 시 자식 데이터 유지 확인
     */
    @Test
    public void testSC19_ParentRemoveChildPreserve() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/a", "Parent");
        trie.insert("/a/b", "Child");
        trie.remove("/a");
        assertNull(trie.find("/a"));
        assertEquals("Child", trie.find("/a/b"));
    }

    /**
     * [SC-20] 특수 문자: 공백, 한글, 특수기호가 포함된 토큰 매칭 확인
     */
    @Test
    public void testSC20_SpecialCharacters() throws DuplicatePathException, InvalidPatternException {
        trie.insert("/한글/특수문자!@/성공", "D1");
        assertEquals("D1", trie.find("/한글/특수문자!@/성공"));
    }
}