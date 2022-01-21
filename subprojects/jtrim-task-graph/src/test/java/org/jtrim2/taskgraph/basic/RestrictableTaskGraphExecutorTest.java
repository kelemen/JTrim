package org.jtrim2.taskgraph.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.taskgraph.BuiltGraph;
import org.jtrim2.taskgraph.ExecutionResultType;
import org.jtrim2.taskgraph.TaskErrorHandler;
import org.jtrim2.taskgraph.TaskGraphExecutionException;
import org.jtrim2.taskgraph.TaskGraphExecutionResult;
import org.jtrim2.taskgraph.TaskNodeKey;
import org.jtrim2.taskgraph.TaskNodeProperties;
import org.jtrim2.taskgraph.TaskSkippedException;
import org.jtrim2.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.jtrim2.taskgraph.basic.TestNodes.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RestrictableTaskGraphExecutorTest {
    private CancellationSource cancel;
    private Consumer<? super Throwable> testErrorHandler;

    @Before
    public void setup() {
        this.cancel = Cancellation.createCancellationSource();
        this.testErrorHandler = (error) -> {
            if (error != null) {
                throw new AssertionError("Unexpected error", error);
            }
        };
    }

    private TaskGraphExecutionResult testExecution(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends TaskNode<?, ?>> taskNodes,
            Consumer<? super RestrictableNodes> releaseAction) {
        return testCustomExecution(graph, taskNodes, Tasks.noOpConsumer(), releaseAction);
    }

    private TaskGraphExecutionResult testCustomExecution(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends TaskNode<?, ?>> taskNodes,
            Consumer<? super RestrictableTaskGraphExecutor> executorConfig,
            Consumer<? super RestrictableNodes> releaseAction) {

        AtomicReference<RestrictableNodes> restrictableNodesRef = new AtomicReference<>();

        CollectorErrorHandler errorHandler = new CollectorErrorHandler();
        Set<TaskNodeKey<?, ?>> notifiedMultipleTimes = new LinkedHashSet<>();

        RestrictableTaskGraphExecutor executor;
        executor = new RestrictableTaskGraphExecutor(graph, taskNodes, (taskGraph, restrictableNodes) -> {
            assertSame(graph, taskGraph);
            RestrictableNodes testRestrictableNodes = new RestrictableNodes(errorHandler, restrictableNodes);
            restrictableNodesRef.set(testRestrictableNodes);
            return (TaskNodeKey<?, ?> nodeKey) -> {
                if (!testRestrictableNodes.setComputed(nodeKey)) {
                    notifiedMultipleTimes.add(nodeKey);
                }
            };
        });

        executor.properties().setComputeErrorHandler(errorHandler);

        executorConfig.accept(executor);

        CompletionStage<TaskGraphExecutionResult> future = executor.execute(cancel.getToken());

        releaseAction.accept(restrictableNodesRef.get());
        errorHandler.verifyNoMoreErrors();

        if (!notifiedMultipleTimes.isEmpty()) {
            throw new AssertionError("Nodes were notified multiple times: " + notifiedMultipleTimes);
        }

        AtomicReference<TaskGraphExecutionResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        WaitableSignal signal = new WaitableSignal();
        future.whenComplete((result, error) -> {
            resultRef.set(result);
            errorRef.set(error);
            signal.signal();
        });

        if (!signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 10, TimeUnit.SECONDS)) {
            throw new AssertionError("timeout");
        }

        Throwable error = errorRef.get();
        testErrorHandler.accept(error);

        return resultRef.get();
    }

    private static DependencyDag<TaskNodeKey<?, ?>> doubleSplitLeafsGraph() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();
        graphBuilder.addNode(node("root"), (root) -> {
            root.addChild(node("child1"), (child1) -> {
                child1.addChild(node("child1.child1"));
                child1.addChild(node("child1.child2"));
            });
            root.addChild(node("child2"), (child2) -> {
                child2.addChild(node("child2.child1"));
                child2.addChild(node("child2.child2"));
            });
        });
        return new DependencyDag<>(graphBuilder.build());
    }

    private TaskGraphExecutionResult testDoubleSplitGraph(
            TaskExecutor executor,
            boolean computeAll,
            Consumer<? super RestrictableTaskGraphExecutor> executorConfig,
            Consumer<? super TestState> testAction) {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleSplitLeafsGraph();
        TaskNodes nodes = new TaskNodes(executor,
                "root", "child1", "child2", "child1.child1", "child1.child2", "child2.child1", "child2.child2");

        return testCustomExecution(graph, nodes.getAllNodes(), executorConfig, (restrictableNodes) -> {
            nodes.verifyNoneComputed();
            testAction.accept(new TestState(nodes, restrictableNodes));
            if (computeAll) {
                nodes.verifyAllComputed();
            }
        });
    }

    private TaskGraphExecutionResult testDoubleSplitGraph(
            TaskExecutor executor,
            Consumer<? super TestState> testAction) {
        return testDoubleSplitGraph(executor, true, Tasks.noOpConsumer(), testAction);
    }

    private TaskGraphExecutionResult testDoubleSplitGraphFails(
            TaskExecutor executor,
            Consumer<? super RestrictableTaskGraphExecutor> executorConfig,
            Consumer<? super TestState> testAction) {
        return testDoubleSplitGraph(executor, false, executorConfig, testAction);
    }

    @Test
    public void testFailingErrorHandler() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> dependencyGraph = new DirectedGraph.Builder<>();
        DependencyDag<TaskNodeKey<?, ?>> graph = new DependencyDag<>(dependencyGraph.build());

        TaskNodes nodes = new TaskNodes(SyncTaskExecutor.getSimpleExecutor(), "A", "B");
        RuntimeException nodeError = new RuntimeException("Test-Node-Error");
        nodes.setException("A", nodeError);
        List<TaskNode<?, ?>> taskNodes = nodes.getAllNodes();

        AtomicReference<RestrictableNodes> restrictableNodesRef = new AtomicReference<>();

        RestrictableTaskGraphExecutor executor;
        executor = new RestrictableTaskGraphExecutor(graph, taskNodes, (taskGraph, restrictableNodes) -> {
            RestrictableNodes testRestrictableNodes = new RestrictableNodes(null, restrictableNodes);
            restrictableNodesRef.set(testRestrictableNodes);
            return (TaskNodeKey<?, ?> nodeKey) -> { };
        });

        RuntimeException handlerError = new RuntimeException("Test-Handler-Error");
        executor.properties().setComputeErrorHandler((nodeKey, error) -> {
            throw handlerError;
        });
        executor.properties().setStopOnFailure(false);

        CompletionStage<TaskGraphExecutionResult> future;
        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2")) {
            future = executor.execute(cancel.getToken());

            RestrictableNodes restrictableNodes = restrictableNodesRef.get();
            restrictableNodes.release("A");

            boolean handlerErrorLogged = Arrays.stream(logs.getExceptions(Level.SEVERE))
                    .filter(error -> error == handlerError)
                    .findAny()
                    .isPresent();
            assertTrue("handlerErrorLogged", handlerErrorLogged);

            restrictableNodes.release("B");
        }

        nodes.verifyComputed("B");
    }

    @Test
    public void testNoNodes() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> dependencyGraph = new DirectedGraph.Builder<>();
        DependencyDag<TaskNodeKey<?, ?>> graph = new DependencyDag<>(dependencyGraph.build());

        List<TaskNode<?, ?>> taskNodes = Collections.emptyList();

        RestrictableTaskGraphExecutor executor;
        executor = new RestrictableTaskGraphExecutor(graph, taskNodes, (taskGraph, restrictableNodes) -> {
            return (TaskNodeKey<?, ?> nodeKey) -> { };
        });

        CompletionStage<TaskGraphExecutionResult> future = executor.execute(cancel.getToken());

        AtomicReference<TaskGraphExecutionResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        future.whenComplete((result, error) -> {
            resultRef.set(result);
            errorRef.set(error);
        });

        assertNotNull("result", resultRef.get());
        assertNull("error", errorRef.get());
    }

    @Test
    public void testNormalExecution() {
        testDoubleSplitGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.releaseAndExpectComputed("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.releaseAndExpectComputed("child2");

            testState.releaseAndExpectComputed("root");
        });
    }

    private static void executeAll(ManualTaskExecutor executor) {
        while (executor.executeCurrentlySubmitted() > 0) {
            // loop until there are no more
        }
    }

    @Test
    public void testNormalAsyncExecution() {
        ManualTaskExecutor executor = new ManualTaskExecutor(false);
        testDoubleSplitGraph(executor, (testState) -> {
            executeAll(executor);

            testState.releaseAndExpectComputedAsync(executor, "child1.child1");
            testState.releaseAndExpectComputedAsync(executor, "child1.child2");
            testState.releaseAndExpectComputedAsync(executor, "child1");

            testState.releaseAndExpectComputedAsync(executor, "child2.child1");
            testState.releaseAndExpectComputedAsync(executor, "child2.child2");
            testState.releaseAndExpectComputedAsync(executor, "child2");

            testState.releaseAndExpectComputedAsync(executor, "root");
        });
    }

    @Test
    public void testLeafReleasedLast() {
        testDoubleSplitGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.release("child1");
            testState.release("child2");
            testState.release("root");

            testState.nodes.verifyNoneComputed();

            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.verifyComputed("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.verifyComputed("child2");
        });
    }

    @Test
    public void testSplitLeafRelease() {
        testDoubleSplitGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.release("child1");
            testState.release("child2");
            testState.release("root");

            testState.nodes.verifyNoneComputed();

            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child2.child1");

            testState.verifyNotComputed("child2");
            testState.releaseAndExpectComputed("child2.child2");
            testState.verifyComputed("child2");

            testState.verifyNotComputed("root");

            testState.verifyNotComputed("child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.verifyComputed("child1");
        });
    }

    private static DependencyDag<TaskNodeKey<?, ?>> doubleConnectedRootGraph() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        graphBuilder.addNode(node("root1"), (root1) -> {
            root1.addChild(node("common"), (common) -> {
                common.addChild(node("common.child1"));
                common.addChild(node("common.child2"));
            });
        });

        graphBuilder.addNode(node("root2"), (root2) -> {
            root2.addChild(node("common"));
            root2.addChild(node("root2.child2"));
        });

        return new DependencyDag<>(graphBuilder.build());
    }

    private TaskGraphExecutionResult testDoubleConnectedRootGraph(
            TaskExecutor executor,
            Consumer<? super TestState> testAction) {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleConnectedRootGraph();
        TaskNodes nodes = new TaskNodes(executor,
                "root1", "root2", "common", "common.child1", "common.child2", "root2.child2");

        return testExecution(graph, nodes.getAllNodes(), (restrictableNodes) -> {
            nodes.verifyNoneComputed();
            testAction.accept(new TestState(nodes, restrictableNodes));
            nodes.verifyAllComputed();
        });
    }

    private TaskGraphExecutionResult testDoubleConnectedRootGraph(
            Consumer<? super TestState> testAction) {
        return testDoubleConnectedRootGraph(SyncTaskExecutor.getSimpleExecutor(), testAction);
    }

    @Test
    public void testPartiallyConnectedExecution() {
        testDoubleConnectedRootGraph((testState) -> {
            testState.release("common");
            testState.release("root1");
            testState.release("root2");

            testState.releaseAndExpectComputed("common.child2");
            testState.releaseAndExpectComputed("common.child1");

            testState.verifyComputed("common");
            testState.verifyComputed("root1");
            testState.verifyNotComputed("root2");

            testState.releaseAndExpectComputed("root2.child2");

            testState.verifyComputed("root2");
        });
    }

    private void expectedError(Class<? extends Throwable> expectedType) {
        testErrorHandler = (error) -> {
            if (!expectedType.isInstance(error)) {
                throw new AssertionError("Unexpected failure.", error);
            }
        };
    }

    private void expectedError() {
        expectedError(TaskGraphExecutionException.class);
    }

    @Test
    public void testFailChildDontStopExecution() {
        expectedError();

        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(false);
        }, (testState) -> {
            testState.release("child1");
            testState.release("child2");
            testState.release("root");

            testState.nodes.verifyNoneComputed();
            testState.releaseAndFail("child1.child1", new Exception("TEST-ERROR"));

            testState.releaseAndExpectComputed("child1.child2");
            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");

            testState.expectedUncomputedFinished("root");
            testState.expectedUncomputedFinished("child1");
            testState.verifyComputed("child2");
        });
    }

    @Test
    public void testFailChildStopExecution() {
        expectedError();

        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(true);
        }, (testState) -> {
            testState.release("child1");
            testState.release("child2");
            testState.release("root");

            testState.nodes.verifyNoneComputed();
            testState.releaseAndFail("child1.child1", new Exception("TEST-ERROR"));

            testState.release("child1.child2");
            testState.release("child2.child1");
            testState.release("child2.child2");

            testState.expectedUncomputedFinished("root");
            testState.expectedUncomputedFinished("child1");
            testState.expectedUncomputedFinished("child2");
            testState.expectedUncomputedFinished("child1.child2");
            testState.expectedUncomputedFinished("child2.child1");
            testState.expectedUncomputedFinished("child2.child2");
        });
    }

    @Test
    public void testCancellation() {
        expectedError(OperationCanceledException.class);

        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(false);
        }, (testState) -> {
            cancel.getController().cancel();

            testState.release("root");
            testState.release("child1");
            testState.release("child2");
            testState.release("child1.child1");
            testState.release("child1.child2");
            testState.release("child2.child1");
            testState.release("child2.child2");

            testState.expectedUncomputedFinished("root");
            testState.expectedUncomputedFinished("child1");
            testState.expectedUncomputedFinished("child2");
            testState.expectedUncomputedFinished("child1.child1");
            testState.expectedUncomputedFinished("child1.child2");
            testState.expectedUncomputedFinished("child2.child1");
            testState.expectedUncomputedFinished("child2.child2");
        });
    }

    private TaskGraphExecutionResult runTestFor(String nodeName) {
        return runTestFor(false, nodeName);
    }

    private TaskGraphExecutionResult runTestFor(boolean deliverResultOnFailure, String... nodeNames) {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        return testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(false);
            graphExecutor.properties().setDeliverResultOnFailure(deliverResultOnFailure);
            for (String nodeName : nodeNames) {
                graphExecutor.properties().addResultNodeKey(node(nodeName));
            }
        }, (testState) -> {
            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.releaseAndExpectComputed("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.releaseAndExpectComputed("child2");

            testState.releaseAndExpectComputed("root");
        });
    }

    private void verifyResult(TaskGraphExecutionResult result, String nodeName) {
        MockResult nodeResult = (MockResult) result.getResult(node(nodeName));
        assertEquals("nodeResult", nodeName, nodeResult.key);
    }

    private void verifyNotRequestedResult(TaskGraphExecutionResult result, String nodeName) {
        try {
            result.getResult(node(nodeName));
        } catch (IllegalArgumentException ex) {
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException for " + nodeName);
    }

    private void verifyErrorResult(TaskGraphExecutionResult result, String nodeName, Throwable expectedError) {
        try {
            result.getResult(node(nodeName));
        } catch (CompletionException ex) {
            assertSame("result", expectedError, ex.getCause());
            return;
        }

        throw new AssertionError("Expected CompletionException for " + nodeName);
    }

    private void verifyCanceledResult(TaskGraphExecutionResult result, String nodeName) {
        TestUtils.expectError(OperationCanceledException.class, () -> result.getResult(node(nodeName)));
    }

    private void verifySkippedResult(TaskGraphExecutionResult result, String nodeName) {
        TestUtils.expectError(TaskSkippedException.class, () -> result.getResult(node(nodeName)));
    }

    private void testExpectResult(String nodeName) {
        TaskGraphExecutionResult result = runTestFor(nodeName);
        assertEquals("result.getResultType", ExecutionResultType.SUCCESS, result.getResultType());
        verifyResult(result, nodeName);
    }

    private static RestrictableTaskGraphExecutor newEmptyExecutor() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> dependencies = new DirectedGraph.Builder<>();
        DependencyDag<TaskNodeKey<?, ?>> graph = new DependencyDag<>(dependencies.build());
        return new RestrictableTaskGraphExecutor(
                graph,
                Collections.emptyList(),
                TaskExecutionRestrictionStrategies.eagerStrategy());
    }

    @Test
    public void testEmptyGraph() {
        RestrictableTaskGraphExecutor executor = newEmptyExecutor();
        CompletionStage<TaskGraphExecutionResult> executeResult = executor.execute(Cancellation.UNCANCELABLE_TOKEN);

        Runnable completed = mock(Runnable.class);
        executeResult.whenComplete((result, error) -> completed.run());
        verify(completed).run();
    }

    @Test
    public void testEmptyGraphRepeatedEXecution() {
        RestrictableTaskGraphExecutor executor = newEmptyExecutor();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN);

        try {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN);
        } catch (IllegalStateException ex) {
            return;
        }
        throw new AssertionError("Exepcted: IllegalStateException");
    }

    @Test
    public void testExpectResult() {
        testExpectResult("child1");
        testExpectResult("child2");
        testExpectResult("child2.child1");
        testExpectResult("root");
    }

    private void testQueryUnexpected(String requested, String queried) {
        TaskGraphExecutionResult result = runTestFor(requested);
        verifyNotRequestedResult(result, queried);
    }

    @Test
    public void testQueryUnexpected() {
        testQueryUnexpected("child1", "child1.child1");
        testQueryUnexpected("child1", "child2");
        testQueryUnexpected("child1", "root");
        testQueryUnexpected("root", "child1");
    }

    @Test
    public void testTaskSkippedResult() {
        TaskSkippedException skipException = new TaskSkippedException("TEST-SKIP");
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        TaskGraphExecutionResult result = testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(true);
            graphExecutor.properties().setDeliverResultOnFailure(false);
            for (String nodeName : new String[]{"child1.child1", "child1", "root", "child2"}) {
                graphExecutor.properties().addResultNodeKey(node(nodeName));
            }
        }, (testState) -> {
            testState.releaseAndSkip("child1.child1", skipException);
            testState.release("child1.child2");
            testState.release("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.releaseAndExpectComputed("child2");

            testState.release("root");
        });

        assertEquals("result.getResultType", ExecutionResultType.SUCCESS, result.getResultType());

        verifyResult(result, "child2");
        verifySkippedResult(result, "root");
        verifySkippedResult(result, "child1");
        verifySkippedResult(result, "child1.child1");
        verifyNotRequestedResult(result, "child1.child2");
        verifyNotRequestedResult(result, "child2.child1");
        verifyNotRequestedResult(result, "child2.child2");
    }

    @Test
    public void testFailedResult() {
        Exception testFailure = new Exception("TEST-FAILURE");
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        TaskGraphExecutionResult result = testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(false);
            graphExecutor.properties().setDeliverResultOnFailure(true);
            for (String nodeName : new String[]{"child1.child1", "child1", "root", "child2"}) {
                graphExecutor.properties().addResultNodeKey(node(nodeName));
            }
        }, (testState) -> {
            testState.releaseAndFail("child1.child1", testFailure);
            testState.release("child1.child2");
            testState.release("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.releaseAndExpectComputed("child2");

            testState.release("root");
        });

        assertEquals("result.getResultType", ExecutionResultType.ERRORED, result.getResultType());

        verifyResult(result, "child2");
        verifyErrorResult(result, "root", testFailure);
        verifyErrorResult(result, "child1", testFailure);
        verifyErrorResult(result, "child1.child1", testFailure);
        verifyNotRequestedResult(result, "child1.child2");
        verifyNotRequestedResult(result, "child2.child1");
        verifyNotRequestedResult(result, "child2.child2");
    }

    @Test
    public void testCanceledResult() {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        TaskGraphExecutionResult result = testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(false);
            graphExecutor.properties().setDeliverResultOnFailure(true);
            for (String nodeName : new String[]{"child1.child1", "child1", "root", "child2"}) {
                graphExecutor.properties().addResultNodeKey(node(nodeName));
            }
        }, (testState) -> {
            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.releaseAndExpectComputed("child1");

            cancel.getController().cancel();

            testState.release("child2.child1");
            testState.release("child2.child2");
            testState.release("child2");
            testState.release("root");
        });

        assertEquals("result.getResultType", ExecutionResultType.CANCELED, result.getResultType());

        verifyCanceledResult(result, "child2");
        verifyCanceledResult(result, "root");
        verifyResult(result, "child1");
        verifyResult(result, "child1.child1");
        verifyNotRequestedResult(result, "child1.child2");
        verifyNotRequestedResult(result, "child2.child1");
        verifyNotRequestedResult(result, "child2.child2");
    }

    @Test
    public void testFailureResultWithStopOnFailure() {
        Exception testFailure = new Exception("TEST-FAILURE");
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        TaskGraphExecutionResult result = testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(true);
            graphExecutor.properties().setDeliverResultOnFailure(true);
            for (String nodeName : new String[]{"child1.child1", "child1", "root", "child2"}) {
                graphExecutor.properties().addResultNodeKey(node(nodeName));
            }
        }, (testState) -> {
            testState.releaseAndFail("child1.child1", testFailure);
            testState.release("child1.child2");
            testState.release("child1");

            testState.release("child2.child1");
            testState.release("child2.child2");
            testState.release("child2");

            testState.release("root");
        });

        assertEquals("result.getResultType", ExecutionResultType.ERRORED, result.getResultType());

        verifyCanceledResult(result, "child2");
        verifyErrorResult(result, "root", testFailure);
        verifyErrorResult(result, "child1", testFailure);
        verifyErrorResult(result, "child1.child1", testFailure);
        verifyNotRequestedResult(result, "child1.child2");
        verifyNotRequestedResult(result, "child2.child1");
        verifyNotRequestedResult(result, "child2.child2");
    }

    private static RestrictableTaskGraphExecutor createDummyExecutor() {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleSplitLeafsGraph();

        Object[] nodeKeys = {
            "root", "child1", "child2", "child1.child1", "child1.child2", "child2.child1", "child2.child2"
        };
        TaskNodes nodes = new TaskNodes(SyncTaskExecutor.getSimpleExecutor(), nodeKeys);

        return new RestrictableTaskGraphExecutor(
                graph,
                nodes.getAllNodes(),
                TaskExecutionRestrictionStrategies.eagerStrategy());
    }

    @Test
    public void testGetBuiltGraph() {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleSplitLeafsGraph();

        Object[] nodeKeys = {
            "X", "root", "child1", "child2", "child1.child1", "child1.child2", "child2.child1", "child2.child2"
        };
        TaskNodes nodes = new TaskNodes(SyncTaskExecutor.getSimpleExecutor(), nodeKeys);

        RestrictableTaskGraphExecutor executor = new RestrictableTaskGraphExecutor(
                graph,
                nodes.getAllNodes(),
                TaskExecutionRestrictionStrategies.eagerStrategy());

        BuiltGraph builtGraph = executor.getBuiltGraph();
        assertEquals("nodes",
                Arrays.stream(nodeKeys).map(TestNodes::node).collect(Collectors.toSet()),
                builtGraph.getNodes());
        assertSame("graph", graph, builtGraph.getGraph());
    }

    @Test
    public void testIllegalGetBuiltGraph() {
        RestrictableTaskGraphExecutor executor = createDummyExecutor();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN);
        TestUtils.expectError(IllegalStateException.class, () -> {
            executor.getBuiltGraph();
        });
    }

    @Test
    public void testFutureOf() {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleSplitLeafsGraph();

        Object[] nodeKeys = {
            "X", "root", "child1", "child2", "child1.child1", "child1.child2", "child2.child1", "child2.child2"
        };
        TaskNodes nodes = new TaskNodes(SyncTaskExecutor.getSimpleExecutor(), nodeKeys);

        RestrictableTaskGraphExecutor executor = new RestrictableTaskGraphExecutor(
                graph,
                nodes.getAllNodes(),
                TaskExecutionRestrictionStrategies.eagerStrategy());

        for (Object key : nodeKeys) {
            CompletionStage<Object> executorFuture = executor.futureOf(node(key));
            if (executorFuture != nodes.getFutureFor(key)) {
                throw new AssertionError("Wrong future for " + key);
            }
        }

        TestUtils.expectError(IllegalArgumentException.class, () -> {
            executor.futureOf(node("MissingNode"));
        });
    }

    @Test
    public void testIllegalFutureOf() {
        RestrictableTaskGraphExecutor executor = createDummyExecutor();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN);
        TestUtils.expectError(IllegalStateException.class, () -> {
            executor.futureOf(node("root"));
        });
    }

    private static final class CollectorErrorHandler implements TaskErrorHandler {
        private final Map<TaskNodeKey<?, ?>, Throwable> errors;
        private AssertionError overwrittenErrors;

        public CollectorErrorHandler() {
            this.errors = new HashMap<>();
            this.overwrittenErrors = null;
        }

        public void verifyAndRemoveError(Object key, Throwable expected) {
            Throwable receivedError = errors.remove(node(key));
            if (receivedError == null) {
                throw new AssertionError("There is no error associated with key: " + key);
            }
            if (receivedError != expected) {
                throw new AssertionError("Unexpected error associated with key: " + key, receivedError);
            }
        }

        public void verifyNoMoreErrors() {
            if (!errors.isEmpty()) {
                throw new AssertionError("There are unchecked errors: " + errors.keySet());
            }

            if (overwrittenErrors != null) {
                throw overwrittenErrors;
            }
        }

        @Override
        public void onError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            Throwable prevError = errors.putIfAbsent(nodeKey, error);
            if (prevError != null) {
                if (overwrittenErrors == null) {
                    overwrittenErrors = new AssertionError("There are overwritten errors.");
                }
                overwrittenErrors.addSuppressed(prevError);
            }
        }
    }

    private static final class TestState {
        private final TaskNodes nodes;
        private final RestrictableNodes restrictableNodes;

        public TestState(TaskNodes nodes, RestrictableNodes restrictableNodes) {
            this.nodes = nodes;
            this.restrictableNodes = restrictableNodes;
        }

        public CollectorErrorHandler errorHandler() {
            return restrictableNodes.errorHandler;
        }

        public void expectedUncomputedFinished(Object key) {
            verifyFinished(key);
            verifyNotComputed(key);
        }

        public void release(Object key) {
            restrictableNodes.release(key);
        }

        public void releaseAndFail(Object key, Exception error) {
            setException(key, error);
            release(key);
            verifyComputed(key);
            errorHandler().verifyAndRemoveError(key, error);
        }

        public void releaseAndSkip(Object key, TaskSkippedException exception) {
            setException(key, exception);
            release(key);
            verifyComputed(key);
        }

        public void releaseButNotComputed(Object key) {
            restrictableNodes.release(key);
            verifyNotComputed(key);
        }

        public void releaseAndExpectComputed(Object key) {
            verifyNotComputed(key);
            restrictableNodes.release(key);
            verifyComputed(key);
        }

        public void releaseAndExpectComputedAsync(ManualTaskExecutor executor, Object key) {
            verifyNotComputed(key);

            restrictableNodes.release(key);
            executeAll(executor);

            verifyComputed(key);
        }

        public void verifyFinished(Object key) {
            boolean removed = restrictableNodes.computed.remove(node(key));
            if (!removed) {
                throw new AssertionError("Expected finished for " + key);
            }
        }

        public void verifyComputed(Object key) {
            nodes.verifyComputed(key);
            verifyFinished(key);
        }

        public void verifyNotComputed(Object key) {
            nodes.verifyNotComputed(key);
        }

        public void setException(Object key, Exception exception) {
            nodes.setException(key, exception);
        }
    }

    private static final class TaskNodes {
        private final Map<Object, TestTaskNode> tasks;

        public TaskNodes(TaskExecutor executor, Object... keys) {
            this.tasks = CollectionsEx.newHashMap(keys.length);

            TaskNodeProperties.Builder propertiesBuilder = new TaskNodeProperties.Builder();
            propertiesBuilder.setExecutor(executor);
            TaskNodeProperties properties = propertiesBuilder.build();

            for (Object key: keys) {
                TestTaskNode node = new TestTaskNode(node(key), properties);
                this.tasks.put(key, node);
            }
        }

        public CompletionStage<?> getFutureFor(Object key) {
            TestTaskNode taskNode = tasks.get(key);
            if (taskNode == null) {
                throw new AssertionError("No TaskNode: " + key);
            }
            return taskNode.node.taskFuture();
        }

        public List<TaskNode<?, ?>> getAllNodes() {
            return tasks.values().stream()
                    .map(TestTaskNode::getNode)
                    .collect(Collectors.toList());
        }

        private TestTaskNode getNode(Object key) {
            TestTaskNode node = tasks.get(key);
            if (node == null) {
                throw new AssertionError("Missing task for key: " + key);
            }
            return node;
        }

        public void verifyNoneComputed() {
            tasks.values().forEach((node) -> {
                node.verifyNotRun();
            });
        }

        public void verifyAllComputed() {
            tasks.values().forEach((node) -> {
                node.verifyRun();
            });
        }

        public void verifyNotComputed(Object key) {
            getNode(key).verifyNotRun();
        }

        public void verifyComputed(Object key) {
            getNode(key).verifyRun();
        }

        public void setException(Object key, Exception exception) {
            TestTaskNode node = getNode(key);
            node.verifyNotRun();
            node.setException(exception);
        }
    }

    private static final class TestTaskNode {
        private final TaskNode<?, ?> node;
        private final MockFunction taskFunction;

        public <I> TestTaskNode(
                TaskNodeKey<Object, I> key,
                TaskNodeProperties properties) {
            MockFunction mockFunction = new MockFunction(key.getFactoryArg());
            this.taskFunction = mockFunction;
            this.node = new TaskNode<>(key, new NodeTaskRef<>(properties, mockFunction));
        }

        public TaskNode<?, ?> getNode() {
            return node;
        }

        public void setException(Exception exception) {
            taskFunction.setException(exception);
        }

        public void verifyRun() {
            taskFunction.verifyRun();
        }

        public void verifyNotRun() {
            taskFunction.verifyNotRun();
        }
    }

    private static final class RestrictableNodes {
        private final Map<TaskNodeKey<?, ?>, RestrictableNode> nodes;
        private final Set<TaskNodeKey<?, ?>> computed;
        private final CollectorErrorHandler errorHandler;

        public RestrictableNodes(CollectorErrorHandler errorHandler, Iterable<? extends RestrictableNode> nodes) {
            this.errorHandler = errorHandler;
            this.computed = new HashSet<>();
            this.nodes = new HashMap<>();
            nodes.forEach((node) -> {
                this.nodes.put(node.getNodeKey(), node);
            });
        }

        public boolean setComputed(TaskNodeKey<?, ?> nodeKey) {
            return computed.add(nodeKey);
        }

        public void release(Object key) {
            RestrictableNode restrictableNode = nodes.get(node(key));
            if (restrictableNode == null) {
                throw new AssertionError("Missing restrictable node: " + key);
            }

            restrictableNode.getReleaseAction().run();
        }
    }

    private static final class MockResult {
        private final Object key;

        public MockResult(Object key) {
            this.key = key;
        }
    }

    private static final class MockFunction implements CancelableFunction<MockResult> {
        private final Object key;
        private Exception exception;
        private final AtomicInteger callCount;

        public MockFunction(Object key) {
            this.key = key;
            this.exception = null;
            this.callCount = new AtomicInteger(0);
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        @Override
        public MockResult execute(CancellationToken cancelToken) throws Exception {
            callCount.incrementAndGet();
            if (exception != null) {
                throw exception;
            }
            return new MockResult(key);
        }

        public void verifyRun() {
            int currentCallCount = callCount.get();
            if (currentCallCount == 0) {
                throw new AssertionError("Expected a call but none received: " + key);
            }
            if (currentCallCount != 1) {
                throw new AssertionError("Expected exactly a single call but received "
                        + currentCallCount + ": " + key);
            }
        }

        public void verifyNotRun() {
            int currentCallCount = callCount.get();
            if (currentCallCount != 0) {
                throw new AssertionError("Not expected a call but received "
                        + currentCallCount + ": " + key);
            }
        }
    }
}
