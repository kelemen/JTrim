package org.jtrim.taskgraph.basic;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.ManualTaskExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.taskgraph.TaskErrorHandler;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.taskgraph.TaskNodeProperties;
import org.junit.Test;

import static org.jtrim.taskgraph.basic.TestNodes.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TaskNodeTest {
    private static void executeAll(ManualTaskExecutor executor) {
        while (executor.executeCurrentlySubmitted() > 0) {
            // Repeat until there are no more task left to be executed.
        }
    }

    @Test
    public void testComputeSync() {
        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT");

        testNode.verifyNotRun();
        testNode.ensureScheduled();

        testNode.verifyRunWithResult();
    }

    @Test
    public void testComputeAsync() {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT", executor);

        testNode.ensureScheduled();
        testNode.verifyNotRun();

        executeAll(executor);
        testNode.verifyRunWithResult();
    }

    @Test
    public void testComputeSyncMultipleTimes() {
        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT");

        testNode.verifyNotRun();
        for (int i = 0; i < 2; i++) {
            testNode.ensureScheduled();
        }

        testNode.verifyRunWithResult();
    }

    @Test
    public void testComputeAsyncMultipleTimes1() {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT", executor);

        for (int i = 0; i < 2; i++) {
            testNode.ensureScheduled();
        }
        testNode.verifyNotRun();

        executeAll(executor);

        testNode.verifyRunWithResult();
    }

    @Test
    public void testComputeAsyncMultipleTimes2() {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT", executor);

        for (int i = 0; i < 2; i++) {
            testNode.ensureScheduled();
            executeAll(executor);
        }

        testNode.verifyRunWithResult();
    }

    @Test
    public void testExternalCancel() {
        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT");

        testNode.getNode().cancel();
        testNode.ensureScheduled();

        testNode.verifyCanceled();
    }

    @Test
    public void testExternalError() {
        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT");
        Exception expectedError = new Exception("TEST-ERROR");

        testNode.getNode().propagateFailure(expectedError);
        testNode.ensureScheduled();

        testNode.verifyRunWithException(expectedError);
        testNode.verifyNoErrorHandler();
    }

    @Test
    public void testTaskError() {
        Exception expectedError = new Exception("TEST-ERROR");
        TestTaskNode testNode = new TestTaskNode("node1", expectedError);

        testNode.ensureScheduled();

        testNode.verifyRunWithException(expectedError);
        testNode.verifyErrorHandler(expectedError);
    }

    @Test
    public void testExecutorError() {
        RuntimeException expectedError = new RuntimeException("EXECUTOR-ERROR");
        TestTaskNode testNode = new TestTaskNode("node1", expectedError, (cancelToken, task, cleanupTask) -> {
            throw expectedError;
        });

        try {
            testNode.ensureScheduled();
        } catch (Throwable ex) {
            assertSame(expectedError, ex);
        }

        testNode.verifyNotRun();
        testNode.verifyErrorHandler(expectedError);
    }

    @Test
    public void testPriorCancel() {
        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT");

        testNode.ensureScheduled(Cancellation.CANCELED_TOKEN);

        testNode.verifyCanceled();
    }

    private TestTaskNode testAfterScheduleCancel(boolean eagerCancel) {
        ManualTaskExecutor executor = new ManualTaskExecutor(eagerCancel);
        CancellationSource cancel = Cancellation.createCancellationSource();

        TestTaskNode testNode = new TestTaskNode("node1", "TEST-RESULT", executor);

        testNode.ensureScheduled(cancel.getToken());
        cancel.getController().cancel();
        executeAll(executor);

        testNode.verifyCanceledWithoutRunTest();

        return testNode;
    }

    @Test
    public void testAfterScheduleCancel() {
        TestTaskNode testNode = testAfterScheduleCancel(true);
        testNode.verifyNotRun();
    }

    @Test
    public void testAfterScheduleCancelReceivedByTask() {
        TestTaskNode testNode = testAfterScheduleCancel(false);
        testNode.verifyRun();
    }

    @Test
    public void testGetExpectedResultNowBeforeCompleted() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        TaskNodeKey<Object, Object> nodeKey = node("Test-Node-1");

        try {
            TaskNode.getExpectedResultNow(nodeKey, future);
        } catch (IllegalStateException ex) {
            String message = ex.getMessage();
            assertTrue(message, message.contains(nodeKey.toString()));
            return;
        }
        throw new AssertionError("Expected IllegalStateException");
    }

    @Test
    public void testGetResultConvertsCancellation() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new CancellationException());

        try {
            TaskNode.getResultNow(future);
        } catch (OperationCanceledException ex) {
            return;
        }
        throw new AssertionError("Expected OperationCanceledException");
    }

    private static final class TestTaskNode {
        private final TaskNode<Object, Object> node;
        private final TaskErrorHandler errorHandler;
        private final TestCancelableFunction<Object> function;
        private final Object result;

        public TestTaskNode(Object key, Object result) {
            this(key, result, SyncTaskExecutor.getSimpleExecutor());
        }

        public TestTaskNode(Object key, Object result, TaskExecutor executor) {
            TaskNodeProperties.Builder properties = new TaskNodeProperties.Builder();
            properties.setExecutor(executor);

            this.result = result;
            this.errorHandler = mock(TaskErrorHandler.class);
            this.function = new TestCancelableFunction<>(key, result);

            this.node = new TaskNode<>(
                    node(key),
                    new NodeTaskRef<>(properties.build(), function));
        }

        public void ensureScheduled() {
            ensureScheduled(Cancellation.UNCANCELABLE_TOKEN);
        }

        public void ensureScheduled(CancellationToken cancelToken) {
            node.ensureScheduleComputed(cancelToken, errorHandler);
        }

        public void verifyRunWithResult() {
            verifyRun();
            assertTrue("hasResult", node.hasResult());
            assertSame("getResult", result, node.getResult());
            assertSame("taskFuture().getNow", result, node.taskFuture().getNow(null));
            assertSame("TaskNode.getResultNow", result, TaskNode.getResultNow(node.taskFuture()));
            assertSame("TaskNode.getExpectedResultNow",
                    result,
                    TaskNode.getExpectedResultNow(node.getKey(), node.taskFuture()));
            verifyNoErrorHandler();
        }

        public void verifyRunWithException(Throwable expected) {
            assertFalse("hasResult", node.hasResult());
            expectCompletionException(expected, () -> node.getResult());
            expectCompletionException(expected, () -> node.taskFuture().getNow(null));
            expectCompletionException(expected, () -> TaskNode.getExpectedResultNow(node.getKey(), node.taskFuture()));
            expectCompletionException(expected, () -> TaskNode.getResultNow(node.taskFuture()));
        }

        public void verifyNoErrorHandler() {
            verifyZeroInteractions(errorHandler);
        }

        public void verifyErrorHandler(Throwable expected) {
            verify(errorHandler).onError(eq(node.getKey()), same(expected));
        }

        public void verifyCanceledWithoutRunTest() {
            assertFalse("hasResult", node.hasResult());
            expectCancellation(() -> node.getResult());
            expectCompletionException(OperationCanceledException.class, () -> node.taskFuture().getNow(null));
            expectCancellation(() -> TaskNode.getExpectedResultNow(node.getKey(), node.taskFuture()));
            expectCancellation(() -> TaskNode.getResultNow(node.taskFuture()));
            verifyNoErrorHandler();
        }

        public void verifyCanceled() {
            verifyNotRun();
            verifyCanceledWithoutRunTest();
        }

        private <E extends Throwable> void expectCompletionException(Class<E> expectedError, Runnable task) {
            try {
                task.run();
            } catch (CompletionException ex) {
                assertTrue("error", expectedError.isAssignableFrom(ex.getCause().getClass()));
                return;
            }

            throw new AssertionError("Expected error for " + node.getKey());
        }


        private void expectCompletionException(Throwable expectedError, Runnable task) {
            try {
                task.run();
            } catch (CompletionException ex) {
                assertSame("error", expectedError, ex.getCause());
                return;
            }

            throw new AssertionError("Expected error for " + node.getKey());
        }

        private void expectCancellation(Runnable task) {
            try {
                task.run();
            } catch (OperationCanceledException ex) {
                return;
            }

            throw new AssertionError("Expected OperationCanceledException for " + node.getKey());
        }

        public TaskNode<Object, Object> getNode() {
            return node;
        }

        public void verifyNotRun() {
            function.verifyNotCalled();
        }

        public void verifyRun() {
            function.verifyCalled();
        }
    }
}
