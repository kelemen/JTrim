package org.jtrim.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.utils.ExceptionHelper;

/**
 * This class contains static helper and factory methods for {@code Executor}s
 * and {@code ExecutorService}s. This class amongst others, allows new executors
 * to be created not provided by {@link java.util.concurrent.Executors}.
 * <P>
 * This class cannot be inherited and instantiated.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are
 * <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class ExecutorsEx {
    private static final RejectedExecutionHandler REJECT_POLICY
            = DiscardOnShutdownPolicy.INSTANCE;

    private static final long DEFAULT_THREAD_KEEPALIVE_TIME = 1000;

    /**
     * Returns a future representing a canceled task. Attempting to retrieve
     * its result (by a call to one of its {@code get} methods) will cause
     * an {@link java.util.concurrent.CancellationException} to be thrown. The
     * future is considered to be not done and canceled, so
     * {@link Future#cancel(boolean) canceling} it will always succeed.
     *
     * <h3>Thread safety</h3>
     * This method and the methods of the returned {@code Future} is safe to use
     * by multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * This method and the methods of the returned {@code Future} is
     * <I>synchronization transparent</I>.
     *
     * @param <T> the type of the result of the returned {@code Future}. Note
     *   that no such value can actually be retrieved from the returned
     *   {@code Future}.
     * @return the future representing a canceled task
     */
    @SuppressWarnings("unchecked")
    public static <T> Future<T> canceledFuture() {
        // This cast is safe because the future will never return a result
        // therefore there is no result of which type can be observed.
        return (Future<T>)CanceledFuture.INSTANCE;
    }

    /**
     * Invokes the {@link ExecutorService#shutdownNow() shutdownNow()}
     * method of all the specified executors.
     *
     * @param executors the executors to be shutted down. This argument cannot
     *   be {@code null} and cannot contain {@code null} elements.
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     *   or any of its elements is {@code null}
     */
    public static void shutdownExecutorsNow(ExecutorService... executors) {
        shutdownExecutorsNow(Arrays.asList(executors));
    }

    /**
     * Invokes the {@link ExecutorService#shutdownNow() shutdownNow()}
     * method of all the executors in the given collection.
     *
     * @param executors the executors to be shutted down. This argument cannot
     *   be {@code null} and cannot contain {@code null} elements.
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     *   or any of its elements is {@code null}
     */
    public static void shutdownExecutorsNow(
            Collection<? extends ExecutorService> executors) {
        ExceptionHelper.checkNotNullElements(executors, "executors");

        for (ExecutorService executor: executors) {
            executor.shutdownNow();
        }
    }

    /**
     * Waits until the specified {@code ExecutorService} terminates. That is,
     * this method waits until no more tasks will be executed by the given
     * {@code ExecutorService}.
     *
     * @param executor the executor waited to be terminated. This argument
     *   cannot be {@code null}.
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before the executor was terminated
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static void awaitExecutor(ExecutorService executor)
            throws InterruptedException {
        while (!executor.isTerminated()) {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Waits until all the specified {@code ExecutorService}s terminate.
     * That is, this method waits until no more tasks will be executed by the
     * specified {@code ExecutorService}s.
     *
     * @param executors the executors waited to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before at least one of the executor was terminated
     * @throws NullPointerException thrown if the argument is {@code null}
     *   or any of its elements is {@code null}
     */
    public static void awaitExecutors(ExecutorService... executors)
            throws InterruptedException {
        awaitExecutors(Arrays.asList(executors));
    }

    /**
     * Waits until all the {@code ExecutorService}s in the specified collection
     * terminate. That is, this method waits until no more tasks will be
     * executed by the specified {@code ExecutorService}s.
     *
     * @param executors the executors waited to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before at least one of the executor was terminated
     * @throws NullPointerException thrown if the argument is {@code null}
     *   or any of its elements is {@code null}
     */
    public static void awaitExecutors(
            Collection<? extends ExecutorService> executors)
                throws InterruptedException {
        ExceptionHelper.checkNotNullElements(executors, "executors");

        for (ExecutorService executor: executors) {
            awaitExecutor(executor);
        }
    }

    /**
     * Waits until all the specified {@code ExecutorService}s terminate or the
     * given timeout elapses. That is, this method waits until no more tasks
     * will be executed by the specified {@code ExecutorService}s or the
     * specified timeout elapses.
     *
     * @param timeout the maximum time to wait in the given time unit
     * @param timeunit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @param executors the executors waited to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     * @return {@code true} if all the executors have terminated, {@code false}
     *   if at least on of the executors has not yet terminated
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before at least one of the executor was terminated
     * @throws NullPointerException thrown if the {@code timeunit} or
     *   {@code executors} argument is {@code null} or any of the elements of
     *   {@code executors} is {@code null}
     */
    public static boolean awaitExecutors(long timeout, TimeUnit timeunit,
            ExecutorService... executors)
            throws InterruptedException {
        return awaitExecutors(timeout, timeunit, Arrays.asList(executors));
    }

    /**
     * Waits until all the {@code ExecutorService}s in the specified collection
     * terminate or the given timeout elapses. That is, this method waits until
     * no more tasks will be executed by the specified {@code ExecutorService}s
     * or the specified timeout elapses.
     *
     * @param timeout the maximum time to wait in the given time unit
     * @param timeunit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @param executors the executors waited to be terminated. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     * @return {@code true} if all the executors have terminated, {@code false}
     *   if at least on of the executors has not yet terminated
     *
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before at least one of the executor was terminated
     * @throws NullPointerException thrown if the {@code timeunit} or
     *   {@code executors} argument is {@code null} or any of the elements of
     *   {@code executors} is {@code null}
     */
    public static boolean awaitExecutors(long timeout, TimeUnit timeunit,
            Collection<? extends ExecutorService> executors)
                throws InterruptedException {
        ExceptionHelper.checkNotNullElements(executors, "executors");

        // Note that this code can possibly wait forever if long overflows but
        // that can only happen if we wait at least about 200 years.
        // Although it would be possible to fix this code, waiting 200 years
        // is nearly equivalent to waiting forever.
        final long toWaitNanos = timeunit.toNanos(timeout);
        final long startTime = System.nanoTime();
        for (ExecutorService executor: executors) {
            long elapsedNanos = System.nanoTime() - startTime;
            long remainingNanos = toWaitNanos - elapsedNanos;
            if (remainingNanos <= 0) {
                break;
            }

            executor.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS);
        }

        for (ExecutorService executor: executors) {
            if (!executor.isTerminated()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns an {@code ExecutorService} forwarding all of its methods to
     * the given {@code ExecutorService} but the returned
     * {@code ExecutorService} cannot be shutted down. Attempting to shutdown
     * the returned {@code ExecutorService} results in an unchecked
     * {@code UnsupportedOperationException} to be thrown.
     *
     * @param executor the executor to which calls to be forwarded by the
     *   returned {@code ExecutorService}. This argument cannot be {@code null}.
     * @return an {@code ExecutorService} which forwards all of its calls to
     *   the specified executor but cannot be shutted down. This method never
     *   returns {@code null}.
     */
    public static ExecutorService asUnstoppableExecutor(
            ExecutorService executor) {
        return new UnstoppableExecutor(executor);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} which will use at most the
     * specified maximum thread count and daemon status. The executor will
     * have a default name and one second timeout before its core threads will
     * terminate if there are no tasks available. Note that every thread of the
     * executor will eventually terminate if no tasks are submitted to the
     * executor even if it was not shutted down (and will be restarted on
     * demand).
     *
     * @param maxThreadCount the maximum number of threads to be used by the
     *   returned executor. This argument must be greater than zero.
     * @param isDaemon the daemon status of the threads used by the returned
     *   executor. Note that the JVM will terminate if there are only daemon
     *   threads are remaining.
     * @return the newly created {@code ThreadPoolExecutor} preinitialized
     *   with the requested properties. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxThreadCount} is not
     *   greater than zero
     *
     * @see #newMultiThreadedExecutor(int, long, boolean, String)
     */
    public static ThreadPoolExecutor newMultiThreadedExecutor(
            int maxThreadCount, boolean isDaemon) {

        return newMultiThreadedExecutor(maxThreadCount,
                DEFAULT_THREAD_KEEPALIVE_TIME, isDaemon, null);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} which will use at most the
     * specified maximum thread count, daemon status and name. The executor will
     * have a one second timeout before its core threads will terminate if there
     * are no tasks available. Note that every thread of the executor will
     * eventually terminate if no tasks are submitted to the executor even if it
     * was not shutted down (and will be restarted on demand).
     *
     * @param maxThreadCount the maximum number of threads to be used by the
     *   returned executor. This argument must be greater than zero.
     * @param isDaemon the daemon status of the threads used by the returned
     *   executor. Note that the JVM will terminate if there are only daemon
     *   threads are remaining.
     * @param poolName the name of the returned executor useful for debugging
     *   purposes. This string will be part of the name of the threads created
     *   by the returned executor. This argument can be {@code null} in which
     *   case a default name will be given to this executor.
     * @return the newly created {@code ThreadPoolExecutor} preinitialized
     *   with the requested properties. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxThreadCount} is not
     *   greater than zero
     *
     * @see #newMultiThreadedExecutor(int, long, boolean, String)
     */
    public static ThreadPoolExecutor newMultiThreadedExecutor(
            int maxThreadCount, boolean isDaemon, String poolName) {

        return newMultiThreadedExecutor(maxThreadCount,
                DEFAULT_THREAD_KEEPALIVE_TIME, isDaemon, poolName);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} which will use at most the
     * specified maximum thread count, daemon status, and thread keep alive
     * time. The executor will have a default name. Note that every thread of
     * the executor will eventually terminate if the specified thread keep alive
     * time elapses and no tasks are submitted to the executor even if it was
     * not shutted down (and will be restarted on demand).
     *
     * @param maxThreadCount the maximum number of threads to be used by the
     *   returned executor. This argument must be greater than zero.
     * @param threadKeepAliveTime the time in milliseconds to wait after
     *   the returned executor will shutdown idle threads. This argument
     *   must be greater than or equal to zero.
     * @param isDaemon the daemon status of the threads used by the returned
     *   executor. Note that the JVM will terminate if there are only daemon
     *   threads are remaining.
     * @return the newly created {@code ThreadPoolExecutor} preinitialized
     *   with the requested properties. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxThreadCount} is not
     *   greater than zero or {@code threadKeepAliveTime} is a negative integer
     *
     * @see #newMultiThreadedExecutor(int, long, boolean, String)
     */
    public static ThreadPoolExecutor newMultiThreadedExecutor(
            int maxThreadCount, long threadKeepAliveTime, boolean isDaemon) {

        return newMultiThreadedExecutor(maxThreadCount, threadKeepAliveTime,
                isDaemon, null);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} which will use at most the
     * specified maximum thread count, daemon status, name and thread keep alive
     * time. Note that every thread of the executor will
     * eventually terminate if the specified thread keep alive time elapses and
     * no tasks are submitted to the executor even if it was not shutted down
     * (and will be restarted on demand).
     *
     * @param maxThreadCount the maximum number of threads to be used by the
     *   returned executor. This argument must be greater than zero.
     * @param threadKeepAliveTime the time in milliseconds to wait after
     *   the returned executor will shutdown idle threads. This argument
     *   must be greater than or equal to zero.
     * @param isDaemon the daemon status of the threads used by the returned
     *   executor. Note that the JVM will terminate if there are only daemon
     *   threads are remaining.
     * @param poolName the name of the returned executor useful for debugging
     *   purposes. This string will be part of the name of the threads created
     *   by the returned executor. This argument can be {@code null} in which
     *   case a default name will be given to this executor.
     * @return the newly created {@code ThreadPoolExecutor} preinitialized
     *   with the requested properties. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxThreadCount} is not
     *   greater than zero or {@code threadKeepAliveTime} is a negative integer
     *
     * @see #newMultiThreadedExecutor(int, long, boolean, String)
     */
    public static ThreadPoolExecutor newMultiThreadedExecutor(
            int maxThreadCount, long threadKeepAliveTime, boolean isDaemon,
            String poolName) {

        ThreadPoolExecutor result;

        result = new ThreadPoolExecutor(maxThreadCount, maxThreadCount,
                threadKeepAliveTime, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory(isDaemon, poolName),
                REJECT_POLICY);

        result.allowCoreThreadTimeOut(true);

        return result;
    }

    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} which will use at most
     * the specified maximum thread count and daemon status. The executor will
     * have a default name and one second timeout before its core threads will
     * terminate if there are no tasks available. Note that every thread of the
     * executor will eventually terminate if no tasks are submitted to the
     * executor even if it was not shutted down (and will be restarted on
     * demand).
     *
     * @param maxThreadCount the maximum number of threads to be used by the
     *   returned executor. This argument must be greater than zero.
     * @param isDaemon the daemon status of the threads used by the returned
     *   executor. Note that the JVM will terminate if there are only daemon
     *   threads are remaining.
     * @return the newly created {@code ThreadPoolExecutor} preinitialized
     *   with the requested properties. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxThreadCount} is not
     *   greater than zero
     *
     * @see #newMultiThreadedExecutor(int, long, boolean, String)
     */
    public static ScheduledThreadPoolExecutor newSchedulerThreadedExecutor(
            int maxThreadCount, boolean isDaemon) {

        return newSchedulerThreadedExecutor(maxThreadCount, isDaemon, null);
    }

    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} which will use at most
     * the specified maximum thread count, daemon status and name. The executor
     * will have a one second timeout before its core threads will terminate if
     * there are no tasks available. Note that every thread of the executor will
     * eventually terminate if no tasks are submitted to the executor even if it
     * was not shutted down (and will be restarted on demand).
     *
     * @param maxThreadCount the maximum number of threads to be used by the
     *   returned executor. This argument must be greater than zero.
     * @param isDaemon the daemon status of the threads used by the returned
     *   executor. Note that the JVM will terminate if there are only daemon
     *   threads are remaining.
     * @param poolName the name of the returned executor useful for debugging
     *   purposes. This string will be part of the name of the threads created
     *   by the returned executor. This argument can be {@code null} in which
     *   case a default name will be given to this executor.
     * @return the newly created {@code ThreadPoolExecutor} preinitialized
     *   with the requested properties. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxThreadCount} is not
     *   greater than zero
     *
     * @see #newMultiThreadedExecutor(int, long, boolean, String)
     */
    public static ScheduledThreadPoolExecutor newSchedulerThreadedExecutor(
            int maxThreadCount, boolean isDaemon, String poolName) {

        return new ScheduledThreadPoolExecutor(maxThreadCount,
                new NamedThreadFactory(isDaemon, poolName),
                REJECT_POLICY);
    }

    /**
     * A {@code ThreadFactory} which creates thread with names containing a
     * specified string and a given daemon status and also belong to the same
     * thread group. Having a name for threads can be useful for debugging and
     * quickly identifying threads. Threads will by default have
     * {@code Thread.NORM_PRIORITY} as their priority.
     * <P>
     * This class is useful when manually creating
     * {@code java.util.concurrent.ThreadPoolExecutor} instances.
     *
     * <h3>Thread safety</h3>
     * Methods of this class are safe to use by multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     *
     * @see java.util.concurrent.ThreadPoolExecutor
     */
    public static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean isDaemon;

        private static String getDefaultName(boolean isDaemon) {
            return "generic " + (isDaemon ? "daemon " : "") + "pool-"
                    + POOL_NUMBER.getAndIncrement();
        }

        /**
         * Creates a new thread factory with the specified daemon status and
         * a default name.
         *
         * @param isDaemon the daemon status of the threads created by this
         *   thread factory
         */
        public NamedThreadFactory(boolean isDaemon) {
            this(isDaemon, getDefaultName(isDaemon));
        }

        /**
         * Creates a new thread factory with the specified daemon status and
         * name. The name will be included in the name of the threads created
         * by this thread factory.
         *
         * @param isDaemon the daemon status of the threads created by this
         *   thread factory
         * @param poolName the name of this thread factory and the string to
         *   be included in the name of the threads created by this thread
         *   factory. This argument can be {@code null} and in this case a
         *   default name will be used.
         */
        public NamedThreadFactory(boolean isDaemon, String poolName) {
            SecurityManager s = System.getSecurityManager();
            this.group = s != null
                    ? s.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
            this.namePrefix =
                    (poolName != null ? poolName : getDefaultName(isDaemon))
                    + "-thread-";
            this.isDaemon = isDaemon;

        }

        /**
         * Creates a new thread with the properties given in the constructor.
         * The thread will execute the specified task. Note that the returned
         * thread will not be started and needed to be started manually.
         * <P>
         * The returned thread will be initialized to have the
         * {@code Thread.NORM_PRIORITY} as its priority.
         *
         * @param r the task to be executed by the returned thread. Once the
         *   specified task returns, the returned thread will terminate. This
         *   argument can be {@code null}, in which case the returned thread
         *   does nothing and terminates immediately when executed.
         * @return the new thread with the properties given in the constructor.
         *   This method never returns {@code null}.
         */
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon() != isDaemon) {
                t.setDaemon(isDaemon);
            }

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private enum DiscardOnShutdownPolicy implements RejectedExecutionHandler {
        INSTANCE;

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                throw new RejectedExecutionException(
                        "Task cannot be executed.");
            }
        }
    }

    private ExecutorsEx() {
        throw new AssertionError();
    }
}
