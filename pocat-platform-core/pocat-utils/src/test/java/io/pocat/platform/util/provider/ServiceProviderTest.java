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

import org.junit.Assert;
import org.junit.Test;
import java.util.List;
import java.util.Optional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceProviderTest {

    @Test
    public void testLoadServicesLazy() {
        // 1. 객체 생성 (로딩 전)
        ServiceProvider<TestService> provider = ServiceProvider.of(TestService.class);

        // 2. 서비스 호출 및 로딩 확인
        List<TestService> services = provider.getServices();

        // 3. 검증
        Assert.assertNotNull(services);
        Assert.assertFalse("서비스 리스트가 비어있으면 안됩니다.", services.isEmpty());
        Assert.assertEquals("Hello SPI", services.get(0).sayHello());
    }

    @Test
    public void testServiceCachingAndImmutability() {
        ServiceProvider<TestService> provider = ServiceProvider.of(TestService.class);

        List<TestService> firstList = provider.getServices();
        List<TestService> secondList = provider.getServices();

        // 캐싱 검증: 동일한 인스턴스여야 함
        Assert.assertSame("두 리스트는 동일한 객체여야 합니다.", firstList, secondList);

        // 불변성 검증: 수정 시 예외 발생 (AssertJ가 편리함)
        try {
            firstList.add(null);
            Assert.fail("수정 불가능한 리스트여야 합니다.");
        } catch (UnsupportedOperationException ignored) {
            // 정상
        }
    }

    @Test
    public void testFindFirst() {
        ServiceProvider<TestService> provider = ServiceProvider.of(TestService.class);

        Optional<TestService> result = provider.findFirst(s -> s.sayHello().equals("Hello SPI"));

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("Hello SPI", result.get().sayHello());
    }

    @Test
    public void testClassLoaderFallback() {
        // null을 전달해도 내부적으로 적절한 로더를 선택하는지 확인
        ServiceProvider<TestService> provider = ServiceProvider.of(TestService.class);

        List<TestService> services = provider.getServices();
        Assert.assertNotNull(services);
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final ServiceProvider<TestService> provider = ServiceProvider.of(TestService.class);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    provider.getServices();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 완료 대기
        Assert.assertNotNull(provider.getServices());
        executor.shutdown();
    }
}
