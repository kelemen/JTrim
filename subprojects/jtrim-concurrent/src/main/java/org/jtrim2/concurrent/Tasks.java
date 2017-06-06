package org.jtrim2.concurrent;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines static methods to return simple, convenient task related instances.
 * <P>
 * This class cannot be inherited nor instantiated.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 */
public final class Tasks {
    /**
     * Returns a {@code Runnable} whose {@code run()} method does nothing but
     * returns immediately to the caller.
     *
     * @return a {@code Runnable} whose {@code run()} method does nothing but
     *   returns immediately to the caller. This method never returns
     *   {@code null}.
     */
    public static Runnable noOpTask() {
        return NoOp.INSTANCE;
    }

    /**
     * Returns a {@code Runnable} which will execute the specified
     * {@code Runnable} but will execute the specified {@code Runnable} only
     * once. The specified task will not be executed more than once even if
     * it is called multiple times concurrently (and is allowed to be called
     * concurrently).
     * <P>
     * Calling the {@code run} method of the returned {@code Runnable} multiple times
     * will only result in a single execution and every other call (not actually
     * executing the specified {@code Runnable}) will silently return without doing anything.
     *
     * @param task the {@code Runnable} to which calls are to be forwarded by
     *   the returned {@code Runnable}. This method cannot be {@code null}.
     * @return the {@code Runnable} which will execute the specified
     *   {@code Runnable} but will execute the specified {@code Runnable} only
     *   once. This method never returns {@code null}.
     */
    public static Runnable runOnceTask(Runnable task) {
        if (isLenientRunOnceTask(task)) {
            return task;
        }

        return new RunOnceTask(task, false);
    }

    private static boolean isLenientRunOnceTask(Runnable task) {
        if (task.getClass() != RunOnceTask.class) {
            return false;
        }

        return !((RunOnceTask)task).failOnReRun;
    }

    /**
     * Returns a {@code Runnable} which will execute the specified
     * {@code Runnable} but will execute the specified {@code Runnable} only
     * once, failing on multiple run attempts. The specified task will not be
     * executed more than once even if it is called multiple times concurrently
     * (and is allowed to be called concurrently).
     * <P>
     * Attempting to call the {@code run} method of the returned
     * {@code Runnable} multiple times will cause an
     * {@code IllegalStateException} to be thrown after the first attempt.
     *
     * @param task the {@code Runnable} to which calls are to be forwarded by
     *   the returned {@code Runnable}. This method cannot be {@code null}.
     * @return the {@code Runnable} which will execute the specified
     *   {@code Runnable} but will execute the specified {@code Runnable} only
     *   once. This method never returns {@code null}.
     */
    public static Runnable runOnceTaskStrict(Runnable task) {
        return new RunOnceTask(task, true);
    }

    /**
     * Executes the specified tasks concurrently, each on a separate thread,
     * attempting to execute them as concurrently as possible. This method was
     * designed for test codes wanting to test the behaviour of multiple tasks
     * if run concurrently. This method will attempt to start the passed tasks
     * in sync (this does not imply thread-safety guarantees), so that there is
     * a better chance that they actually run concurrently.
     * <P>
     * This method will wait until all the specified tasks complete.
     * <P>
     * <B>Warning</B>: This method was <B>not</B> designed to give better
     * performance by running the tasks concurrently. Performance of this method
     * is secondary to any other purposes.
     *
     * @param tasks the tasks to be run concurrently. Each of the specified
     *   tasks will run on a dedicated thread. This argument and its elements
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or any of the
     *   specified tasks is {@code null}
     * @throws TaskExecutionException thrown if any of the tasks thrown an
     *   exception. The first exception (in the order they were passed) is the
     *   cause of this exception and subsequent exceptions are suppressed
     *   (via {@code Throwable.addSuppressed}).
     */
    public static void runConcurrently(Runnable... tasks) {
        ExceptionHelper.checkNotNullElements(tasks, "tasks");

        final CountDownLatch latch = new CountDownLatch(tasks.length);
        Thread[] threads = new Thread[tasks.length];
        final Throwable[] exceptions = new Throwable[tasks.length];

        try {
            for (int i = 0; i < threads.length; i++) {
                final Runnable task = tasks[i];

                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    try {
                        latch.countDown();
                        latch.await();

                        task.run();
                    } catch (Throwable ex) {
                        exceptions[threadIndex] = ex;
                    }
                });
                try {
                    threads[i].start();
                } catch (Throwable ex) {
                    threads[i] = null;
                    throw ex;
                }
            }
        } finally {
            joinThreadsSilently(threads);
        }

        TaskExecutionException toThrow = null;
        for (Throwable current: exceptions) {
            if (current != null) {
                if (toThrow == null) toThrow = new TaskExecutionException(current);
                else toThrow.addSuppressed(current);
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
    }

    private static void joinThreadsSilently(Thread[] threads) {
        boolean interrupted = false;
        for (Thread thread: threads) {
            if (thread == null) {
                continue;
            }

            boolean threadStopped = false;
            while (!threadStopped) {
                try {
                    thread.join();
                    threadStopped = true;
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static class RunOnceTask implements Runnable {
        private final boolean failOnReRun;
        private final AtomicReference<Runnable> taskRef;

        public RunOnceTask(Runnable task, boolean failOnReRun) {
            Objects.requireNonNull(task, "task");
            this.taskRef = new AtomicReference<>(task);
            this.failOnReRun = failOnReRun;
        }

        @Override
        public void run() {
            Runnable task = taskRef.getAndSet(null);
            if (task == null) {
                if (failOnReRun) {
                    throw new IllegalStateException("This task is not allowed"
                            + " to be called multiple times.");
                }
            }
            else {
                task.run();
            }
        }

        @Override
        public String toString() {
            final String strValueCaption = "Idempotent task";
            Runnable currentTask = taskRef.get();
            if (currentTask != null) {
                return strValueCaption + "{" + currentTask + "}";
            }
            else {
                return strValueCaption + "{Already executed}";
            }
        }
    }

    private enum NoOp implements Runnable {
        INSTANCE;

        @Override
        public void run() { }

        @Override
        public String toString() {
            return "NO-OP";
        }
    }

    private Tasks() {
        throw new AssertionError();
    }
}
