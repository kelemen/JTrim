package org.jtrim2.executor;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see TaskExecutors#inOrderExecutor(TaskExecutor)
 */
final class InOrderTaskExecutor
extends
    AbstractTaskExecutor
implements
        MonitorableTaskExecutor {
    private final TaskExecutor executor;
    private final Lock queueLock;
    private final AtomicReference<Thread> dispatcherThread; // null means that noone is dispatching
    private final Queue<TaskDef> taskQueue;

    public InOrderTaskExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
        this.queueLock = new ReentrantLock();
        this.dispatcherThread = new AtomicReference<>(null);
        this.taskQueue = new LinkedList<>();
    }

    private boolean isCurrentThreadDispatching() {
        return dispatcherThread.get() == Thread.currentThread();
    }

    private boolean isQueueEmpty() {
        queueLock.lock();
        try {
            return taskQueue.isEmpty();
        } finally {
            queueLock.unlock();
        }
    }

    private TaskDef pollFromQueue() {
        queueLock.lock();
        try {
            return taskQueue.poll();
        } finally {
            queueLock.unlock();
        }
    }

    private void dispatchTasks(CancellationToken cancelToken) {
        if (isCurrentThreadDispatching()) {
            // Tasks will be dispatched there.
            return;
        }

        Thread currentThread = Thread.currentThread();
        Throwable toThrow = null;
        while (!isQueueEmpty()) {
            if (dispatcherThread.compareAndSet(null, currentThread)) {
                try {
                    TaskDef taskDef = pollFromQueue();
                    if (taskDef != null) {
                        taskDef.doTask(cancelToken);
                    }
                } catch (Throwable ex) {
                    // Only in a case of a very serious error (like OutOfMemory)
                    // will this block be executed because exceptions thrown
                    // by the task is caught and logged.
                    if (toThrow == null) toThrow = ex;
                    else toThrow.addSuppressed(ex);
                } finally {
                    dispatcherThread.set(null);
                }
            } else {
                return;
            }
        }

        ExceptionHelper.rethrowIfNotNull(toThrow);
    }

    @Override
    public long getNumberOfQueuedTasks() {
        queueLock.lock();
        try {
            return taskQueue.size();
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public long getNumberOfExecutingTasks() {
        return dispatcherThread.get() != null ? 1 : 0;
    }

    @Override
    public boolean isExecutingInThis() {
        return isCurrentThreadDispatching();
    }

    @Override
    protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
        final TaskDef taskDef = new TaskDef(cancelToken, submittedTask);

        queueLock.lock();
        try {
            taskQueue.add(taskDef);
        } finally {
            queueLock.unlock();
        }

        final ListenerRef cancelRef = cancelToken.addCancellationListener(taskDef::removeTask);

        final AtomicBoolean executorCancellation = new AtomicBoolean(true);
        CompletionStage<Void> future = executor.execute(Cancellation.UNCANCELABLE_TOKEN, (taskCancelToken) -> {
            try {
                dispatchTasks(taskCancelToken);
            } finally {
                executorCancellation.set(false);
            }
        });
        future.whenComplete((result, error) -> {
            try {
                cancelRef.unregister();
            } finally {
                // If the executor did not execute our task, this might be
                // our last chance to execute the completion handlers.
                // Note that since we pass CANCELED_TOKEN, only completion
                // handlers will be executed.
                if (executorCancellation.get()) {
                    dispatchTasks(Cancellation.CANCELED_TOKEN);
                }
            }
        });
    }

    private static class TaskDef {
        private volatile CancellationToken cancelToken;
        private volatile SubmittedTask<?> submittedTask;

        public TaskDef(
                CancellationToken cancelToken,
                SubmittedTask<?> submittedTask) {
            this.cancelToken = Objects.requireNonNull(cancelToken, "cancelToken");
            this.submittedTask = Objects.requireNonNull(submittedTask, "submittedTask");
        }

        public void doTask(CancellationToken executorCancelToken) {
            CancellationToken currentCancelToken = cancelToken;
            currentCancelToken = currentCancelToken != null
                    ? Cancellation.anyToken(executorCancelToken, currentCancelToken)
                    : executorCancelToken;

            SubmittedTask<?> currentSubmittedTask = submittedTask;
            if (currentSubmittedTask != null) {
                currentSubmittedTask.execute(currentCancelToken);
            }
        }

        public void removeTask() {
            submittedTask = null;
            cancelToken = null;
        }
    }
}
