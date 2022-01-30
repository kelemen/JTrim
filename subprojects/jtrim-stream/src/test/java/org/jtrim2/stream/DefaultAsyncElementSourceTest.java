package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.testutils.RepeatTest;
import org.jtrim2.testutils.RepeatTestRule;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.UnsafeRunnable;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultAsyncElementSourceTest {
    @Rule
    public final RepeatTestRule repeatRule = new RepeatTestRule();

    private static DefaultAsyncElementSource<String> create(int maxQueueSize) {
        return new DefaultAsyncElementSource<>(maxQueueSize, maxQueueSize);
    }

    @Test(timeout = 10000)
    public void testSimplePutThenTake() throws Exception {
        DefaultAsyncElementSource<String> source = create(10);

        for (int i = 0; i < 3; i++) {
            String testValue = "MyTestElement" + i;
            assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, testValue));
            assertEquals("get", testValue, source.getNext(Cancellation.UNCANCELABLE_TOKEN));
        }
    }

    @Test(timeout = 10000)
    public void testPutManyThenTakeAll() throws Exception {
        int elementCount = 10;
        DefaultAsyncElementSource<String> source = create(elementCount);

        List<String> elements = IntStream
                .range(0, elementCount).
                mapToObj(i -> "MyTestElement" + i)
                .collect(Collectors.toList());

        for (String element: elements) {
            assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, element));
        }

        for (String element: elements) {
            assertEquals("get", element, source.getNext(Cancellation.UNCANCELABLE_TOKEN));
        }
    }

    @Test(timeout = 10000)
    public void testNormalFinish() throws Exception {
        DefaultAsyncElementSource<String> source = create(10);

        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V1"));
        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V2"));

        source.finish(null);

        assertFalse("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V3"));

        assertEquals("get", "V1", source.getNext(Cancellation.UNCANCELABLE_TOKEN));
        assertEquals("get", "V2", source.getNext(Cancellation.UNCANCELABLE_TOKEN));
        assertNull("get", source.getNext(Cancellation.UNCANCELABLE_TOKEN));
    }

    private static void expectException(TestException expected, UnsafeRunnable task) throws Exception {
        try {
            task.run();
            fail("Expected TestException");
        } catch (TestException ex) {
            assertSame(expected, ex);
        }
    }

    @Test(timeout = 10000)
    public void testFailedFinish() throws Exception {
        DefaultAsyncElementSource<String> source = create(10);

        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V1"));
        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V2"));

        TestException expected = new TestException();
        source.finish(expected);

        expectException(expected, () -> source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V3"));
        expectException(expected, () -> source.getNext(Cancellation.UNCANCELABLE_TOKEN));
    }

    private static int getTestThreadCount() {
        return Math.min(4, 4 * Runtime.getRuntime().availableProcessors());
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testNormalFinishConcurrentPut() throws Exception {
        DefaultAsyncElementSource<String> source = create(2);

        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V1"));
        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V2"));

        int testThreadCount = getTestThreadCount();
        List<Runnable> tasks = new ArrayList<>(testThreadCount + 1);
        tasks.add(TestUtils.toSafeRunnable(() -> {
            source.finish(null);
        }));

        Set<String> extraAdded = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (int i = 0; i < testThreadCount; i++) {
            String message = "X" + i;
            tasks.add(TestUtils.toSafeRunnable(() -> {
                if (source.tryPut(Cancellation.UNCANCELABLE_TOKEN, message)) {
                    extraAdded.add(message);
                }
            }));
        }

        Tasks.runConcurrently(tasks);

        assertEquals("get", "V1", source.getNext(Cancellation.UNCANCELABLE_TOKEN));
        assertEquals("get", "V2", source.getNext(Cancellation.UNCANCELABLE_TOKEN));

        Set<String> pulled = new HashSet<>();
        for (int i = extraAdded.size(); i > 0; i--) {
            String current = source.getNext(Cancellation.UNCANCELABLE_TOKEN);
            if (current == null) {
                throw new AssertionError("Expected " + extraAdded.size() + " but received " + i + " less");
            }
            pulled.add(current);
        }
        assertEquals("remaining", extraAdded, pulled);
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testFailedFinishConcurrentPut() throws Exception {
        DefaultAsyncElementSource<String> source = create(2);

        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V1"));
        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V2"));

        TestException expectedFailure = new TestException();

        int testThreadCount = getTestThreadCount();
        List<Runnable> tasks = new ArrayList<>(testThreadCount + 1);
        tasks.add(TestUtils.toSafeRunnable(() -> {
            source.finish(expectedFailure);
        }));

        for (int i = 0; i < testThreadCount; i++) {
            String message = "X" + i;
            tasks.add(TestUtils.toSafeRunnable(() -> {
                try {
                    assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, message));
                } catch (TestException ex) {
                    assertSame(expectedFailure, ex);
                }
            }));
        }

        Tasks.runConcurrently(tasks);

        expectException(expectedFailure, () -> source.getNext(Cancellation.UNCANCELABLE_TOKEN));
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testCanceledPut() throws Exception {
        DefaultAsyncElementSource<String> source = create(2);

        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V1"));
        assertTrue("put", source.tryPut(Cancellation.UNCANCELABLE_TOKEN, "V2"));

        CancellationSource cancellation = Cancellation.createCancellationSource();

        List<Runnable> tasks = new ArrayList<>();

        int testThreadCount = getTestThreadCount();

        for (int i = 0; i < testThreadCount; i++) {
            String message = "X" + i;
            tasks.add(TestUtils.toSafeRunnable(() -> {
                try {
                    source.tryPut(cancellation.getToken(), message);
                    fail("Expected cancellation.");
                } catch (OperationCanceledException ex) {
                    // Expected.
                }
            }));
        }
        tasks.add(cancellation.getController()::cancel);

        Tasks.runConcurrently(tasks);
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testCanceledGet() {
        DefaultAsyncElementSource<String> source = create(2);

        CancellationSource cancellation = Cancellation.createCancellationSource();

        List<Runnable> tasks = new ArrayList<>();

        int testThreadCount = getTestThreadCount();

        for (int i = 0; i < testThreadCount; i++) {
            tasks.add(TestUtils.toSafeRunnable(() -> {
                try {
                    source.getNext(cancellation.getToken());
                    fail("Expected cancellation.");
                } catch (OperationCanceledException ex) {
                    // Expected.
                }
            }));
        }
        tasks.add(cancellation.getController()::cancel);

        Tasks.runConcurrently(tasks);
    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
