package org.jtrim2.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contains static helper methods for shutting down an application.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are not
 * <I>synchronization transparent</I>.
 */
public final class ShutdownHelper {
    private static void trySleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startShutdownTask(Runnable task, int status,
            boolean daemon) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkExit(status);
        }

        // Starting a new thread can be important if
        // task waits for an event occuring in the current thread.
        Thread exitTask = new Thread(task, "System shutdown task.");
        if (exitTask.isDaemon() != daemon) {
            exitTask.setDaemon(daemon);
        }
        exitTask.start();
    }

    /**
     * {@link Runtime#halt(int) Halts} the JVM if it does not shutdown after a
     * given timeout. This method may require certain permission from the
     * currently present security manager.
     * <P>
     * This method will start a new daemon thread and will attempt to
     * invoke the {@code Runtime.halt} method on this daemon thread. Notice,
     * that a daemon thread is started which does not prevent the JVM to
     * shutdown. Also, this method will return immediately regardless of the
     * timeout argument.
     * <P>
     * This method is intended to be used as a last resort shutting down if
     * the JVM fails to shutdown when the application believes it should (most
     * likely because of remaining non-daemon threads).
     *
     * @param status the {@code int} value to be passed to the
     *   {@code Runtime.halt} method. This value can be anything which is
     *   allowed by the currently present security manager.
     * @param msToWait the milliseconds to wait before actually calling the
     *   {@code Runtime.halt} method. This argument must be greater than or
     *   equal to zero.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is lower than zero
     * @throws SecurityException thrown if there is a security manager present
     *   and its {@link SecurityManager#checkExit(int) checkExit} method does
     *   not allow to terminate with the given status
     *
     * @see #exitLater(int, int)
     * @see #exitLater(Runnable, int, int)
     */
    public static void haltLater(final int status, final int msToWait) {
        ExceptionHelper.checkArgumentInRange(msToWait,
                0, Integer.MAX_VALUE, "msToWait");

        if (msToWait == 0) {
            Runtime.getRuntime().halt(status);
        }

        startShutdownTask(() -> {
            try {
                trySleep(msToWait);
            } finally {
                Runtime.getRuntime().halt(status);
            }
        }, status, true);
    }

    /**
     * {@link System#exit(int) Exits} the JVM if it does not shutdown after a
     * given timeout. This method may require certain permission from the
     * currently present security manager.
     * <P>
     * This method will start a new daemon thread and will attempt to
     * invoke the {@code System.exit} method on this daemon thread. Notice,
     * that a daemon thread is started which does not prevent the JVM to
     * shutdown. Also, this method will return immediately regardless of the
     * timeout argument.
     * <P>
     * This method is intended to be used as a last resort shutting down if
     * the JVM fails to shutdown when the application believes it should (most
     * likely because of remaining non-daemon threads).
     *
     * @param status the {@code int} value to be passed to the
     *   {@code System.exit} method. This value can be anything which is
     *   allowed by the currently present security manager.
     * @param msToWait the milliseconds to wait before actually calling the
     *   {@code System.exit} method. This argument must be greater than or
     *   equal to zero.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is lower than zero
     * @throws SecurityException thrown if there is a security manager present
     *   and its {@link SecurityManager#checkExit(int) checkExit} method does
     *   not allow to terminate with the given status
     *
     * @see #exitLater(Runnable, int, int)
     * @see #haltLater(int, int)
     */
    public static void exitLater(final int status, final int msToWait) {
        ExceptionHelper.checkArgumentInRange(msToWait,
                0, Integer.MAX_VALUE, "msToWait");

        if (msToWait == 0) {
            System.exit(status);
        }

        startShutdownTask(() -> {
            try {
                trySleep(msToWait);
            } finally {
                System.exit(status);
            }
        }, status, true);
    }

    /**
     * {@link System#exit(int) Exits} the JVM if it does not shutdown after a
     * given timeout and executes a given task before shutting down. This method
     * may require certain permission from the currently present security
     * manager.
     * <P>
     * The method will start a new non-daemon thread and will execute the task
     * on this thread. After this task completes, it will start a new daemon
     * thread and after the given timeout, it will attempt to invoke the
     * {@code System.exit} method on this daemon thread. Notice, that a daemon
     * thread is started which does not prevent the JVM to shutdown. Also, this
     * method will return immediately regardless of the timeout argument.
     * <P>
     * This method is intended to be used as a last resort shutting down if
     * the JVM fails to shutdown when the application believes it should (most
     * likely because of remaining non-daemon threads).
     *
     * @param exitTask the task to be executed before shutting down the JVM.
     *   This argument can be {@code null}, in which case no task will be
     *   executed.
     * @param status the {@code int} value to be passed to the
     *   {@code System.exit} method. This value can be anything which is
     *   allowed by the currently present security manager.
     * @param msToWait the milliseconds to wait before actually calling the
     *   {@code System.exit} method. This argument must be greater than or
     *   equal to zero.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is lower than zero
     * @throws SecurityException thrown if there is a security manager present
     *   and its {@link SecurityManager#checkExit(int) checkExit} method does
     *   not allow to terminate with the given status
     *
     * @see #exitLater(int, int)
     * @see #haltLater(int, int)
     */
    public static void exitLater(final Runnable exitTask, final int status,
            final int msToWait) {
        ExceptionHelper.checkArgumentInRange(msToWait,
                0, Integer.MAX_VALUE, "msToWait");

        startShutdownTask(() -> {
            try {
                if (exitTask != null) {
                    exitTask.run();
                }
            } finally {
                exitLater(status, msToWait);
            }
        }, status, false);
    }

    /**
     * Immediately starts a non-daemon thread, executes the specified task
     * on it and {@link System#exit(int) terminates} the JVM.
     *
     * @param exitTask the task to be executed before shutting down the JVM.
     *   This argument can be {@code null}, in which case no task will be
     *   executed.
     * @param status the {@code int} value to be passed to the
     *   {@code System.exit} method. This value can be anything which is
     *   allowed by the currently present security manager.
     *
     * @throws SecurityException thrown if there is a security manager present
     *   and its {@link SecurityManager#checkExit(int) checkExit} method does
     *   not allow to terminate with the given status
     */
    public static void exit(final Runnable exitTask, final int status) {
        startShutdownTask(() -> {
            try {
                if (exitTask != null) {
                    exitTask.run();
                }
            } finally {
                System.exit(status);
            }
        }, status, false);
    }

    /**
     * Invokes the {@link ExecutorService#shutdown() shutdown} method of the
     * specified executors.
     *
     * @param executors the array of executors whose {@code shutdown} method is
     *   to be called. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     *
     * @throws NullPointerException thrown if the specified array is
     *   {@code null} or contains {@code null} elements
     */
    public static void shutdownExecutors(ExecutorService... executors) {
        ExceptionHelper.checkNotNullElements(executors, "executors");

        for (ExecutorService executor: executors) {
            executor.shutdown();
        }
    }

    /**
     * Invokes the {@link ExecutorService#shutdownNow() shutdownNow} method of
     * the specified executors and returns all the tasks returned by these
     * {@code shutdownNow} methods in a single list.
     *
     * @param executors the array of executors whose {@code shutdownNow} method
     *   is to be called. This argument cannot be {@code null} and cannot
     *   contain {@code null} elements.
     * @return the list of tasks returned by the {@code shutdownNow} methods
     *   of the specified executors. This list is the concatenation of the task
     *   lists returned by the {@code shutdownNow} methods. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified array is
     *   {@code null} or contains {@code null} elements
     */
    public static List<Runnable> shutdownNowExecutors(
            ExecutorService... executors) {
        ExceptionHelper.checkNotNullElements(executors, "executors");

        List<Runnable> result = new ArrayList<>();

        for (ExecutorService executor: executors) {
            result.addAll(executor.shutdownNow());
        }

        return result;
    }

    /**
     * Waits until all the specified executors terminate or the current thread
     * gets interrupted. In case the current thread gets interrupted, this
     * method returns immediately and keeps the interrupted status of the
     * thread.
     *
     * @param executors the executors to wait to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     *
     * @throws NullPointerException thrown if the specified executor array is
     *   {@code null} or one of the executors is {@code null}
     *
     * @see #awaitTerminateExecutors(ExecutorService[])
     */
    public static void awaitTerminateExecutorsSilently(
            ExecutorService... executors) {

        try {
            awaitTerminateExecutors(executors);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits until all the specified executors terminate or the given timeout
     * elapses or the current thread gets interrupted. In case the current
     * thread gets interrupted, this method returns immediately and keeps the
     * interrupted status of the thread.
     *
     * @param timeout the maximum time to wait for the executors to terminate
     *   in the given time unit. This argument must be greater than or equal to
     *   zero.
     * @param timeunit the time unit of the timeout argument. This argument
     *   cannot be {@code null}.
     * @param executors the executors to wait to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     * @return {@code true} if the specified executors have terminated,
     *   {@code false} if the timeout expired or the current got interrupted
     *   before the executors terminated
     *
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is lower than zero
     * @throws NullPointerException thrown if the {@code timeunit} argument or
     *   the specified executor array or one of the executors is {@code null}
     *
     * @see #awaitTerminateExecutors(long, TimeUnit, ExecutorService[])
     */
    public static boolean awaitTerminateExecutorsSilently(
            long timeout, TimeUnit timeunit, ExecutorService... executors) {

        boolean result = false;
        try {
            result = awaitTerminateExecutors(timeout, timeunit, executors);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    /**
     * Waits until all the specified executors terminate or the current thread
     * gets interrupted. In case the current thread gets interrupted, this
     * method will throw an {@link InterruptedException} and clear the
     * interrupted status of the thread.
     *
     * @param executors the executors to wait to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before all the specified executor has terminated
     * @throws NullPointerException thrown if the specified executor array is
     *   {@code null} or one of the executors is {@code null}
     *
     * @see #awaitTerminateExecutorsSilently(ExecutorService[])
     */
    public static void awaitTerminateExecutors(ExecutorService... executors)
            throws InterruptedException {
        ExceptionHelper.checkNotNullElements(executors, "executors");

        for (ExecutorService executor: executors) {
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                // After waiting about 300 years we should interrupt ourselves
                // and not wait another century.
                throw new InterruptedException();
            }
        }
    }

    /**
     * Waits until all the specified executors terminate or the given timeout
     * elapses or the current thread gets interrupted. In case the current
     * thread gets interrupted, this method will throw an
     * {@link InterruptedException} and clear the interrupted status of the
     * thread.
     *
     * @param timeout the maximum time to wait for the executors to terminate
     *   in the given time unit. This argument must be greater than or equal to
     *   zero.
     * @param timeunit the time unit of the timeout argument. This argument
     *   cannot be {@code null}.
     * @param executors the executors to wait to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     * @return {@code true} if the specified executors have terminated before
     *   the given timeout expired, {@code false} otherwise
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before all the specified executor has terminated or the given timeout
     *   expired
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is lower than zero
     * @throws NullPointerException thrown if the {@code timeunit} argument or
     *   the specified executor array or one of the executors is {@code null}
     *
     * @see #awaitTerminateExecutorsSilently(long, TimeUnit, ExecutorService[])
     */
    public static boolean awaitTerminateExecutors(
            long timeout,
            TimeUnit timeunit,
            ExecutorService... executors) throws InterruptedException {

        ExceptionHelper.checkArgumentInRange(timeout,
                0, Long.MAX_VALUE, "timeout");
        Objects.requireNonNull(timeunit, "timeunit");
        ExceptionHelper.checkNotNullElements(executors, "executors");

        final long startTime = System.nanoTime();
        final long toWait = timeunit.toNanos(timeout);

        for (ExecutorService executor: executors) {
            long thisWait = toWait - (System.nanoTime() - startTime);
            if (thisWait <= 0) {
                break;
            }

            executor.awaitTermination(thisWait, TimeUnit.NANOSECONDS);
        }

        for (ExecutorService executor: executors) {
            if (!executor.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    private ShutdownHelper() {
        throw new AssertionError();
    }
}
