package org.jtrim2.taskgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.testutils.TestObj;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.UnsafeConsumer;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class AbstractGraphExecutorTest {
    private final Supplier<TaskGraphDefConfigurer> graphConfigurerFactory;

    public AbstractGraphExecutorTest(Supplier<TaskGraphDefConfigurer> graphConfigurerFactory) {
        this.graphConfigurerFactory = Objects.requireNonNull(graphConfigurerFactory, "graphConfigurerFactory");
    }

    private void test(UnsafeConsumer<TaskGraphDefConfigurer> graphConfigurerAction) throws Exception {
        graphConfigurerAction.accept(graphConfigurerFactory.get());
    }

    private static <R, I> TaskNodeKey<R, I> nodeKey(Class<R> outputType, Class<I> argType, I arg) {
        return new TaskNodeKey<>(new TaskFactoryKey<>(outputType, argType), arg);
    }

    /**
     * This tests verifies that no deeply nested calls occur when the whole execution gets canceled.
     *
     * @throws Exception test failure
     */
    @Test(timeout = 60000)
    public void testFailureWithLongChainCancel() throws Exception {
        int rootCount = 10000;

        test((configurer) -> {
            TaskFactoryDefiner factoryGroup1 = configurer.factoryGroupDefiner((properties) -> {
            });

            List<TaskFactoryKey<TestObj, String>> rootKeys = new ArrayList<>(rootCount);
            for (int i = 0; i < rootCount; i++) {
                int index = i;

                TaskFactoryKey<TestObj, String> leafFactoryKey
                        = new TaskFactoryKey<>(TestObj.class, String.class, "leaf." + i);
                factoryGroup1.defineSimpleFactory(leafFactoryKey, (cancelToken, nodeDef) -> {
                    String factoryArg = nodeDef.factoryArg();
                    return taskCancelToken -> {
                        throw new TestException(factoryArg);
                    };
                });

                TaskFactoryKey<TestObj, String> rootFactoryKey
                        = new TaskFactoryKey<>(TestObj.class, String.class, "root." + i);
                factoryGroup1.defineSimpleFactory(rootFactoryKey, (cancelToken, nodeDef) -> {
                    TaskInputRef<TestObj> inputRef = nodeDef.inputs().bindInput(leafFactoryKey, "arg." + index);
                    return taskCancelToken -> inputRef.consumeInput();
                });

                rootKeys.add(rootFactoryKey);
            }

            TaskGraphBuilder builder = configurer.build();

            List<TaskNodeKey<TestObj, String>> requestedNodeKeys = new ArrayList<>();
            rootKeys.forEach(rootKey -> {
                TaskNodeKey<TestObj, String> requestedNodeKey = new TaskNodeKey<>(rootKey, "R");
                requestedNodeKeys.add(requestedNodeKey);

                builder.addNode(requestedNodeKey);
            });

            CompletionStage<TaskGraphExecutor> buildFuture = builder.buildGraph(Cancellation.UNCANCELABLE_TOKEN);
            List<CompletionStage<TestObj>> rootFutures = new ArrayList<>();
            buildFuture
                    .thenAccept((executor) -> {
                        executor.properties().setComputeErrorHandler((nodeKey, error) -> {
                            // Redefine to prevent absurd amount of logs
                        });
                        executor.properties().setStopOnFailure(true);
                        executor.properties().setDeliverResultOnFailure(false);
                        requestedNodeKeys.forEach(nodeKey -> {
                            rootFutures.add(executor.futureOf(nodeKey));
                        });
                        executor.execute(Cancellation.UNCANCELABLE_TOKEN);
                    });

            assertFalse("Test setup error", rootFutures.isEmpty());
            rootFutures.forEach(future -> {
                AtomicReference<Throwable> errorRef = new AtomicReference<>();
                future.whenComplete((result, error) -> errorRef.set(error));

                Throwable error = AsyncTasks.unwrap(errorRef.get());
                assertNotNull("error", error);
                if (!(error instanceof TestException) && !(error instanceof OperationCanceledException)) {
                    throw new AssertionError("Unexpected exception type: " + error.getClass().getName(), error);
                }
            });
        });
    }

    @Test
    public void testSingleNodeFails() throws Exception {
        test((configurer) -> {
            TaskFactoryDefiner factoryGroup1 = configurer.factoryGroupDefiner((properties) -> {
            });

            TaskFactoryKey<TestObj, String> factoryKey
                    = new TaskFactoryKey<>(TestObj.class, String.class, "test-node");
            factoryGroup1.defineSimpleFactory(factoryKey, (cancelToken, nodeDef) -> {
                String factoryArg = nodeDef.factoryArg();
                return taskCancelToken -> {
                    throw new TestException(factoryArg);
                };
            });

            TaskGraphBuilder builder = configurer.build();

            TaskNodeKey<TestObj, String> requestedNodeKey = new TaskNodeKey<>(factoryKey, "R");
            builder.addNode(requestedNodeKey);

            CompletionStage<TaskGraphExecutor> buildFuture = builder.buildGraph(Cancellation.UNCANCELABLE_TOKEN);
            AtomicReference<TaskGraphExecutionResult> resultRef = new AtomicReference<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            buildFuture
                    .thenCompose((executor) -> {
                        executor.properties().setComputeErrorHandler((nodeKey, error) -> {
                            // Redefine to prevent logs
                        });
                        executor.properties().setStopOnFailure(true);
                        executor.properties().setDeliverResultOnFailure(true);

                        executor.properties().addResultNodeKey(requestedNodeKey);

                        return executor.execute(Cancellation.UNCANCELABLE_TOKEN);
                    })
                    .whenComplete((result, error) -> {
                        resultRef.set(result);
                        errorRef.set(error);
                    });

            TaskGraphExecutionResult result = resultRef.get();
            Throwable error = errorRef.get();

            assertNotNull("result", result);
            assertNull("error", error);

            assertEquals("resultType", ExecutionResultType.ERRORED, result.getResultType());

            TestUtils.expectUnwrappedError(TestException.class, () -> result.getResult(requestedNodeKey));
        });
    }

    @Test
    public void testDependencyErrorHandler() throws Exception {
        test((configurer) -> {
            TaskFactoryDefiner factoryGroup1 = configurer.factoryGroupDefiner((properties) -> {
            });

            TaskFactoryKey<TestObj, String> leafFactoryKey
                    = new TaskFactoryKey<>(TestObj.class, String.class, "leaf-node");
            factoryGroup1.defineSimpleFactory(leafFactoryKey, (cancelToken, nodeDef) -> {
                String factoryArg = nodeDef.factoryArg();
                return taskCancelToken -> {
                    throw new TestException(factoryArg);
                };
            });

            DependencyErrorHandler errorHandler = mock(DependencyErrorHandler.class);

            TaskFactoryKey<TestObj, String> rootFactoryKey
                    = new TaskFactoryKey<>(TestObj.class, String.class, "root-node");
            factoryGroup1.defineSimpleFactory(rootFactoryKey, (cancelToken, nodeDef) -> {
                nodeDef.properties().setDependencyErrorHandler(errorHandler);

                TaskInputRef<TestObj> inputRef = nodeDef.inputs().bindInput(leafFactoryKey, "test-arg");
                return taskCancelToken -> inputRef.consumeInput();
            });


            TaskGraphBuilder builder = configurer.build();

            TaskNodeKey<TestObj, String> requestedNodeKey = new TaskNodeKey<>(rootFactoryKey, "R");
            builder.addNode(requestedNodeKey);

            CompletionStage<TaskGraphExecutor> buildFuture = builder.buildGraph(Cancellation.UNCANCELABLE_TOKEN);
            AtomicReference<TaskGraphExecutionResult> resultRef = new AtomicReference<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            buildFuture
                    .thenCompose((executor) -> {
                        executor.properties().setComputeErrorHandler((nodeKey, error) -> {
                            // Redefine to prevent logs
                        });
                        executor.properties().setStopOnFailure(false);
                        executor.properties().setDeliverResultOnFailure(true);

                        executor.properties().addResultNodeKey(requestedNodeKey);

                        return executor.execute(Cancellation.UNCANCELABLE_TOKEN);
                    })
                    .whenComplete((result, error) -> {
                        resultRef.set(result);
                        errorRef.set(error);
                    });

            verify(errorHandler).handleDependencyError(
                    any(CancellationToken.class),
                    eq(requestedNodeKey),
                    isA(TestException.class));

            TaskGraphExecutionResult result = resultRef.get();
            Throwable error = errorRef.get();

            assertNotNull("result", result);
            assertNull("error", error);

            assertEquals("resultType", ExecutionResultType.ERRORED, result.getResultType());

            TestUtils.expectUnwrappedError(TestException.class, () -> result.getResult(requestedNodeKey));
        });
    }

    @Test
    public void testDoubleSplitGraph() throws Exception {
        test((configurer) -> {
            TaskFactoryDefiner factoryGroup1 = configurer.factoryGroupDefiner((properties) -> {
            });
            factoryGroup1.defineSimpleFactory(TestOutput1.class, String.class, (cancelToken, nodeDef) -> {
                return (taskCancelToken) -> new TestOutput1(nodeDef.factoryArg());
            });

            factoryGroup1.defineSimpleFactory(TestOutput2.class, String.class, (cancelToken, nodeDef) -> {
                String factoryArg = nodeDef.factoryArg();

                TaskInputRef<TestOutput1> inputRef1 = nodeDef.inputs()
                        .bindInput(TestOutput1.class, String.class, factoryArg + ".a");
                TaskInputRef<TestOutput1> inputRef2 = nodeDef.inputs()
                        .bindInput(TestOutput1.class, String.class, factoryArg + ".b");

                return (taskCancelToken) -> {
                    return new TestOutput2(nodeDef.factoryArg(), inputRef1.consumeInput(), inputRef2.consumeInput());
                };
            });

            TaskNodeKey<TestOutput2, String> nodeKey1 = nodeKey(TestOutput2.class, String.class, "x");
            TaskNodeKey<TestOutput2, String> nodeKey2 = nodeKey(TestOutput2.class, String.class, "y");

            TaskGraphBuilder graphBuilder = configurer.build();
            graphBuilder.addNode(nodeKey1);
            graphBuilder.addNode(nodeKey2);

            AtomicReference<TaskGraphExecutionResult> resultRef = new AtomicReference<>();
            CompletionStage<TaskGraphExecutor> buildFuture = graphBuilder.buildGraph(Cancellation.UNCANCELABLE_TOKEN);
            buildFuture
                    .thenCompose((executor) -> {
                        executor.properties().addResultNodeKey(nodeKey1);
                        executor.properties().addResultNodeKey(nodeKey2);
                        return executor.execute(Cancellation.UNCANCELABLE_TOKEN);
                    })
                    .thenAccept(resultRef::set);

            TaskGraphExecutionResult result = resultRef.get();
            assertNotNull(result);

            TestOutput2 node1 = result.getResult(nodeKey1);
            assertEquals(new TestOutput2("x", new TestOutput1("x.a"), new TestOutput1("x.b")), node1);

            TestOutput2 node2 = result.getResult(nodeKey2);
            assertEquals(new TestOutput2("y", new TestOutput1("y.a"), new TestOutput1("y.b")), node2);
        });
    }

    private abstract static class AbstractTestPojo {
        private final Object[] content;

        public AbstractTestPojo(Object... content) {
            this.content = content.clone();
        }

        @Override
        public final int hashCode() {
            int hash = 5;
            hash = 97 * hash + Arrays.hashCode(content);
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final AbstractTestPojo other = (AbstractTestPojo) obj;
            return Arrays.equals(this.content, other.content);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + Arrays.toString(content);
        }
    }

    private static final class TestOutput1 extends AbstractTestPojo {
        public TestOutput1(Object... content) {
            super(content);
        }
    }

    private static final class TestOutput2 extends AbstractTestPojo {
        public TestOutput2(Object... content) {
            super(content);
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException(String message) {
            super(message);
        }
    }
}
