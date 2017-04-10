package org.jtrim.taskgraph.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.EventListeners;
import org.jtrim.event.OneShotListenerManager;
import org.jtrim.taskgraph.TaskErrorHandler;
import org.jtrim.taskgraph.TaskInputBinder;
import org.jtrim.taskgraph.TaskInputRef;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

public final class TaskNode<R, I> {
    private static final Logger LOGGER = Logger.getLogger(TaskNode.class.getName());

    private final TaskNodeKey<R, I> key;

    private NodeTaskRef<R> nodeTask;

    private boolean hasResult;
    private R result;

    private volatile OneShotListenerManager<Runnable, Void> computedEvent;
    private final OneShotListenerManager<Runnable, Void> finishedEvent;

    private final AtomicBoolean scheduled;

    public TaskNode(TaskNodeKey<R, I> key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        this.key = key;
        this.hasResult = false;
        this.nodeTask = null;
        this.computedEvent = new OneShotListenerManager<>();
        this.finishedEvent = new OneShotListenerManager<>();
        this.scheduled = new AtomicBoolean(false);
    }

    public TaskNodeKey<R, I> getKey() {
        return key;
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

    public Set<TaskNodeKey<?, ?>> buildChildren(
            CancellationToken cancelToken,
            TaskNodeBuilder nodeBuilder) throws Exception {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(nodeBuilder, "nodeBuilder");

        TaskInputBinderImpl inputBinder = new TaskInputBinderImpl(cancelToken, nodeBuilder);
        nodeTask = nodeBuilder.createNode(cancelToken, key, inputBinder);
        if (nodeTask == null) {
            throw new NullPointerException("TaskNodeBuilder.createNode returned null for key " + key);
        }
        return inputBinder.closeAndGetInputs();
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

        TaskExecutor executor = currentTask.getProperties().getExecutor();
        executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
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

    private static boolean isError(boolean canceled, Throwable error) {
        if (canceled && (error instanceof OperationCanceledException)) {
            return false;
        }
        return error != null;
    }

    private static final class TaskInputBinderImpl implements TaskInputBinder {
        private final TaskNodeBuilder nodeBuilder;
        private Set<TaskNodeKey<?, ?>> inputKeys;

        public TaskInputBinderImpl(CancellationToken cancelToken, TaskNodeBuilder nodeBuilder) {
            this.nodeBuilder = nodeBuilder;
            this.inputKeys = new HashSet<>();
        }

        @Override
        public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey) {
            Set<TaskNodeKey<?, ?>> currentInputKeys = inputKeys;
            if (currentInputKeys == null) {
                throw new IllegalStateException("May only be called from the associated task node factory.");
            }

            TaskNode<I, A> child = nodeBuilder.addAndBuildNode(defKey);
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
            Set<TaskNodeKey<?, ?>> result = inputKeys;
            inputKeys = null;
            return result;
        }
    }
}
