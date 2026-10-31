package io.pocat.platform.util.executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ContextAwareExecutorServiceTest {

    private ContextAwareExecutorService executor;

    @Before
    public void setUp() {
        // 단일 스레드 풀을 사용하여 큐 대기 상태와 생명주기를 결정론적으로 제어합니다.
        executor = new ContextAwareExecutorService(1, new LinkedBlockingQueue<>());
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        ExecutorContext.destroy();
    }

    @Test
    public void testSubmitCallablePropagatesContextSuccessfully() throws Exception {
        // Arrange - 부모 스레드에 추적 컨텍스트(Trace ID) 주입
        ExecutorContext.setAttribute("traceId", "TX-1000");

        // Act - 비동기 작업 submit 호출
        // [정상 경로] newTaskFor()가 TracableFutureTask를 생성하고, execute()의 첫 번째 분기를 타고 super.execute()로 직행
        Future<String> future = executor.submit(() -> (String) ExecutorContext.getAttribute("traceId"));

        // Assert - 자식 스레드가 부모의 컨텍스트를 완벽하게 인계받았는지 검증
        String childTraceId = future.get(2, TimeUnit.SECONDS);
        assertEquals("TX-1000", childTraceId);
    }

    @Test(expected = CancellationException.class)
    public void testCancelQueuedTaskRemovesFromQueueSuccessfully() throws Exception {
        // Arrange - 첫 번째 작업으로 워커 스레드를 인위적으로 선점하여 대기 상태 유도
        final CountDownLatch blocker = new CountDownLatch(1);
        final CountDownLatch firstTaskStarted = new CountDownLatch(1);

        executor.execute(() -> {
            firstTaskStarted.countDown();
            try {
                blocker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        firstTaskStarted.await();

        // 부모 컨텍스트 설정 후, 두 번째 비동기 작업 submit (현재 큐에서 대기하게 됨)
        ExecutorContext.setAttribute("traceId", "TX-2000");
        Future<String> future = executor.submit(() -> "SHOULD_BE_CANCELLED");

        // Act - 작업이 실행되기 전에 취소(cancel) 감행
        // [정상 경로] execute() 단에서 중첩 래핑을 타지 않아 원본 레퍼런스가 보존되므로 큐에서 정상 삭제됨
        boolean isCancelled = future.cancel(true);
        blocker.countDown(); // 첫 번째 작업 해제

        // Assert - 자바 표준 동시성 명세대로 cancel() 성공 여부와 CancellationException 전파 확인
        assertTrue(isCancelled);
        assertTrue(future.isCancelled());

        future.get(1, TimeUnit.SECONDS);
        fail("Expected CancellationException was not thrown.");
    }

    @Test
    public void testExecuteRunnableClearsThreadLocalAfterCompletion() throws Exception {
        // Arrange - 부모 컨텍스트 설정
        ExecutorContext.setAttribute("traceId", "TX-3000");

        final CountDownLatch firstTaskDone = new CountDownLatch(1);
        final AtomicReference<String> capturedTraceId = new AtomicReference<>();

        // Act & Assert 1 - 순수 일반 execute(Runnable) 호출 및 컨텍스트 전파 검증
        // [정상 경로] execute()의 else 문으로 빠져 TraceableTask로 포장되어 컨텍스트 전파
        executor.execute(() -> {
            capturedTraceId.set((String) ExecutorContext.getAttribute("traceId"));
            firstTaskDone.countDown();
        });

        if(firstTaskDone.await(2, TimeUnit.SECONDS)) {
            assertEquals("TX-3000", capturedTraceId.get());
        } else {
            throw new TimeoutException("Time out at 1");
        }

        // Act & Assert 2 - afterExecute 훅에 의해 스레드 풀 오염(ThreadLocal 누수)이 완벽히 방어되는지 검증
        final AtomicReference<String> leakedTraceId = new AtomicReference<>();
        final CountDownLatch leakCheckDone = new CountDownLatch(1);

        // 부모 컨텍스트를 완전히 클리어한 뒤, 추적 기능이 없는 '일반 작업'을 풀에 밀어 넣음
        ExecutorContext.destroy();

        executor.execute(() -> {
            // ThreadLocal.withInitial(HashMap::new) 구조이므로 빈 맵 상태에서 null을 반환해야 함
            leakedTraceId.set((String) ExecutorContext.getAttribute("traceId"));
            leakCheckDone.countDown();
        });

        if(leakCheckDone.await(2, TimeUnit.SECONDS)) {
            assertNull(leakedTraceId.get());
        } else {
            throw new TimeoutException("Time out at 2");
        }
    }

    @Test
    public void testExecuteExternalFutureTaskUnblocksGetSuccessfully() throws Exception {
        // Arrange - 부모 컨텍스트 설정
        ExecutorContext.setAttribute("traceId", "TX-4000");

        final AtomicReference<String> capturedTraceId = new AtomicReference<>();

        // 사용자가 직접 자바 표준 FutureTask를 생성해서 execute()로 강제 주입하는 엣지 케이스 시뮬레이션
        // [정상 경로] execute()의 else 문에서 TraceableTask로 1회 안전하게 래핑됨
        FutureTask<Void> externalFutureTask = new FutureTask<>(() -> capturedTraceId.set((String) ExecutorContext.getAttribute("traceId")), null);

        // Act - 외부 FutureTask를 직접 풀에 밀어 넣음
        executor.execute(externalFutureTask);

        // Assert - 내부 run() 호출 연쇄가 성공하여 외부의 get() 대기가 정상 해제(교착 상태 방지)되는지 검증
        externalFutureTask.get(2, TimeUnit.SECONDS);

        assertTrue(externalFutureTask.isDone());
        assertEquals("TX-4000", capturedTraceId.get());
    }
}

