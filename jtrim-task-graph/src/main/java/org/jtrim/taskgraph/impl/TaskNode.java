package org.jtrim.taskgraph.impl;

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
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

public final class TaskNode<R, I> {
    private static final Logger LOGGER = Logger.getLogger(TaskNode.class.getName());

    private final TaskNodeKey<R, I> key;
    private final AtomicReference<NodeTaskRef<R>> nodeTaskRefRef;

    private boolean hasResult;
    private R result;

    private volatile OneShotListenerManager<Runnable, Void> computedEvent;
    private final OneShotListenerManager<Runnable, Void> finishedEvent;
    private volatile boolean finished;

    public TaskNode(TaskNodeKey<R, I> key, NodeTaskRef<R> nodeTask) {
        ExceptionHelper.checkNotNullArgument(key, "key");
        ExceptionHelper.checkNotNullArgument(nodeTask, "nodeTask");

        this.key = key;
        this.nodeTaskRefRef = new AtomicReference<>(nodeTask);
        this.hasResult = false;
        this.computedEvent = new OneShotListenerManager<>();
        this.finishedEvent = new OneShotListenerManager<>();
        this.finished = false;
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

    public void ensureScheduleComputed(CancellationToken cancelToken, TaskErrorHandler errorHandler) {
        NodeTaskRef<R> nodeTaskRef = nodeTaskRefRef.getAndSet(null);
        if (nodeTaskRef == null) {
            return;
        }

        try {
            if (cancelToken.isCanceled()) {
                finish();
                return;
            }

            compute(cancelToken, nodeTaskRef, (canceled, error) -> {
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

    private void compute(CancellationToken cancelToken, NodeTaskRef<R> nodeTaskRef, CleanupTask cleanup) {
        TaskExecutor executor = nodeTaskRef.getProperties().getExecutor();
        executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            if (finished) {
                return;
            }

            result = nodeTaskRef.compute(taskCancelToken);
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

    public void finish() {
        finished = true;
        EventListeners.dispatchRunnable(finishedEvent);
        // Once finished, computed event must have triggered or they will never trigger.
        // We set it to null, so that we no longer retain the listeners.
        computedEvent = null;
    }

    public boolean hasResult() {
        return hasResult;
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
}
