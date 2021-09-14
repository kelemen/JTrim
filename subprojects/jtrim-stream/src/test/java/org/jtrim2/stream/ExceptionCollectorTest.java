
package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExceptionCollectorTest {
    private static <T> List<T> repeat(int times, T obj) {
        return ProducerConsumerTestUtils.repeat(times, obj);
    }

    private void testConsumeLatestAndGetReturnsOnce(Function<ExceptionCollector, Throwable> consumeAction) {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            RuntimeException testException = new RuntimeException();
            ExceptionCollector collector = new ExceptionCollector();
            collector.setFirstFailure(testException);

            AtomicInteger receiveCount = new AtomicInteger(0);
            Tasks.runConcurrently(repeat(threadCount, () -> {
                Throwable received = consumeAction.apply(collector);
                if (received != null) {
                    receiveCount.incrementAndGet();
                    assertSame(testException, received);
                }
            }));

            assertEquals(1, receiveCount.get());
        }
    }

    @Test
    public void testConsumeLatestAndGetReturnsOnce() {
        testConsumeLatestAndGetReturnsOnce(ExceptionCollector::consumeLatestAndGet);
    }

    @Test
    public void testConsumeLatestAndUpdateReturnsOnceWithoutMain() {
        testConsumeLatestAndGetReturnsOnce(collector -> collector.consumeLatestAndUpdate(null));
    }

    @Test
    public void testConsumeLatestAndUpdateReturnsOnceWithMain() {
        testConsumeLatestAndGetReturnsOnce(collector -> {
            RuntimeException mainEx = new RuntimeException(Long.toString(Thread.currentThread().getId()));

            Throwable receivedMainEx = collector.consumeLatestAndUpdate(mainEx);
            assertSame(mainEx, receivedMainEx);

            Throwable[] suppressed = receivedMainEx.getSuppressed();
            switch (suppressed.length) {
                case 0:
                    return null;
                case 1:
                    return suppressed[0];
                default:
                    throw new AssertionError("Unexpected suppression", receivedMainEx);
            }
        });
    }

    @Test
    public void testConsumeLatestAndUpdateCancellationOverride() {
        ExceptionCollector collector = new ExceptionCollector();

        RuntimeException baseEx = new RuntimeException();
        collector.setFirstFailure(baseEx);

        RuntimeException mainEx = new OperationCanceledException();

        Throwable receivedMainEx = collector.consumeLatestAndUpdate(mainEx);
        assertSame(baseEx, receivedMainEx);
        assertEquals(Arrays.asList(mainEx), Arrays.asList(receivedMainEx.getSuppressed()));
    }

    @Test
    public void testConsumeLatestAndUpdateWithoutFirst() {
        ExceptionCollector collector = new ExceptionCollector();

        RuntimeException mainEx = new RuntimeException();
        Throwable receivedMainEx = collector.consumeLatestAndUpdate(mainEx);
        assertSame(mainEx, receivedMainEx);
        assertArrayEquals(new Throwable[0], receivedMainEx.getSuppressed());
    }

    @Test
    public void testConsumeLatestAndUpdateWithoutFirstAndMain() {
        ExceptionCollector collector = new ExceptionCollector();
        assertNull(collector.consumeLatestAndUpdate(null));
    }

    @Test
    public void testConsumeAndSetConcurrently() {
        int threadCount = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            ExceptionCollector collector = new ExceptionCollector();

            Set<Throwable> received = Collections.newSetFromMap(new IdentityHashMap<>());
            List<Runnable> actions = new ArrayList<>(2 * threadCount);
            for (int j = 0; j < threadCount; j++) {
                RuntimeException testException = new RuntimeException("EX-" + j);
                actions.add(() -> {
                    collector.setFirstFailure(testException);
                });
                actions.add(() -> {
                    Throwable latest = collector.consumeLatestAndGet();
                    if (latest != null) {
                        received.add(latest);
                    }
                });
            }

            Tasks.runConcurrently(actions);

            int receivedCount = received.size();
            if (receivedCount == 0) {
                assertNotNull(collector.consumeLatestAndGet());
            } else if (receivedCount > 1) {
                throw new AssertionError("Unexpected number of consumes: " + receivedCount + ": " + received);
            }
        }
    }
}
