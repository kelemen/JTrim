package org.jtrim.taskgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.event.CountDownEvent;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.OneShotListenerManager;
import org.jtrim.utils.ExceptionHelper;

public final class DefaultTaskGraphDefConfigurer implements TaskGraphDefConfigurer {
    private static final Logger LOGGER = Logger.getLogger(DefaultTaskGraphDefConfigurer.class.getName());

    private final TaskGraphExecProperties.Builder properties;
    private final ConcurrentMap<TaskFactoryKey<?, ?>, FactoryDef<?, ?>> factoryDefs;

    public DefaultTaskGraphDefConfigurer() {
        this.properties = new TaskGraphExecProperties.Builder();
        this.factoryDefs = new ConcurrentHashMap<>();
    }

    @Override
    public TaskGraphExecProperties.Builder properties() {
        return properties;
    }

    @Override
    public TaskFactoryDefiner factoryGroupDefiner(TaskFactoryGroupConfigurer groupConfigurer) {
        return new TaskFactoryDefinerImpl(groupConfigurer);
    }

    @Override
    public TaskGraphBuilder build() {
        return new TaskGraphBuilderImpl(this);
    }

    private static boolean isError(boolean canceled, Throwable error) {
        if (canceled && (error instanceof OperationCanceledException)) {
            return false;
        }
        return error != null;
    }

    private static Consumer<? super Throwable> safeErrorHandler(Consumer<? super Throwable> unsafeHandler) {
        return (Throwable error) -> {
            try {
                unsafeHandler.accept(error);
            } catch (Throwable subError) {
                if (subError instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                subError.addSuppressed(error);
                LOGGER.log(Level.SEVERE, "Error while handling generic error.", subError);
            }
        };
    }

    private static final class TaskGraphBuilderImpl implements TaskGraphBuilder {
        private final Map<TaskFactoryKey<?, ?>, FactoryDef<?, ?>> factoryDefs;
        private final TaskGraphExecProperties properties;
        private final ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;

        private final AtomicBoolean executed;
        private final AtomicInteger outstandingBuildEvents;
        private final OneShotListenerManager<Runnable, Void> graphBuiltEvents;

        private final CancellationSource graphBuildCancel;

        private volatile boolean errored;

        public TaskGraphBuilderImpl(DefaultTaskGraphDefConfigurer parent) {
            this.factoryDefs = new HashMap<>(parent.factoryDefs);
            this.properties = parent.properties.build();
            this.nodes = new ConcurrentHashMap<>();
            this.graphBuildCancel = Cancellation.createCancellationSource();
            this.graphBuiltEvents = new OneShotListenerManager<>();
            this.outstandingBuildEvents = new AtomicInteger(0);
            this.executed = new AtomicBoolean(false);
            this.errored = false;

            TaskFactoryProperties defaultFactoryProperties = this.properties.getDefaultFactoryProperties();
            this.factoryDefs.values().forEach((def) -> def.setDefaults(defaultFactoryProperties));
        }

        private void checkNotExecuted() {
            if (executed.get()) {
                throw new IllegalStateException("Already scheduled execution of task graph.");
            }
        }

        @Override
        public void addNode(TaskNodeKey<?, ?> nodeKey) {
            ExceptionHelper.checkNotNullArgument(nodeKey, "nodeKey");
            checkNotExecuted();

            TaskNode<?, ?> newNode = new TaskNode<>(nodeKey);
            TaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            if (prev != null) {
                throw new IllegalStateException("Node was already added with key: " + nodeKey);
            }
            buildChildren(graphBuildCancel.getToken(), newNode);

            // Just in case of an even worse abuse: Concurrent execute call.
            checkNotExecuted();
        }

        private <R, I> TaskNode<R, I> addAndBuildNode(CancellationToken cancelToken, TaskNodeKey<R, I> nodeKey) {
            TaskNode<R, I> newNode = new TaskNode<>(nodeKey);
            TaskNode<?, ?> prev = nodes.putIfAbsent(nodeKey, newNode);
            if (prev != null) {
                @SuppressWarnings("unchecked")
                TaskNode<R, I> result = (TaskNode<R, I>)prev;
                assert result.key.equals(nodeKey);
                return result;
            }

            buildChildren(cancelToken, newNode);
            return newNode;
        }

        private void buildChildren(CancellationToken cancelToken, TaskNode<?, ?> newNode) {
            TaskNodeKey<?, ?> key = newNode.key;

            outstandingBuildEvents.incrementAndGet();
            properties.getGraphBuilderExecutor().execute(cancelToken, (CancellationToken taskCancelToken) -> {
                // We intentionally do not use taskCancelToken because that would be a needless chaining
                // of cancellation tokens.
                newNode.buildChildren(cancelToken, this);
            }, (boolean canceled, Throwable error) -> {
                outstandingBuildEvents.decrementAndGet();
                if (isError(canceled, error)) {
                    onError(key, error);
                }

                notifyIfBuilt();
            });
        }

        private void onError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            try {
                errored = true;
                properties.getNodeCreateErrorHandler().onError(nodeKey, error);
            } catch (Throwable subError) {
                if (subError instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                subError.addSuppressed(error);
                LOGGER.log(Level.SEVERE, "Error while creating task node: " + nodeKey, subError);
            }
        }

        private void notifyIfBuilt() {
            if (executed.get() && outstandingBuildEvents.get() == 0) {
                EventListeners.dispatchRunnable(graphBuiltEvents);
            }
        }

        private <R, I> FactoryDef<R, I> getFactoryDef(TaskFactoryKey<R, I> key) {
            @SuppressWarnings("unchecked")
            FactoryDef<R, I> result = (FactoryDef<R, I>)factoryDefs.get(key);
            assert result == null || result.getDefKey().equals(key);
            return result;
        }

        @Override
        public void execute(CancellationToken cancelToken, Consumer<? super TaskGraphExecutionResult> onTerimateAction) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(onTerimateAction, "onTerimateAction");

            if (!executed.compareAndSet(false, true)) {
                throw new IllegalStateException("Already scheduled execution of task graph.");
            }

            notifyIfBuilt();

            ListenerRef cancelRef = cancelToken.addCancellationListener(graphBuildCancel.getController()::cancel);

            graphBuiltEvents.registerOrNotifyListener(() -> {
                try {
                    executeGraph(cancelToken, makeIdempotent(onTerimateAction));
                } finally {
                    cancelRef.unregister();
                }
            });
        }

        private static <T> Consumer<T> makeIdempotent(Consumer<? super T> wrapped) {
            AtomicReference<Consumer<? super T>> wrappedRef = new AtomicReference<>(wrapped);
            return (arg) -> {
                Consumer<? super T> currentWrapped = wrappedRef.getAndSet(null);
                if (currentWrapped != null) {
                    currentWrapped.accept(arg);
                }
            };
        }

        private void executeGraph(
                CancellationToken cancelToken,
                Consumer<? super TaskGraphExecutionResult> onTerimateAction) {
            try {
                executeGraphUnsafe(cancelToken, onTerimateAction);
            } catch (Throwable ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                safeErrorHandler(properties.getOtherErrorHandler()).accept(ex);
                TaskGraphExecutionResult.Builder result = new TaskGraphExecutionResult.Builder();
                result.setGraphBuiltSuccessfully(true);
                result.setSuccessful(false);
                onTerimateAction.accept(result.build());
            }
        }

        private void executeGraphUnsafe(
                CancellationToken cancelToken,
                Consumer<? super TaskGraphExecutionResult> onTerimateAction) {

            if (errored) {
                TaskGraphExecutionResult.Builder result = new TaskGraphExecutionResult.Builder();
                result.setGraphBuiltSuccessfully(false);
                result.setSuccessful(false);
                onTerimateAction.accept(result.build());
                return;
            }

            GraphExecutor graphExecutor = new GraphExecutor(properties, nodes, onTerimateAction);
            graphExecutor.execute(cancelToken);
        }
    }

    private static final class GraphExecutor {
        private final TaskGraphExecProperties properties;
        private final Consumer<? super TaskGraphExecutionResult> onTerimateAction;
        private final ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;

        private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> dependencyGraph;
        private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> forwardGraph;

        private volatile boolean errored;

        private CancellationSource cancel;

        public GraphExecutor(
                TaskGraphExecProperties properties,
                Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes,
                Consumer<? super TaskGraphExecutionResult> onTerimateAction) {
            this.properties = properties;
            this.onTerimateAction = onTerimateAction;
            this.nodes = new ConcurrentHashMap<>(nodes);

            this.errored = false;

            this.dependencyGraph = new HashMap<>();
            this.forwardGraph = new HashMap<>();

            this.nodes.forEach((key, node) -> {
                node.getInputKeys().forEach((dependency) -> {
                    Collection<TaskNodeKey<?, ?>> dependencies
                            = dependencyGraph.computeIfAbsent(key, (x) -> new HashSet<>());
                    dependencies.add(dependency);

                    Collection<TaskNodeKey<?, ?>> parents
                            = forwardGraph.computeIfAbsent(dependency, (x) -> new HashSet<>());
                    parents.add(key);
                });
            });
        }

        private void checkNotCircular() {
            Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> graph = new HashMap<>(dependencyGraph);
            while (!graph.isEmpty()) {
                TaskNodeKey<?, ?> key = graph.keySet().iterator().next();
                checkNotCircular(Collections.singleton(key), new LinkedHashSet<>(), graph);
            }
        }

        private static <T> List<T> afterFirstMatch(T match, List<T> list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (match.equals(list.get(i))) {
                    return list.subList(i, size);
                }
            }
            return Collections.emptyList();
        }

        private static void checkNotCircular(
                Set<TaskNodeKey<?, ?>> startNodes,
                Set<TaskNodeKey<?, ?>> visited,
                Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> graph) {

            startNodes.forEach((key) -> {
                if (visited.contains(key)) {
                    List<TaskNodeKey<?, ?>> cycle = new ArrayList<>(visited.size() + 1);
                    cycle.addAll(visited);
                    cycle.add(key);

                    cycle = afterFirstMatch(key, cycle);

                    throw new IllegalStateException("The graph is cyclic: " + cycle);
                }

                Set<TaskNodeKey<?, ?>> dependencies = graph.get(key);
                if (dependencies != null) {
                    visited.add(key);
                    checkNotCircular(dependencies, visited, graph);
                    visited.remove(key);
                }
                graph.remove(key);
            });
        }

        private Collection<TaskNodeKey<?, ?>> getEndNodes() {
            Collection<TaskNodeKey<?, ?>> result = new ArrayList<>();

            nodes.keySet().forEach((key) -> {
                if (!forwardGraph.containsKey(key)) {
                    result.add(key);
                }
            });

            return result;
        }

        public void execute(CancellationToken cancelToken) {
            checkNotCircular();

            this.cancel = Cancellation.createChildCancellationSource(cancelToken);

            List<TaskNode<?, ?>> allNodes = new ArrayList<>(nodes.values());
            if (allNodes.isEmpty()) {
                finish();
                return;
            }

            CountDownEvent completeEvent = new CountDownEvent(allNodes.size(), this::finish);
            allNodes.forEach((node) -> {
                node.addOnFinished(() -> {
                    removeNode(node.key);
                    completeEvent.dec();
                });
            });

            getEndNodes().forEach(this::scheduleNode);
        }

        private void finish() {
            TaskGraphExecutionResult.Builder result = new TaskGraphExecutionResult.Builder();
            result.setGraphBuiltSuccessfully(true);
            result.setSuccessful(!errored);
            onTerimateAction.accept(result.build());
        }

        private void onError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            try {
                errored = true;
                if (properties.isStopOnFailure()) {
                    cancel.getController().cancel();
                }

                properties.getComputeErrorHandler().onError(nodeKey, error);
            } catch (Throwable subError) {
                if (subError instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                subError.addSuppressed(error);
                LOGGER.log(Level.SEVERE, "Error while computing node: " + nodeKey, subError);
            }
        }

        private void ensureScheduled(TaskNode<?, ?> node) {
            node.ensureScheduleComputed(getCancelToken(), this::onError);
        }

        private void scheduleNode(TaskNodeKey<?, ?> nodeKey) {
            TaskNode<?, ?> node = nodes.get(nodeKey);
            if (node != null) {
                scheduleNode(node);
            }
        }

        private void scheduleNode(TaskNode<?, ?> node) {
            Collection<TaskNode<?, ?>> dependencies = getDependencies(node.key);
            if (dependencies.isEmpty()) {
                ensureScheduled(node);
                return;
            }

            CountDownEvent doneEvent = new CountDownEvent(dependencies.size(), () -> {
                ensureScheduled(node);
            });
            dependencies.forEach((dependency) -> {
                dependency.addOnComputed(doneEvent::dec);
                scheduleNode(dependency);
            });
        }

        private Set<TaskNodeKey<?, ?>> getDependencyKeys(TaskNodeKey<?, ?> nodeKey) {
            Set<TaskNodeKey<?, ?>> result = dependencyGraph.get(nodeKey);
            return result != null ? result : Collections.emptySet();
        }

        private Collection<TaskNode<?, ?>> getDependencies(TaskNodeKey<?, ?> nodeKey) {
            Set<TaskNodeKey<?, ?>> keys = getDependencyKeys(nodeKey);
            if (keys.isEmpty()) {
                return Collections.emptySet();
            }

            Collection<TaskNode<?, ?>> result = new ArrayList<>(keys.size());
            keys.forEach((key) -> {
                TaskNode<?, ?> dependency = nodes.get(key);
                if (dependency != null) {
                    result.add(dependency);
                }
            });
            return result;
        }

        private void removeNode(TaskNodeKey<?, ?> nodeKey) {
            nodes.remove(nodeKey);
        }

        private CancellationToken getCancelToken() {
            return cancel.getToken();
        }
    }

    private static final class TaskNode<R, I> {
        private final TaskNodeKey<R, I> key;

        private NodeTaskRef<R> nodeTask;
        private Set<TaskNodeKey<?, ?>> inputKeys;

        private boolean hasResult;
        private R result;

        private volatile OneShotListenerManager<Runnable, Void> computedEvent;
        private final OneShotListenerManager<Runnable, Void> finishedEvent;

        private final AtomicBoolean scheduled;

        public TaskNode(TaskNodeKey<R, I> key) {
            this.key = key;
            this.hasResult = false;
            this.nodeTask = null;
            this.inputKeys = null;
            this.computedEvent = new OneShotListenerManager<>();
            this.finishedEvent = new OneShotListenerManager<>();
            this.scheduled = new AtomicBoolean(false);
        }

        public void addOnComputed(Runnable handler) {
            OneShotListenerManager<Runnable, Void> currentListeners = computedEvent;
            if (currentListeners != null) {
                currentListeners.registerOrNotifyListener(handler);
            }
            else {
                if (hasResult) {
                    handler.run();
                }
            }
        }

        public void addOnFinished(Runnable handler) {
            finishedEvent.registerOrNotifyListener(handler);
        }

        public void buildChildren(
                CancellationToken cancelToken,
                TaskGraphBuilderImpl graph) throws Exception {

            TaskFactoryKey<R, I> factoryKey = key.getFactoryKey();
            FactoryDef<R, I> factoryDef = graph.getFactoryDef(factoryKey);
            if (factoryDef == null) {
                throw new IllegalStateException("Missing node factory definition for key: " + factoryKey);
            }

            TaskInputBinderImpl inputBinder = new TaskInputBinderImpl(cancelToken, graph);

            I factoryArg = key.getFactoryArg();
            nodeTask = factoryDef.createTaskNode(cancelToken, factoryArg, inputBinder);
            if (nodeTask == null) {
                throw new NullPointerException("factoryDef.createTaskNode returned null for key " + factoryArg);
            }
            inputKeys = inputBinder.closeAndGetInputs();
        }

        public void ensureScheduleComputed(CancellationToken cancelToken, TaskErrorHandler errorHandler) {
            if (!scheduled.compareAndSet(false, true)) {
                return;
            }

            try {
                if (cancelToken.isCanceled()) {
                    finish();
                    return;
                }

                compute(cancelToken, (canceled, error) -> {
                    if (isError(canceled, error)) {
                        errorHandler.onError(key, error);
                    }
                    finish();
                });
            } catch (Throwable ex) {
                errorHandler.onError(key, ex);
                finish();
                throw ex;
            }
        }

        public void compute(CancellationToken cancelToken, CleanupTask cleanup) {
            NodeTaskRef<R> currentTask = nodeTask;
            if (currentTask == null) {
                throw new IllegalStateException("Node was not build when trying to compute it: " + key);
            }

            currentTask.properties.getExecutor().execute(cancelToken, (CancellationToken taskCancelToken) -> {
                result = currentTask.compute(taskCancelToken);
                hasResult = true;
                computed();
            }, cleanup);
        }

        private void computed() {
            OneShotListenerManager<Runnable, Void> currentListeners = computedEvent;
            if (currentListeners == null) {
                LOGGER.log(Level.WARNING,
                        "Node was marked as finished but computation completed after marked finished: {0}",
                        key);
                return;
            }

            EventListeners.dispatchRunnable(currentListeners);
        }

        private void finish() {
            EventListeners.dispatchRunnable(finishedEvent);
            // Once finished, computed event must have triggered or they will never trigger.
            // We set it to null, so that we no longer retain the listeners.
            computedEvent = null;
        }

        public R getResult() {
            if (!hasResult) {
                throw new IllegalStateException("Trying to retrieve result of node before computation: " + key);
            }
            return result;
        }

        public Set<TaskNodeKey<?, ?>> getInputKeys() {
            Set<TaskNodeKey<?, ?>> currentInputKeys = inputKeys;
            if (currentInputKeys == null) {
                throw new IllegalStateException("Input keys were not built.");
            }
            return currentInputKeys;
        }
    }

    private static final class TaskInputBinderImpl implements TaskInputBinder {
        private final CancellationToken cancelToken;
        private TaskGraphBuilderImpl graph;

        private final Set<TaskNodeKey<?, ?>> inputKeys;

        public TaskInputBinderImpl(
                CancellationToken cancelToken,
                TaskGraphBuilderImpl graph) {
            this.cancelToken = cancelToken;
            this.graph = graph;
            this.inputKeys = new HashSet<>();
        }

        @Override
        public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey) {
            if (graph == null) {
                throw new IllegalStateException();
            }

            TaskNode<I, A> child = graph.addAndBuildNode(cancelToken, defKey);
            inputKeys.add(child.key);

            AtomicReference<TaskNode<I, A>> childRef = new AtomicReference<>(child);
            return () -> {
                TaskNode<I, A> node = childRef.get();
                if (node == null) {
                    throw new IllegalStateException("Input already consumed for key: " + defKey);
                }
                return node.getResult();
            };
        }

        public Set<TaskNodeKey<?, ?>> closeAndGetInputs() {
            graph = null;
            return new HashSet<>(inputKeys);
        }
    }

    private final class TaskFactoryDefinerImpl implements TaskFactoryDefiner {
        private final LazyFactoryConfigurer groupConfigurer;

        public TaskFactoryDefinerImpl(TaskFactoryGroupConfigurer groupConfigurer) {
            ExceptionHelper.checkNotNullArgument(groupConfigurer, "groupConfigurer");
            this.groupConfigurer = new LazyFactoryConfigurer(groupConfigurer);
        }

        @Override
        public <R, I> void defineFactory(TaskFactoryKey<R, I> defKey, TaskFactorySetup<R, I> setup) {
            FactoryDef<?, ?> prev = factoryDefs.putIfAbsent(defKey, new FactoryDef<>(defKey, groupConfigurer, setup));
            if (prev != null) {
                throw new IllegalStateException("Already defined factory for key: " + defKey);
            }
        }
    }

    private static final class NodeTaskRef<R> {
        private final TaskNodeProperties properties;
        private final CancelableFunction<R> task;

        public NodeTaskRef(TaskNodeProperties properties, CancelableFunction<R> task) {
            this.properties = properties;
            this.task = task;
        }

        public R compute(CancellationToken cancelToken) throws Exception {
            return task.execute(cancelToken);
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

        public void setDefaults(TaskFactoryProperties defaults) {
            groupConfigurer.setDefaults(defaults);
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
        private final TaskFactoryGroupConfigurer groupConfigurer;
        private final AtomicReference<TaskFactoryProperties> propertiesRef;

        private TaskFactoryProperties defaults;

        public LazyFactoryConfigurer(TaskFactoryGroupConfigurer groupConfigurer) {
            this.groupConfigurer = groupConfigurer;
            this.propertiesRef = new AtomicReference<>(null);

            this.defaults = null;
        }

        public TaskFactoryProperties getProperties() {
            TaskFactoryProperties result = propertiesRef.get();
            if (result == null) {
                TaskFactoryProperties.Builder resultBuilder = new TaskFactoryProperties.Builder(getDefaults());
                groupConfigurer.setup(resultBuilder);
                result = resultBuilder.build();
                if (!propertiesRef.compareAndSet(null, result)) {
                    result = propertiesRef.get();
                }
            }
            return result;
        }

        public void setDefaults(TaskFactoryProperties defaults) {
            this.defaults = defaults;
        }

        private TaskFactoryProperties getDefaults() {
            TaskFactoryProperties result = defaults;
            if (result == null) {
                throw new IllegalStateException("Defaults were not yet set.");
            }
            return result;
        }
    }
}
