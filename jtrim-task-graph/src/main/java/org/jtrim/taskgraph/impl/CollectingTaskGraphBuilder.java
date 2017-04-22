package org.jtrim.taskgraph.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.taskgraph.TaskFactory;
import org.jtrim.taskgraph.TaskFactoryConfig;
import org.jtrim.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim.taskgraph.TaskFactoryKey;
import org.jtrim.taskgraph.TaskFactoryProperties;
import org.jtrim.taskgraph.TaskFactorySetup;
import org.jtrim.taskgraph.TaskGraphBuilder;
import org.jtrim.taskgraph.TaskGraphBuilderProperties;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskInputBinder;
import org.jtrim.taskgraph.TaskInputRef;
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
        if (!configs.containsKey(nodeKey.getFactoryKey())) {
            throw new IllegalArgumentException("There is no factory to create this node: " + nodeKey);
        }

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

    private static final class TaskGraphBuilderImpl {
        private final Map<TaskFactoryKey<?, ?>, FactoryDef<?, ?>> factoryDefs;
        private final TaskGraphBuilderProperties properties;
        private final CancellationSource graphBuildCancel;

        private final Lock taskGraphLock;
        private final Map<TaskNodeKey<?, ?>, BuildableTaskNode<?, ?>> nodes;
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

            BuildableTaskNode<?, ?> newNode = new BuildableTaskNode<>(nodeKey);
            BuildableTaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            if (prev != null) {
                throw new IllegalStateException("Node was already added with key: " + nodeKey);
            }
            buildChildren(graphBuildCancel.getToken(), newNode);
        }

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
            I factoryArg = nodeKey.getFactoryArg();

            return factoryDef.createTaskNode(cancelToken, factoryArg, inputBinder);
        }

        public <R, I> BuildableTaskNode<R, I> addAndBuildNode(TaskNodeKey<R, I> nodeKey) {
            BuildableTaskNode<R, I> newNode = new BuildableTaskNode<>(nodeKey);
            BuildableTaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            if (prev != null) {
                @SuppressWarnings("unchecked")
                BuildableTaskNode<R, I> result = (BuildableTaskNode<R, I>)prev;
                assert result.getKey().equals(nodeKey);
                return result;
            }

            buildChildren(graphBuildCancel.getToken(), newNode);
            return newNode;
        }

        private void buildChildren(CancellationToken cancelToken, BuildableTaskNode<?, ?> newNode) {
            TaskNodeKey<?, ?> key = newNode.getKey();

            TaskFactoryProperties factoryProperties = getFactoryDef(key.getFactoryKey()).getProperties();
            TaskExecutor factoryExecutor = factoryProperties.getFactoryExecutor();

            incOutstandingBuilds();
            factoryExecutor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
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
            if (result == null) {
                throw new IllegalStateException("Missing node factory definition for key: " + key);
            }
            assert result.getDefKey().equals(key);
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
                // TODO: Do not save the callstack once OperationCanceledException allows us.
                graphBuildResult.completeExceptionally(new OperationCanceledException());
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
                TaskGraphExecutor executor = executorFactory.createExecutor(graph, getBuiltNodes());
                graphBuildResult.complete(executor);
            } catch (Throwable ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.log(Level.SEVERE, "Error while attempting to notify graph built handler.", ex);

                graphBuildResult.completeExceptionally(ex);
            }
        }

        private Iterable<TaskNode<?, ?>> getBuiltNodes() {
            List<TaskNode<?, ?>> result = new ArrayList<>(nodes.size());
            nodes.values().forEach((buildableNode) -> {
                result.add(buildableNode.getBuiltNode());
            });
            return result;
        }
    }

    private static final class FactoryDef<R, I> {
        private final TaskFactoryKey<R, I> defKey;
        private final LazyFactoryConfigurer groupConfigurer;
        private final TaskFactorySetup<R, I> setup;

        public FactoryDef(
                TaskFactoryKey<R, I> defKey,
                LazyFactoryConfigurer groupConfigurer,
                TaskFactorySetup<R, I> setup) {

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

    private static final class BuildableTaskNode<R, I> {
        private final TaskNodeKey<R, I> key;
        private final CompletableFuture<R> taskFuture;

        private TaskNode<R, I> builtNode;

        public BuildableTaskNode(TaskNodeKey<R, I> key) {
            this.key = key;
            this.taskFuture = new CompletableFuture<>();
        }

        public TaskNodeKey<R, I> getKey() {
            return key;
        }

        public CompletableFuture<R> getTaskFuture() {
            return taskFuture;
        }

        public Set<TaskNodeKey<?, ?>> buildChildren(
                CancellationToken cancelToken,
                TaskGraphBuilderImpl nodeBuilder) throws Exception {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(nodeBuilder, "nodeBuilder");

            TaskInputBinderImpl inputBinder = new TaskInputBinderImpl(cancelToken, nodeBuilder);
            NodeTaskRef<R> nodeTask = nodeBuilder.createNode(cancelToken, key, inputBinder);
            if (nodeTask == null) {
                throw new NullPointerException("TaskNodeBuilder.createNode returned null for key " + key);
            }

            builtNode = new TaskNode<>(key, nodeTask, taskFuture);
            return inputBinder.closeAndGetInputs();
        }

        public TaskNode<R, I> getBuiltNode() {
            assert builtNode != null;
            return builtNode;
        }
    }

    private static final class TaskInputBinderImpl implements TaskInputBinder {
        private final TaskGraphBuilderImpl nodeBuilder;
        private Set<TaskNodeKey<?, ?>> inputKeys;

        public TaskInputBinderImpl(CancellationToken cancelToken, TaskGraphBuilderImpl nodeBuilder) {
            this.nodeBuilder = nodeBuilder;
            this.inputKeys = new HashSet<>();
        }

        @Override
        public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey) {
            Set<TaskNodeKey<?, ?>> currentInputKeys = inputKeys;
            if (currentInputKeys == null) {
                throw new IllegalStateException("May only be called from the associated task node factory.");
            }

            BuildableTaskNode<I, A> child = nodeBuilder.addAndBuildNode(defKey);
            inputKeys.add(child.getKey());

            AtomicReference<CompletableFuture<I>> resultRef = new AtomicReference<>(child.getTaskFuture());
            return () -> {
                CompletableFuture<I> nodeFuture = resultRef.getAndSet(null);
                if (nodeFuture == null) {
                    throw new IllegalStateException("Input already consumed for key: " + defKey);
                }

                return TaskNode.getExpectedResultNow(defKey, nodeFuture);
            };
        }

        public Set<TaskNodeKey<?, ?>> closeAndGetInputs() {
            Set<TaskNodeKey<?, ?>> result = inputKeys;
            inputKeys = null;
            return result;
        }
    }
}
