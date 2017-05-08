package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.taskgraph.TaskFactory;
import org.jtrim2.taskgraph.TaskFactoryConfig;
import org.jtrim2.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim2.taskgraph.TaskFactoryKey;
import org.jtrim2.taskgraph.TaskFactoryProperties;
import org.jtrim2.taskgraph.TaskFactorySetup;
import org.jtrim2.taskgraph.TaskGraphBuilder;
import org.jtrim2.taskgraph.TaskGraphBuilderProperties;
import org.jtrim2.taskgraph.TaskGraphExecutor;
import org.jtrim2.taskgraph.TaskInputBinder;
import org.jtrim2.taskgraph.TaskInputRef;
import org.jtrim2.taskgraph.TaskNodeCreateArgs;
import org.jtrim2.taskgraph.TaskNodeKey;
import org.jtrim2.taskgraph.TaskNodeProperties;

/**
 * Defines a simple implementation of {@code TaskGraphBuilder} which collects the
 * added task node keys and simply passes them to a given {@link TaskGraphExecutorFactory}.
 * Once graph building is requested, the graph is built with its task nodes already aware of
 * their input. That is, the nodes of the graph will be translated into {@link TaskNode} instances.
 *
 * <h3>Thread safety</h3>
 * The methods of this class may not be used by multiple threads concurrently, unless otherwise noted.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I> in general.
 *
 * @see CollectingTaskGraphDefConfigurer
 * @see RestrictableTaskGraphExecutor
 */
public final class CollectingTaskGraphBuilder implements TaskGraphBuilder {
    private static final Logger LOGGER = Logger.getLogger(CollectingTaskGraphBuilder.class.getName());

    private final TaskGraphBuilderProperties.Builder properties;
    private final Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs;
    private final TaskGraphExecutorFactory executorFactory;

    private final Set<TaskNodeKey<?, ?>> nodeKeys;

    /**
     * Creates a new {@code CollectingTaskGraphBuilder} with the given task factory definitions
     * and {@code TaskGraphExecutorFactory}.
     *
     * @param configs the task factory definitions used to create the task nodes.
     *   This argument cannot be {@code null} and cannot contain {@code null} elements.
     * @param executorFactory the {@code TaskGraphExecutorFactory} used to create
     *   the {@code TaskGraphExecutor} actually executing the task graph. This argument cannot
     *   be {@code null}.
     */
    public CollectingTaskGraphBuilder(
            Collection<? extends TaskFactoryConfig<?, ?>> configs,
            TaskGraphExecutorFactory executorFactory) {
        Objects.requireNonNull(configs, "configs");
        Objects.requireNonNull(executorFactory, "executorFactory");

        this.properties = new TaskGraphBuilderProperties.Builder();
        this.executorFactory = executorFactory;
        this.nodeKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

        this.configs = CollectionsEx.newHashMap(configs.size());
        configs.forEach((config) -> {
            this.configs.put(config.getDefKey(), config);
        });

        Objects.requireNonNull(this.configs, "configs");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addNode(TaskNodeKey<?, ?> nodeKey) {
        if (!configs.containsKey(nodeKey.getFactoryKey())) {
            throw new IllegalArgumentException("There is no factory to create this node: " + nodeKey);
        }

        if (!nodeKeys.add(nodeKey)) {
            throw new IllegalStateException("Duplicate node key: " + nodeKey);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskGraphBuilderProperties.Builder properties() {
        return properties;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CompletionStage<TaskGraphExecutor> buildGraph(CancellationToken cancelToken) {
        Objects.requireNonNull(cancelToken, "cancelToken");

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
            incOutstandingBuilds();
            nodeKeys.forEach(this::addNode);
            decOutstandingBuilds();
            return graphBuildResult;
        }

        private static <R, I> FactoryDef<R, I> factoryDef(
                LazyFactoryConfigurer lazyConfigurer,
                TaskFactoryConfig<R, I> config) {
            return new FactoryDef<>(config.getDefKey(), lazyConfigurer, config.getSetup());
        }

        private void addNode(TaskNodeKey<?, ?> nodeKey) {
            Objects.requireNonNull(nodeKey, "nodeKey");

            BuildableTaskNode<?, ?> newNode = new BuildableTaskNode<>(nodeKey);
            BuildableTaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            assert prev == null;

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
                if (isError(canceled, error)) {
                    onError(key, error);
                    return;
                }

                if (canceled) {
                    onCancel();
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
                subError.addSuppressed(error);
                LOGGER.log(Level.SEVERE, "Error while handling error of a task node: " + nodeKey, subError);
            }
        }

        private void onCancel() {
            graphBuildResult.completeExceptionally(new OperationCanceledException());
        }

        private void onSuccess() {
            try {
                // No synchronization is necessary because we already know that we have built the graph,
                // so no more node will be added.
                DependencyDag<TaskNodeKey<?, ?>> graph = new DependencyDag<>(taskGraphBuilder.build());
                TaskGraphExecutor executor = executorFactory.createExecutor(graph, getBuiltNodes());
                graphBuildResult.complete(executor);
            } catch (Throwable ex) {
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
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(nodeBuilder, "nodeBuilder");

            TaskInputBinderImpl inputBinder = new TaskInputBinderImpl(cancelToken, nodeBuilder);
            NodeTaskRef<R> nodeTask = nodeBuilder.createNode(cancelToken, key, inputBinder);
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
