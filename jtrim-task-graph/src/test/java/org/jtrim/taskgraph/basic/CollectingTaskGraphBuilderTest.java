package org.jtrim.taskgraph.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.taskgraph.TaskErrorHandler;
import org.jtrim.taskgraph.TaskFactory;
import org.jtrim.taskgraph.TaskFactoryConfig;
import org.jtrim.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim.taskgraph.TaskFactoryKey;
import org.jtrim.taskgraph.TaskFactorySetup;
import org.jtrim.taskgraph.TaskGraphBuilder;
import org.jtrim.taskgraph.TaskGraphExecutionResult;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskGraphExecutorProperties;
import org.jtrim.taskgraph.TaskInputRef;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.LogCollector;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollectingTaskGraphBuilderTest {
    private static Supplier<TestTaskGraphExecutor> testBuildGraph(
            CancellationToken cancelToken,
            Collection<TaskFactoryConfig<?, ?>> configs,
            Consumer<TaskGraphBuilder> graphBuilderConfigurer) {

        CollectingTaskGraphBuilder graphBuilder = new CollectingTaskGraphBuilder(configs, (taskGraph, nodes) -> {
            return new TestTaskGraphExecutor(taskGraph, nodes);
        });

        graphBuilderConfigurer.accept(graphBuilder);

        AtomicReference<TestTaskGraphExecutor> graphExecutorRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        graphBuilder
                .buildGraph(cancelToken)
                .whenComplete((graphExecutor, error) -> {
                    graphExecutorRef.set((TestTaskGraphExecutor)graphExecutor);
                    errorRef.set(error);
                });

        return () -> {
            Throwable error = errorRef.get();
            if (error != null) {
                throw ExceptionHelper.throwUnchecked(error);
            }
            return graphExecutorRef.get();
        };
    }

    private static TaskFactoryKey<Object, Object> factoryKey(Object key) {
        return new TaskFactoryKey<>(Object.class, Object.class, key);
    }

    private static TaskNodeKey<Object, Object> nodeKey(Object factoryKey, Object nodeKey) {
        return new TaskNodeKey<>(factoryKey(factoryKey), nodeKey);
    }

    @Test
    public void testSingleFactorySingleNode() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", leafFactory());

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
            graphBuilder.addNode(nodeKey("F1", "N1"));
        });

        TestTaskGraphExecutor graphExecutor = graphExecutorRef.get();
        assertNotNull(graphExecutor);

        graphExecutor.verifyGraph((graphBuilder) -> { });
        graphExecutor.expectedNodeCount(1);

        TaskNode<?, ?> node1 = graphExecutor.node("F1", "N1");
        assertFalse(node1.hasResult());

        factoryBuilder.verified();
    }

    @Test
    public void testLineDepdency() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", (cancelToken, nodeDef) -> {
            int arg = (int)nodeDef.factoryArg();

            List<TaskInputRef<?>> inputs = new ArrayList<>();
            if (arg > 0) {
                inputs.add(nodeDef.inputs().bindInput(nodeKey("F1", arg - 1)));
            }

            return new TestTask(nodeDef.factoryArg(), inputs);
        });

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
            graphBuilder.addNode(nodeKey("F1", 2));
        });

        TestTaskGraphExecutor graphExecutor = graphExecutorRef.get();
        assertNotNull(graphExecutor);

        graphExecutor.verifyGraph((graphBuilder) -> {
            graphBuilder.setEdges(nodeKey("F1", 2), nodeKey("F1", 1));
            graphBuilder.setEdges(nodeKey("F1", 1), nodeKey("F1", 0));
        });
        graphExecutor.expectedNodeCount(3);

        TestOutput result0 = graphExecutor.computeAndVerifyResult("F1", 0);
        TestOutput result1 = graphExecutor.computeAndVerifyResult("F1", 1, result0);
        graphExecutor.computeAndVerifyResult("F1", 2, result1);

        factoryBuilder.verified();
    }

    private static TaskFactory<Object, Object> leafFactory() {
        return (cancelToken, nodeDef) -> {
            return new TestTask(nodeDef.factoryArg(), Collections.emptyList());
        };
    }

    private static TaskFactory<Object, Object> failingFactory() {
        return (cancelToken, nodeDef) -> {
            throw new TestFactoryException(nodeDef.factoryArg());
        };
    }

    private static TaskFactory<Object, Object> splitFactory(String dependency) {
        return splitFactory(dependency, 2);
    }

    private static TaskFactory<Object, Object> splitFactory(String dependency, int childCount) {
        return (cancelToken, nodeDef) -> {
            String arg = nodeDef.factoryArg().toString();

            List<TaskInputRef<?>> inputs = new ArrayList<>();
            for (int i = 0; i < childCount; i++) {
                char suffixCh = (char)('a' + i);
                inputs.add(nodeDef.inputs().bindInput(nodeKey(dependency, arg + "." + suffixCh)));
            }

            return new TestTask(nodeDef.factoryArg(), inputs);
        };
    }

    @Test
    public void testTreeDependency() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", splitFactory("F2"));
        factoryBuilder.addSimpleConfig("F2", splitFactory("F3"));
        factoryBuilder.addSimpleConfig("F3", leafFactory());

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
            graphBuilder.addNode(nodeKey("F1", "r"));
        });

        TestTaskGraphExecutor graphExecutor = graphExecutorRef.get();
        assertNotNull(graphExecutor);

        graphExecutor.verifyGraph((graphBuilder) -> {
            graphBuilder.setEdges(nodeKey("F1", "r"), nodeKey("F2", "r.a"), nodeKey("F2", "r.b"));
            graphBuilder.setEdges(nodeKey("F2", "r.a"), nodeKey("F3", "r.a.a"), nodeKey("F3", "r.a.b"));
            graphBuilder.setEdges(nodeKey("F2", "r.b"), nodeKey("F3", "r.b.a"), nodeKey("F3", "r.b.b"));
        });
        graphExecutor.expectedNodeCount(7);

        TestOutput result0 = graphExecutor.computeAndVerifyResult("F3", "r.a.a");
        TestOutput result1 = graphExecutor.computeAndVerifyResult("F3", "r.a.b");
        TestOutput result2 = graphExecutor.computeAndVerifyResult("F2", "r.a", result0, result1);

        TestOutput result3 = graphExecutor.computeAndVerifyResult("F3", "r.b.a");
        TestOutput result4 = graphExecutor.computeAndVerifyResult("F3", "r.b.b");
        TestOutput result5 = graphExecutor.computeAndVerifyResult("F2", "r.b", result3, result4);

        graphExecutor.computeAndVerifyResult("F1", "r", result2, result5);

        factoryBuilder.verified();
    }

    @Test
    public void testDagDependency() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("R1", (cancelToken, nodeDef) -> {
            List<TaskInputRef<?>> inputs = new ArrayList<>();
            inputs.add(nodeDef.inputs().bindInput(nodeKey("C", "c")));

            return new TestTask(nodeDef.factoryArg(), inputs);
        });
        factoryBuilder.addSimpleConfig("R2", (cancelToken, nodeDef) -> {
            List<TaskInputRef<?>> inputs = new ArrayList<>();
            inputs.add(nodeDef.inputs().bindInput(nodeKey("C", "c")));
            inputs.add(nodeDef.inputs().bindInput(nodeKey("F3", "r2.x")));

            return new TestTask(nodeDef.factoryArg(), inputs);
        });
        factoryBuilder.addSimpleConfig("C", splitFactory("F3"));
        factoryBuilder.addSimpleConfig("F3", leafFactory());

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
            graphBuilder.addNode(nodeKey("R1", "r1"));
            graphBuilder.addNode(nodeKey("R2", "r2"));
        });

        TestTaskGraphExecutor graphExecutor = graphExecutorRef.get();
        assertNotNull(graphExecutor);

        graphExecutor.verifyGraph((graphBuilder) -> {
            graphBuilder.setEdges(nodeKey("R1", "r1"), nodeKey("C", "c"));
            graphBuilder.setEdges(nodeKey("R2", "r2"), nodeKey("C", "c"), nodeKey("F3", "r2.x"));
            graphBuilder.setEdges(nodeKey("C", "c"), nodeKey("F3", "c.a"), nodeKey("F3", "c.b"));
        });
        graphExecutor.expectedNodeCount(6);

        TestOutput result0 = graphExecutor.computeAndVerifyResult("F3", "c.a");
        TestOutput result1 = graphExecutor.computeAndVerifyResult("F3", "c.b");
        TestOutput result2 = graphExecutor.computeAndVerifyResult("C", "c", result0, result1);

        TestOutput result3 = graphExecutor.computeAndVerifyResult("F3", "r2.x");

        graphExecutor.computeAndVerifyResult("R1", "r1", result2);
        graphExecutor.computeAndVerifyResult("R2", "r2", result2, result3);

        factoryBuilder.verified();
    }

    @Test
    public void testMissingNode() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", leafFactory());

        CollectingTaskGraphBuilder graphBuilder;
        graphBuilder = new CollectingTaskGraphBuilder(factoryBuilder.configs, (taskGraph, nodes) -> {
            return new TestTaskGraphExecutor(taskGraph, nodes);
        });

        TaskNodeKey<Object, Object> node = nodeKey("X", "r");
        try {
            graphBuilder.addNode(node);
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            assertTrue(message.contains(node.toString()));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    @Test
    public void testAddNodeTwice() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", leafFactory());

        CollectingTaskGraphBuilder graphBuilder;
        graphBuilder = new CollectingTaskGraphBuilder(factoryBuilder.configs, (taskGraph, nodes) -> {
            return new TestTaskGraphExecutor(taskGraph, nodes);
        });

        TaskNodeKey<Object, Object> node = nodeKey("F1", "r");
        graphBuilder.addNode(node);

        try {
            graphBuilder.addNode(node);
        } catch (IllegalStateException ex) {
            String message = ex.getMessage();
            assertTrue(message.contains(node.toString()));
            return;
        }
        throw new AssertionError("Expected IllegalStateException");
    }

    @Test
    public void testFailInTheMiddle() {
        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", splitFactory("F2", 1));
        factoryBuilder.addSimpleConfig("F2", failingFactory());
        factoryBuilder.addSimpleConfig("F3", leafFactory());

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
            graphBuilder.addNode(nodeKey("F1", "r"));
        });

        Consumer<Throwable> errorVerifier = (error) -> {
            Object factoryArg = ((TestFactoryException)error).getFactoryArg();
            assertEquals("factoryArg", "r.a", factoryArg);
        };
        factoryBuilder.verifyError(nodeKey("F2", "r.a"), errorVerifier);
        factoryBuilder.verified();

        try {
            graphExecutorRef.get();
        } catch (TestFactoryException ex) {
            errorVerifier.accept(ex);
            return;
        }
        throw new AssertionError("Expected TestFactoryException");
    }

    @Test
    public void testFailInTheMiddleWithoutHandler() {
        FactoryBuilder factoryBuilder = new FactoryBuilder(null);
        factoryBuilder.addSimpleConfig("F1", splitFactory("F2", 1));
        factoryBuilder.addSimpleConfig("F2", failingFactory());
        factoryBuilder.addSimpleConfig("F3", leafFactory());

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
            graphBuilder.addNode(nodeKey("F1", "r"));
        });

        try {
            graphExecutorRef.get();
        } catch (TestFactoryException ex) {
            assertEquals("factoryArg", "r.a", ex.getFactoryArg());
            return;
        }
        throw new AssertionError("Expected TestFactoryException");
    }

    @Test
    public void testFailInTheMiddleWithBuggyHandler() {
        RuntimeException handlerError = new RuntimeException("testFailInTheMiddleWithBuggyHandler");
        FactoryBuilder factoryBuilder = new FactoryBuilder((TaskNodeKey<?, ?> nodeKey, Throwable error) -> {
            throw handlerError;
        });

        factoryBuilder.addSimpleConfig("F1", splitFactory("F2", 1));
        factoryBuilder.addSimpleConfig("F2", failingFactory());
        factoryBuilder.addSimpleConfig("F3", leafFactory());

        try (LogCollector logs = LogCollector.startCollecting("org.jtrim")) {
            Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test((graphBuilder) -> {
                graphBuilder.addNode(nodeKey("F1", "r"));
            });

            boolean wasLogged = Arrays.stream(logs.getExceptions(Level.SEVERE))
                    .filter(ex -> ex == handlerError)
                    .findAny()
                    .isPresent();
            if (!wasLogged) {
                throw new AssertionError("Expected log for handler error.", handlerError);
            }

            try {
                graphExecutorRef.get();
            } catch (TestFactoryException ex) {
                assertEquals("factoryArg", "r.a", ex.getFactoryArg());
                return;
            }
        }
        throw new AssertionError("Expected TestFactoryException");
    }

    @Test
    public void testCanceledInTheMiddle() {
        CancellationSource cancel = Cancellation.createCancellationSource();

        FactoryBuilder factoryBuilder = new FactoryBuilder();
        factoryBuilder.addSimpleConfig("F1", splitFactory("F2", 1));
        factoryBuilder.addSimpleConfig("F2", (cancelToken, nodeDef) -> {
            cancel.getController().cancel();
            return splitFactory("F3").createTaskNode(cancelToken, nodeDef);
        });
        factoryBuilder.addSimpleConfig("F3", leafFactory());

        Supplier<TestTaskGraphExecutor> graphExecutorRef = factoryBuilder.test(cancel.getToken(), (graphBuilder) -> {
            graphBuilder.addNode(nodeKey("F1", "r"));
        });

        try {
            graphExecutorRef.get();
        } catch (OperationCanceledException ex) {
            return;
        }
        throw new AssertionError("Expected OperationCanceledException");
    }

    private static TestOutput verifyResult(TaskNode<?, ?> node, Object arg, Object... inputs) {
        TestOutput result = (TestOutput)node.getResult();
        assertNotNull(result);

        return verifyResult(result, arg, inputs);
    }

    private static TestOutput verifyResult(TestOutput result, Object arg, Object... inputs) {
        assertEquals("result.arg", arg, result.getArg());
        assertEquals("result.inputs", Arrays.asList(inputs), result.getInputs());

        return result;
    }

    private static class TestFactoryException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final Object factoryArg;

        public TestFactoryException(Object factoryArg) {
            this.factoryArg = factoryArg;
        }

        public Object getFactoryArg() {
            return factoryArg;
        }
    }

    private static final class TestTask implements CancelableFunction<Object> {
        private final Object arg;
        private final List<TaskInputRef<?>> inputs;

        public TestTask(Object arg, List<TaskInputRef<?>> inputs) {
            this.arg = arg;
            this.inputs = inputs;
        }

        @Override
        public Object execute(CancellationToken cancelToken) throws Exception {
            List<Object> consumedInputs = new ArrayList<>(inputs.size());
            inputs.forEach(inputRef -> consumedInputs.add(inputRef.consumeInput()));
            return new TestOutput(arg, consumedInputs);
        }
    }

    private static final class TestOutput {
        private final Object arg;
        private final List<Object> inputs;

        public TestOutput(Object arg, List<Object> inputs) {
            this.arg = arg;
            this.inputs = new ArrayList<>(inputs);
        }

        public Object getArg() {
            return arg;
        }

        public List<Object> getInputs() {
            return inputs;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(arg);
            hash = 53 * hash + Objects.hashCode(inputs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TestOutput other = (TestOutput)obj;
            return Objects.equals(this.arg, other.arg)
                    && Objects.equals(this.inputs, other.inputs);
        }

        @Override
        public String toString() {
            return "TestOutput{" + "arg=" + arg + ", inputs=" + inputs + '}';
        }
    }

    private static final class FactoryBuilder {
        private final List<TaskFactoryConfig<?, ?>> configs;
        private final Map<TaskNodeKey<?, ?>, Throwable> errors;
        private final Set<TaskNodeKey<?, ?>> multiErrors;
        private final TaskErrorHandler errorHandler;

        public FactoryBuilder() {
            this.configs = new ArrayList<>();
            this.errors = new HashMap<>();
            this.multiErrors = new HashSet<>();
            this.errorHandler = this::handleError;
        }

        public FactoryBuilder(TaskErrorHandler errorHandler) {
            this.configs = new ArrayList<>();
            this.errors = new HashMap<>();
            this.multiErrors = new HashSet<>();
            this.errorHandler = errorHandler;
        }

        public void addSimpleConfig(
                Object factoryKey,
                TaskFactory<Object, Object> factory) {
            addSimpleConfig(factoryKey, properties -> { }, factory);
        }

        public void addSimpleConfig(
                Object factoryKey,
                TaskFactoryGroupConfigurer groupConfig,
                TaskFactory<Object, Object> factory) {
            addConfig(factoryKey, groupConfig, properties -> factory);
        }

        public void addConfig(
                Object factoryKey,
                TaskFactoryGroupConfigurer groupConfig,
                TaskFactorySetup<Object, Object> setup) {
            configs.add(new TaskFactoryConfig<>(factoryKey(factoryKey), groupConfig, setup));
        }

        public Supplier<TestTaskGraphExecutor> test(
                Consumer<TaskGraphBuilder> graphBuilderConfigurer) {
            return test(Cancellation.UNCANCELABLE_TOKEN, graphBuilderConfigurer);
        }

        public Supplier<TestTaskGraphExecutor> test(
                CancellationToken cancelToken,
                Consumer<TaskGraphBuilder> graphBuilderConfigurer) {
            return testBuildGraph(cancelToken, configs, (graphBuilder) -> {
                if (errorHandler != null) {
                    graphBuilder.properties().setNodeCreateErrorHandler(errorHandler);
                }
                graphBuilderConfigurer.accept(graphBuilder);
            });
        }

        private void handleError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            if (errors.putIfAbsent(nodeKey, error) != null) {
                multiErrors.add(nodeKey);
            }
        }

        public void verifyError(TaskNodeKey<?, ?> nodeKey, Consumer<Throwable> errorHandler) {
            Throwable error = errors.remove(nodeKey);
            if (error == null) {
                throw new AssertionError("No error for key: " + nodeKey);
            }
            errorHandler.accept(error);
        }

        public void verified() {
            if (!errors.isEmpty()) {
                throw new AssertionError("Unverified errors for nodes: " + errors.keySet());
            }
            if (!multiErrors.isEmpty()) {
                throw new AssertionError("Multiple error handler calls for: " + multiErrors);
            }
        }
    }

    private static final class TestTaskGraphExecutor implements TaskGraphExecutor {
        private final DependencyDag<TaskNodeKey<?, ?>> graph;
        private final List<TaskNode<?, ?>> nodes;
        private final Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodesMap;
        private final Set<TaskNodeKey<?, ?>> sameKeyNodes;

        public TestTaskGraphExecutor(DependencyDag<TaskNodeKey<?, ?>> graph, Iterable<? extends TaskNode<?, ?>> nodes) {
            this.graph = graph;
            this.nodes = new ArrayList<>();
            nodes.forEach(this.nodes::add);

            this.sameKeyNodes = new HashSet<>();
            this.nodesMap = CollectionsEx.newHashMap(this.nodes.size());
            this.nodes.forEach(node -> {
                if (nodesMap.put(node.getKey(), node) != null) {
                    sameKeyNodes.add(node.getKey());
                }
            });
        }

        public void verifyGraph(Consumer<TestGraphBuilder> expectationBuilder) {
            Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> expected = new HashMap<>();
            expectationBuilder.accept((TaskNodeKey<?, ?> parent, TaskNodeKey<?, ?>... children) -> {
                expected.put(parent, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(children))));
            });
            assertEquals(expected, graph.getDependencyGraph().getRawGraph());
        }

        public DependencyDag<TaskNodeKey<?, ?>> getGraph() {
            return graph;
        }

        public void expectedNodeCount(int expectedCount) {
            if (!sameKeyNodes.isEmpty()) {
                throw new AssertionError("Node(s) with the same key: " + sameKeyNodes);
            }
            assertEquals("nodes.size", expectedCount, nodes.size());
        }

        public TestOutput computeAndVerifyResult(Object factoryKey, Object nodeKey, Object... arguments) {
            TaskNode<?, ?> node = computeNode(factoryKey, nodeKey);
            return verifyResult(node, nodeKey, arguments);
        }

        public TaskNode<?, ?> computeNode(Object factoryKey, Object nodeKey) {
            TaskNode<?, ?> currentNode = node(factoryKey, nodeKey);
            if (currentNode.hasResult()) {
                throw new AssertionError("Already computed: " + nodeKey(factoryKey, nodeKey));
            }
            currentNode.ensureScheduleComputed(Cancellation.UNCANCELABLE_TOKEN, (errorKey, error) -> { });
            return currentNode;
        }

        public TaskNode<?, ?> node(Object factoryKey, Object nodeKey) {
            TaskNodeKey<Object, Object> key = nodeKey(factoryKey, nodeKey);
            TaskNode<?, ?> node = getNodes().get(key);
            if (node == null) {
                throw new AssertionError("Missing node: " + key);
            }
            return node;
        }

        public Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> getNodes() {
            return nodesMap;
        }

        @Override
        public TaskGraphExecutorProperties.Builder properties() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CompletionStage<TaskGraphExecutionResult> execute(CancellationToken cancelToken) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private interface TestGraphBuilder {
        public void setEdges(TaskNodeKey<?, ?> parent, TaskNodeKey<?, ?>... children);
    }
}
