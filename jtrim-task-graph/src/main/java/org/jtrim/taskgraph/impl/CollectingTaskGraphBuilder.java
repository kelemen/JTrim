package org.jtrim.taskgraph.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskFactory;
import org.jtrim.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim.taskgraph.TaskFactoryKey;
import org.jtrim.taskgraph.TaskFactoryProperties;
import org.jtrim.taskgraph.TaskFactorySetup;
import org.jtrim.taskgraph.TaskGraphBuilder;
import org.jtrim.taskgraph.TaskGraphBuilderProperties;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskInputBinder;
import org.jtrim.taskgraph.TaskNodeCreateArgs;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.taskgraph.TaskNodeProperties;
import org.jtrim.utils.ExceptionHelper;

public final class CollectingTaskGraphBuilder implements TaskGraphBuilder {
    private static final Logger LOGGER = Logger.getLogger(CollectingTaskGraphBuilder.class.getName());

    private final TaskGraphBuilderProperties.Builder properties;
    private final Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs;
    private final TaskGraphExecutorFactory executorFactory;

    private final Set<TaskNodeKey<?, ?>> nodeKeys;

    public CollectingTaskGraphBuilder(
            Collection<? extends TaskFactoryConfig<?, ?>> configs,
            TaskGraphExecutorFactory executorFactory) {
        ExceptionHelper.checkNotNullArgument(configs, "configs");
        ExceptionHelper.checkNotNullArgument(executorFactory, "executorFactory");

        this.properties = new TaskGraphBuilderProperties.Builder();
        this.executorFactory = executorFactory;
        this.nodeKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

        this.configs = CollectionsEx.newHashMap(configs.size());
        configs.forEach((config) -> {
            this.configs.put(config.getDefKey(), config);
        });

        ExceptionHelper.checkNotNullArgument(this.configs, "configs");
    }

    @Override
    public void addNode(TaskNodeKey<?, ?> nodeKey) {
        if (!nodeKeys.add(nodeKey)) {
            throw new IllegalStateException("Duplicate node key: " + nodeKey);
        }
    }

    @Override
    public TaskGraphBuilderProperties.Builder properties() {
        return properties;
    }

    @Override
    public CompletionStage<TaskGraphExecutor> buildGraph(CancellationToken cancelToken) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");

        TaskGraphBuilderImpl builder = new TaskGraphBuilderImpl(
                cancelToken, properties.build(), configs, executorFactory);
        return builder.build(nodeKeys);
    }

    private static boolean isError(boolean canceled, Throwable error) {
        if (canceled && (error instanceof OperationCanceledException)) {
            return false;
        }
        return error != null;
    }

    private static final class TaskGraphBuilderImpl implements TaskNodeBuilder {
        private final Map<TaskFactoryKey<?, ?>, FactoryDef<?, ?>> factoryDefs;
        private final TaskGraphBuilderProperties properties;
        private final CancellationSource graphBuildCancel;

        private final Lock taskGraphLock;
        private final Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;
        private final DirectedGraph.Builder<TaskNodeKey<?, ?>> taskGraphBuilder;

        private final AtomicInteger outstandingBuilds;
        private final CompletableFuture<TaskGraphExecutor> graphBuildResult;

        private final TaskGraphExecutorFactory executorFactory;

        public TaskGraphBuilderImpl(
                CancellationToken cancelToken,
                TaskGraphBuilderProperties properties,
                Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> factoryDefs,
                TaskGraphExecutorFactory executorFactory) {

            this.properties = properties;
            this.taskGraphLock = new ReentrantLock();
            this.nodes = new ConcurrentHashMap<>();
            this.taskGraphBuilder = new DirectedGraph.Builder<>();
            this.graphBuildCancel = Cancellation.createChildCancellationSource(cancelToken);
            this.outstandingBuilds = new AtomicInteger(0);
            this.graphBuildResult = new CompletableFuture<>();
            this.executorFactory = executorFactory;

            this.factoryDefs = CollectionsEx.newHashMap(factoryDefs.size());

            // We try to minimize the configuration if possible
            Map<TaskFactoryGroupConfigurer, LazyFactoryConfigurer> configurers = new IdentityHashMap<>();
            TaskFactoryProperties defaultFactoryProperties = properties.getDefaultFactoryProperties();
            factoryDefs.forEach((key, config) -> {
                LazyFactoryConfigurer lazyConfigurer;
                lazyConfigurer = configurers.computeIfAbsent(config.getConfigurer(), (groupConfigurer) -> {
                    return new LazyFactoryConfigurer(defaultFactoryProperties, groupConfigurer);
                });

                this.factoryDefs.put(key, factoryDef(lazyConfigurer, config));
            });
        }

        public CompletionStage<TaskGraphExecutor> build(Set<TaskNodeKey<?, ?>> nodeKeys) {
            try {
                incOutstandingBuilds();
                nodeKeys.forEach(this::addNode);
                decOutstandingBuilds();
            } catch (Throwable ex) {
                graphBuildCancel.getController().cancel();
                throw ex;
            }
            return graphBuildResult;
        }

        private static <R, I> FactoryDef<R, I> factoryDef(
                LazyFactoryConfigurer lazyConfigurer,
                TaskFactoryConfig<R, I> config) {
            return new FactoryDef<>(config.getDefKey(), lazyConfigurer, config.getSetup());
        }

        private void addNode(TaskNodeKey<?, ?> nodeKey) {
            ExceptionHelper.checkNotNullArgument(nodeKey, "nodeKey");

            TaskNode<?, ?> newNode = new TaskNode<>(nodeKey);
            TaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            if (prev != null) {
                throw new IllegalStateException("Node was already added with key: " + nodeKey);
            }
            buildChildren(graphBuildCancel.getToken(), newNode);
        }

        @Override
        public <R> NodeTaskRef<R> createNode(
                CancellationToken cancelToken,
                TaskNodeKey<R, ?> nodeKey,
                TaskInputBinder inputBinder) throws Exception {
            return createNodeBridge(cancelToken, nodeKey, inputBinder);
        }

        private <R, I> NodeTaskRef<R> createNodeBridge(
                CancellationToken cancelToken,
                TaskNodeKey<R, I> nodeKey,
                TaskInputBinder inputBinder) throws Exception {
            TaskFactoryKey<R, I> factoryKey = nodeKey.getFactoryKey();
            FactoryDef<R, I> factoryDef = getFactoryDef(factoryKey);
            if (factoryDef == null) {
                throw new IllegalStateException("Missing node factory definition for key: " + factoryKey);
            }

            I factoryArg = nodeKey.getFactoryArg();

            return factoryDef.createTaskNode(cancelToken, factoryArg, inputBinder);
        }

        @Override
        public <R, I> TaskNode<R, I> addAndBuildNode(TaskNodeKey<R, I> nodeKey) {
            TaskNode<R, I> newNode = new TaskNode<>(nodeKey);
            TaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            if (prev != null) {
                @SuppressWarnings("unchecked")
                        TaskNode<R, I> result = (TaskNode<R, I>)prev;
                assert result.getKey().equals(nodeKey);
                return result;
            }

            buildChildren(graphBuildCancel.getToken(), newNode);
            return newNode;
        }

        private void buildChildren(CancellationToken cancelToken, TaskNode<?, ?> newNode) {
            TaskNodeKey<?, ?> key = newNode.getKey();

            incOutstandingBuilds();
            properties.getGraphBuilderExecutor().execute(cancelToken, (CancellationToken taskCancelToken) -> {
                Set<TaskNodeKey<?, ?>> childrenKeys = newNode.buildChildren(taskCancelToken, this);
                taskGraphLock.lock();
                try {
                    taskGraphBuilder.addNodeWithChildren(key, childrenKeys);
                } finally {
                    taskGraphLock.unlock();
                }
            }, (boolean canceled, Throwable error) -> {
                // We intentionally do not decrease the oustandingBuilds to prevent
                // success notification.
                if (canceled) {
                    onCancel();
                    return;
                }

                if (isError(canceled, error)) {
                    onError(key, error);
                    return;
                }

                decOutstandingBuilds();
            });
        }

        private void incOutstandingBuilds() {
            outstandingBuilds.incrementAndGet();
        }

        private void decOutstandingBuilds() {
            int outstandingBuildCount = outstandingBuilds.decrementAndGet();
            if (outstandingBuildCount == 0) {
                onSuccess();
            }
        }

        private <R, I> FactoryDef<R, I> getFactoryDef(TaskFactoryKey<R, I> key) {
            @SuppressWarnings("unchecked")
            FactoryDef<R, I> result = (FactoryDef<R, I>)factoryDefs.get(key);
            assert result == null || result.getDefKey().equals(key);
            return result;
        }

        private void onError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            try {
                try {
                    graphBuildCancel.getController().cancel();
                    properties.getNodeCreateErrorHandler().onError(nodeKey, error);
                } finally {
                    graphBuildResult.completeExceptionally(error);
                }
            } catch (Throwable subError) {
                if (subError instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                subError.addSuppressed(error);
                LOGGER.log(Level.SEVERE, "Error while handling error of a task node: " + nodeKey, subError);
            }
        }

        private void onCancel() {
            try {
                graphBuildResult.complete(null);
            } catch (Throwable ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.log(Level.SEVERE, "Error while handling cancellation.", ex);
            }
        }

        private void onSuccess() {
            try {
                // No synchronization is necessary because we already know that we have built the graph,
                // so no more node will be added.
                DependencyDag<TaskNodeKey<?, ?>> graph = new DependencyDag<>(taskGraphBuilder.build());
                TaskGraphExecutor executor = executorFactory.createExecutor(graph, nodes);
                graphBuildResult.complete(executor);
            } catch (Throwable ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.log(Level.SEVERE, "Error while attempting to notify graph built handler.", ex);

                graphBuildResult.completeExceptionally(ex);
            }
        }
    }

    private static final class FactoryDef<R, I> {
        private final TaskFactoryKey<R, I> defKey;
        private final LazyFactoryConfigurer groupConfigurer;
        private final TaskFactorySetup<R, I> setup;

        public FactoryDef(TaskFactoryKey<R, I> defKey, LazyFactoryConfigurer groupConfigurer, TaskFactorySetup<R, I> setup) {
            this.defKey = defKey;
            this.groupConfigurer = groupConfigurer;
            this.setup = setup;
        }

        public NodeTaskRef<R> createTaskNode(
                CancellationToken cancelToken,
                I factoryArg,
                TaskInputBinder inputs) throws Exception {

            TaskNodeProperties defaults = getProperties().getDefaultNodeProperties();
            TaskNodeCreateArgs<I> createArgs = new TaskNodeCreateArgs<>(factoryArg, defaults, inputs);

            CancelableFunction<R> nodeTask = createFactory().createTaskNode(cancelToken, createArgs);
            return new NodeTaskRef<>(createArgs.properties().build(), nodeTask);
        }

        public TaskFactory<R, I> createFactory() throws Exception {
            return setup.setup(getProperties());
        }

        public TaskFactoryKey<R, I> getDefKey() {
            return defKey;
        }

        public TaskFactoryProperties getProperties() {
            return groupConfigurer.getProperties();
        }
    }

    private static final class LazyFactoryConfigurer {
        private final TaskFactoryProperties defaults;
        private final TaskFactoryGroupConfigurer groupConfigurer;
        private final AtomicReference<TaskFactoryProperties> propertiesRef;

        public LazyFactoryConfigurer(TaskFactoryProperties defaults, TaskFactoryGroupConfigurer groupConfigurer) {
            this.defaults = defaults;
            this.groupConfigurer = groupConfigurer;
            this.propertiesRef = new AtomicReference<>(null);
        }

        public TaskFactoryProperties getProperties() {
            TaskFactoryProperties result = propertiesRef.get();
            if (result == null) {
                TaskFactoryProperties.Builder resultBuilder = new TaskFactoryProperties.Builder(defaults);
                groupConfigurer.setup(resultBuilder);
                result = resultBuilder.build();
                if (!propertiesRef.compareAndSet(null, result)) {
                    result = propertiesRef.get();
                }
            }
            return result;
        }
    }
}
