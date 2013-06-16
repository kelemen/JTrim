package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see TaskExecutors#inOrderExecutor(TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class InOrderTaskExecutor implements MonitorableTaskExecutor {
    private final TaskExecutor executor;
    private final Lock queueLock;
    private final AtomicReference<Thread> dispatcherThread; // null means that noone is dispatching
    private final RefLinkedList<TaskDef> taskQueue;

    public InOrderTaskExecutor(TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
        this.queueLock = new ReentrantLock();
        this.dispatcherThread = new AtomicReference<>(null);
        this.taskQueue = new RefLinkedList<>();
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
            return taskQueue.pollFirst();
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
                    // by the task (and the cleanup task) is caught and logged.
                    if (toThrow == null) toThrow = ex;
                    else toThrow.addSuppressed(ex);
                } finally {
                    dispatcherThread.set(null);
                }
            }
            else {
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
    public void execute(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        final TaskDef taskDef = new TaskDef(cancelToken, task, cleanupTask);

        final RefCollection.ElementRef<TaskDef> taskDefRef;
        queueLock.lock();
        try {
            taskDefRef = taskQueue.addLastGetReference(taskDef);
        } finally {
            queueLock.unlock();
        }

        final ListenerRef cancelRef;
        if (taskDef.hasCleanupTask()) {
            cancelRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    taskDef.removeTask();
                }
            });
        }
        else {
            cancelRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    queueLock.lock();
                    try {
                        taskDefRef.remove();
                    } finally {
                        queueLock.unlock();
                    }
                }
            });
        }

        final AtomicBoolean executorCancellation = new AtomicBoolean(true);
        // Notice that we pass an Cancellation.UNCANCELABLE_TOKEN and so we
        // assume if the submitted task gets canceled
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                try {
                    dispatchTasks(cancelToken);
                } finally {
                    executorCancellation.set(false);
                }
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                try {
                    cancelRef.unregister();
                } finally {
                    // If the executor did not execute our task, this might be
                    // our last chance to execute the cleanup tasks.
                    // Note that since we pass CANCELED_TOKEN, only cleanup
                    // tasks will be executed.
                    if (executorCancellation.get()) {
                        dispatchTasks(Cancellation.CANCELED_TOKEN);
                    }
                }
            }
        });
    }

    private static class TaskDef {
        private volatile CancellationToken cancelToken;
        private volatile CancelableTask task;
        private final CleanupTask cleanupTask;

        public TaskDef(
                CancellationToken cancelToken,
                CancelableTask task,
                CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            this.cancelToken = cancelToken;
            this.task = task;
            this.cleanupTask = cleanupTask;
        }

        public void doTask(CancellationToken executorCancelToken) {
            CancellationToken currentCancelToken = cancelToken;
            currentCancelToken = currentCancelToken != null
                    ? Cancellation.anyToken(executorCancelToken, currentCancelToken)
                    : executorCancelToken;
            Tasks.executeTaskWithCleanup(currentCancelToken, task, cleanupTask);
        }

        public void removeTask() {
            task = null;
            cancelToken = null;
        }

        public boolean hasCleanupTask() {
            return cleanupTask != null;
        }
    }
}
