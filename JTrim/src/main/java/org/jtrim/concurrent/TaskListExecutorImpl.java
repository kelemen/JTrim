/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.jtrim.collections.*;
import org.jtrim.collections.RefList.ElementRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TaskListExecutorImpl {
    private static final int STATE_RUNNING = 0;
    private static final int STATE_TERMINATING = 1;
    private static final int STATE_TERMINATED = 2;

    private volatile int state;

    private final Executor backingExecutor;
    private final ReentrantLock mainLock;
    private final Condition endSignal;

    private final RefList<Runnable> tasks;
    private int activeCount;

    private final TaskRefusePolicy taskRefusePolicy;
    private final ExecutorShutdownListener shutdownListener;

    public TaskListExecutorImpl(Executor backingExecutor) {
        this(backingExecutor, null);
    }

    public TaskListExecutorImpl(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy) {
        this(backingExecutor, taskRefusePolicy, null);
    }

    public TaskListExecutorImpl(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy,
            ExecutorShutdownListener shutdownListener) {

        this.activeCount = 0;
        this.state = STATE_RUNNING;
        this.mainLock = new ReentrantLock();
        this.endSignal = this.mainLock.newCondition();
        this.backingExecutor = backingExecutor;
        this.tasks = new RefLinkedList<>();
        this.shutdownListener = shutdownListener;
        this.taskRefusePolicy = taskRefusePolicy != null
                ? taskRefusePolicy
                : SilentTaskRefusePolicy.INSTANCE;
    }

    private void onTerminate() {
        if (shutdownListener != null) {
            shutdownListener.onTerminate();
        }
    }

    private boolean tryTerminate() {
        assert mainLock.isHeldByCurrentThread();
        assert activeCount >= 0;

        if (state == STATE_TERMINATING && activeCount == 0 && tasks.isEmpty()) {
            state = STATE_TERMINATED;
            endSignal.signalAll();
            return true;
        }
        else {
            return false;
        }
    }

    private static class ElementAndRef {
        public final RefList.ElementRef<?> ref;
        public final Object element;

        public ElementAndRef(ElementRef<?> ref) {
            this.ref = ref;
            this.element = ref.getElement();
        }
    }

    public void purge() {
        List<ElementAndRef> taskRefs = new LinkedList<>();

        mainLock.lock();
        try {
            RefList.ElementRef<?> ref = tasks.getFirstReference();
            while (ref != null) {
                taskRefs.add(new ElementAndRef(ref));
                ref = ref.getNext(1);
            }
        } finally {
            mainLock.unlock();
        }

        Iterator<ElementAndRef> taskRefItr = taskRefs.iterator();
        while (taskRefItr.hasNext()) {
            ElementAndRef ref = taskRefItr.next();
            Object element = ref.element;

            if (element instanceof Future
                    && ((Future<?>)element).isCancelled()) {
                taskRefItr.remove();
            }
        }

        if (!taskRefs.isEmpty()) {
            boolean terminatedNow = false;
            mainLock.lock();
            try {
                for (ElementAndRef ref: taskRefs) {
                    ref.ref.remove();
                }

                terminatedNow = tryTerminate();
            } finally {
                mainLock.unlock();

                if (terminatedNow) {
                    onTerminate();
                }
            }
        }
    }

    public Executor getBackingExecutor() {
        return backingExecutor;
    }

    public void shutdown() {
        boolean terminatedNow = false;
        mainLock.lock();
        try {
            if (state < STATE_TERMINATING) {
                state = STATE_TERMINATING;
                terminatedNow = tryTerminate();
            }
        } finally {
            mainLock.unlock();

            if (terminatedNow) {
                onTerminate();
            }
        }
    }

    public List<Runnable> shutdownNow() {
        boolean terminatedNow = false;

        List<Runnable> remaining = Collections.emptyList();

        mainLock.lock();
        try {
            if (state < STATE_TERMINATED) {
                remaining = new ArrayList<>(tasks);
                tasks.clear();

                // If we cannot terminate now (i.e.: there are active tasks)
                // the last active task will terminate.
                // Note that state is not greater than STATE_TERMINATING
                // because of the outer check.
                state = STATE_TERMINATING;
                terminatedNow = tryTerminate();
            }
        } finally {
            mainLock.unlock();

            if (terminatedNow) {
                onTerminate();
            }
        }

        return remaining;
    }

    public boolean isShutdown() {
        return state >= STATE_TERMINATING;
    }

    public boolean isTerminated() {
        return state == STATE_TERMINATED;
    }

    public boolean awaitTermination() throws InterruptedException {
        mainLock.lock();
        try {
            while (state != STATE_TERMINATED) {
                endSignal.await();
            }
        } finally {
            mainLock.unlock();
        }

        return true;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        long nanosToWait = unit.toNanos(timeout);
        mainLock.lock();
        try {
            while (nanosToWait > 0 && state != STATE_TERMINATED) {
                nanosToWait = endSignal.awaitNanos(nanosToWait);
            }
            return state == STATE_TERMINATED;
        } finally {
            mainLock.unlock();
        }
    }

    private void executeActive(Runnable task) {
        assert !mainLock.isHeldByCurrentThread();

        boolean terminatedNow = false;

        try {
            if (task != null) {
                task.run();
            }
        } finally {
            mainLock.lock();
            try {
                activeCount--;
                terminatedNow = tryTerminate();
            } finally {
                mainLock.unlock();

                if (terminatedNow) {
                    onTerminate();
                }
            }
        }
    }

    public boolean executeNow(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        mainLock.lock();
        try {
            if (isShutdown()) {
                return false;
            }

            activeCount++;
        } finally {
            mainLock.unlock();
        }

        executeActive(task);
        return true;
    }

    private RefList.ElementRef<Runnable> tryExecute(Runnable command) {
        ExceptionHelper.checkNotNullArgument(command, "command");

        if (backingExecutor == null) {
            throw new IllegalStateException("execute method cannot be called"
                    + " if the backing executor was not specified.");
        }

        RefList.ElementRef<Runnable> task;

        mainLock.lock();
        try {
            if (state >= STATE_TERMINATING) {
                return null;
            }

            task = tasks.addLastGetReference(command);
        } finally {
            mainLock.unlock();
        }

        backingExecutor.execute(new SingleTaskExecutor(task));
        return task;
    }

    public void execute(Runnable command) {
        if (tryExecute(command) == null) {
            taskRefusePolicy.refuseTask(command);
        }
    }

    public RefCollection.ElementRef<Runnable> executeAndGetRef(
            Runnable command) {

        RefList.ElementRef<Runnable> ref = tryExecute(command);
        if (ref == null) {
            taskRefusePolicy.refuseTask(command);
            return null;
        }
        else {
            return new TaskRefRemover(ref);
        }
    }

    private class SingleTaskExecutor implements Runnable {
        private final RefList.ElementRef<Runnable> task;

        public SingleTaskExecutor(RefList.ElementRef<Runnable> task) {
            this.task = task;
        }

        @Override
        public void run() {
            Runnable currentTask = null;

            mainLock.lock();
            try {
                if (!task.isRemoved()) {
                    currentTask = task.getElement();
                    task.remove();
                }

                activeCount++;
            } finally {
                mainLock.unlock();
            }

            executeActive(currentTask);
        }
    }

    private class TaskRefRemover implements RefCollection.ElementRef<Runnable> {
        private final RefList.ElementRef<Runnable> taskRef;

        public TaskRefRemover(ElementRef<Runnable> taskRef) {
            this.taskRef = taskRef;
        }

        @Override
        public boolean isRemoved() {
            mainLock.lock();
            try {
                return taskRef.isRemoved();
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public void remove() {
            boolean terminatedNow = false;
            mainLock.lock();
            try {
                taskRef.remove();
                terminatedNow = tryTerminate();
            } finally {
                mainLock.unlock();

                if (terminatedNow) {
                    onTerminate();
                }
            }
        }

        @Override
        public Runnable setElement(Runnable newElement) {
            throw new UnsupportedOperationException("Task cannot be replaced");
        }

        @Override
        public Runnable getElement() {
            // This lock is not required because we do not update
            // the element. Still the lock will remain because this way we don't
            // have to worry about future changes.
            mainLock.lock();
            try {
                return taskRef.getElement();
            } finally {
                mainLock.unlock();
            }
        }
    }
}
