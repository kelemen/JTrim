package org.jtrim.taskgraph.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.ManualTaskExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.taskgraph.TaskErrorHandler;
import org.jtrim.taskgraph.TaskGraphExecutionException;
import org.jtrim.taskgraph.TaskGraphExecutionResult;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.taskgraph.TaskNodeProperties;
import org.jtrim.utils.ExceptionHelper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RestrictableTaskGraphExecutorTest {
    private CancellationSource cancel;
    private Consumer<? super Throwable> testErrorHandler;

    @Before
    public void setup() {
        this.cancel = Cancellation.createCancellationSource();
        this.testErrorHandler = (error) -> {
            if (error != null) {
                throw ExceptionHelper.throwUnchecked(error);
            }
        };
    }

    private static TaskNodeKey<Object, Object> node(Object key) {
        return AbstractTaskExecutionRestrictionStrategyFactoryTest.node(key);
    }

    private TaskGraphExecutionResult testExecution(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends TaskNode<?, ?>> taskNodes,
            Consumer<? super RestrictableNodes> releaseAction) {
        return testCustomExecution(graph, taskNodes, (executor) -> { }, releaseAction);
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
        return testDoubleSplitGraph(executor, true, (graphExecutor) -> { }, testAction);
    }

    private TaskGraphExecutionResult testDoubleSplitGraphFails(
            TaskExecutor executor,
            Consumer<? super RestrictableTaskGraphExecutor> executorConfig,
            Consumer<? super TestState> testAction) {
        return testDoubleSplitGraph(executor, false, executorConfig, testAction);
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
            testState.releaseAdFail("child1.child1", new Exception("TEST-ERROR"));

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
            testState.releaseAdFail("child1.child1", new Exception("TEST-ERROR"));

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
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        return testDoubleSplitGraphFails(executor, (graphExecutor) -> {
            graphExecutor.properties().setStopOnFailure(false);
            graphExecutor.properties().addResultNodeKey(node(nodeName));
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

    private void testExpectResult(String nodeName) {
        TaskGraphExecutionResult result = runTestFor(nodeName);
        MockResult nodeResult = (MockResult)result.getResult(node(nodeName));
        assertEquals("nodeResult", nodeName, nodeResult.key);
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

        try {
            result.getResult(node(queried));
        } catch (IllegalArgumentException ex) {
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException for " + requested + ", " + queried);
    }

    @Test
    public void testQueryUnexpected() {
        testQueryUnexpected("child1", "child1.child1");
        testQueryUnexpected("child1", "child2");
        testQueryUnexpected("child1", "root");
        testQueryUnexpected("root", "child1");
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

        public void releaseAdFail(Object key, Exception error) {
            setException(key, error);
            release(key);
            verifyComputed(key);
            errorHandler().verifyAndRemoveError(key, error);
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
