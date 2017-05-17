package org.jtrim2.concurrent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.OperationCanceledException;
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
    private static final Logger LOGGER = Logger.getLogger(Tasks.class.getName());

    /**
     * Logs the argument as a {@code SEVERE} level issue, if it is not {@code null}
     * and does not represent cancellation.
     * <P>
     * This function is expected to be used with {@code CompletionStage}'s
     * {@code exceptionally} method to log any uncaught error. This is important to do as a last action
     * because otherwise the stack trace of the thrown exception will be lost. That is, the intended use is:
     * <pre>{@code
     * CompletionStage<?> future = ...;
     * future.exceptionally(Tasks::expectNoError);
     * }</pre>
     * <P>
     * This method does not log {@code OperationCanceledException} because
     * cancellation is considered as a normal event.
     *
     * @param error the error to be logged if not {@code null}. This argument can be {@code null},
     *   in which case, this method does nothing.
     * @return always null
     */
    public static Void expectNoError(Throwable error) {
        if (error != null && !(error instanceof OperationCanceledException)) {
            LOGGER.log(Level.SEVERE, "Uncaught exception in task.", error);
        }
        return null;
    }

    /**
     * Returns a {@code BiConsumer} passable to the {@code whenComplete} method of {@code CompletionStage}
     * completing the passed {@code CompletableFuture}. That is, the returned {@code BiConsumer}
     * simply calls the {@link #complete(Object, Throwable, CompletableFuture) complete} method
     * with the arguments passed to the returned {@code BiConsumer}.
     *
     * @param <V> the type of the result of the asynchronous computation
     * @param future the future to be completed by the returned {@code BiConsumer}. This argument
     *   cannot be {@code null}.
     * @return a {@code BiConsumer} passable to the {@code whenComplete} method of {@code CompletionStage}
     *   completing the passed {@code CompletableFuture}. This method never returns {@code null}.
     */
    public static <V> BiConsumer<V, Throwable> completeForwarder(CompletableFuture<? super V> future) {
        Objects.requireNonNull(future, "future");
        return (result, error) -> complete(result, error, future);
    }

    /**
     * Completes the passed future exceptionally or normally. This method was designed to be called
     * from the {@code whenComplete} method of {@code CompletionStage}.
     * <P>
     * If the passed exception is not {@code null}, the passed future is completed exceptionally with
     * the given error. Otherwise, the passed future is completed normally with the given {@code result}.
     *
     * @param <V> the type of the result of the asynchronous computation
     * @param result the result of the asynchronous computation to complete the passed future normally with.
     *   This argument can be {@code null}, if the asynchronous computation yielded {@code null} result,
     *   and should be {@code null} if the passed error is not {@code null}.
     * @param error the exception to complete the passed future with (if not {@code null}. This
     *   argument can be {@code null}, if the asynchronous computation completed normally. However,
     *   if not {@code null}, the result argument will be ignored.
     * @param future the {@code CompletableFuture} to be completed. This future will always be
     *   completed after this method returns. This argument cannot be {@code null}.
     */
    public static <V> void complete(V result, Throwable error, CompletableFuture<? super V> future) {
        if (error != null) {
            future.completeExceptionally(error);
        }
        else {
            future.complete(result);
        }
    }

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
     * What happens when the returned task is attempted to be executed depends
     * on the {@code failOnReRun} argument. If the argument is {@code true},
     * attempting to call the {@code run} method of the returned
     * {@code Runnable} multiple times will cause an
     * {@code IllegalStateException} to be thrown. If the argument is
     * {@code false}, calling the {@code run} method of the returned
     * {@code Runnable} multiple times will only result in a single execution
     * and every other call (not actually executing the specified
     * {@code Runnable}) will silently return without doing anything.
     *
     * @param task the {@code Runnable} to which calls are to be forwarded by
     *   the returned {@code Runnable}. This method cannot be {@code null}.
     * @param failOnReRun if {@code true} multiple calls to the {@code run()}
     *   method of the returned {@code Runnable} will cause an
     *   {@code IllegalStateException} to be thrown. If this argument is
     *   {@code false} subsequent calls after the first call to the
     *   {@code run()} method of the returned {@code Runnable} will silently
     *   return without doing anything.
     * @return the {@code Runnable} which will execute the specified
     *   {@code Runnable} but will execute the specified {@code Runnable} only
     *   once. This method never returns {@code null}.
     */
    public static Runnable runOnceTask(
            Runnable task, boolean failOnReRun) {

        return new RunOnceTask(task, failOnReRun);
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
        for (int i = 0; i < exceptions.length; i++) {
            Throwable current = exceptions[i];
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
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] == null) continue;

            boolean threadStopped = false;
            while (!threadStopped) {
                try {
                    threads[i].join();
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
