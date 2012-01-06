/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Kelemen Attila
 */
public final class ExecutorsEx {
    private static final RejectedExecutionHandler REJECT_POLICY
            = DiscardOnShutdownPolicy.INSTANCE;

    private static final long DEFAULT_THREAD_KEEPALIVE_TIME = 1000;

    private ExecutorsEx() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Future<T> canceledFuture() {
        // This cast is safe because the future will never return a result
        // therefore there is no result of which type can be observed.
        return (Future<T>)CanceledFuture.INSTANCE;
    }

    public static void shutdownExecutorsNow(ExecutorService... executors) {
        shutdownExecutorsNow(Arrays.asList(executors));
    }

    public static void shutdownExecutorsNow(Collection<? extends ExecutorService> executors) {
        for (ExecutorService executor: executors) {
            executor.shutdownNow();
        }
    }

    public static void awaitExecutor(ExecutorService executor)
            throws InterruptedException {
        while (!executor.isTerminated()) {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    public static void awaitExecutors(ExecutorService... executors)
            throws InterruptedException {
        awaitExecutors(Arrays.asList(executors));
    }

    public static void awaitExecutors(Collection<? extends ExecutorService> executors)
            throws InterruptedException {
        for (ExecutorService executor: executors) {
            awaitExecutor(executor);
        }
    }

    public static boolean awaitExecutors(long timeout, TimeUnit timeunit,
            ExecutorService... executors)
            throws InterruptedException {
        return awaitExecutors(timeout, timeunit, Arrays.asList(executors));
    }

    public static boolean awaitExecutors(long timeout, TimeUnit timeunit,
            Collection<? extends ExecutorService> executors)
            throws InterruptedException {

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

    public static ExecutorService asUnstoppableExecutor(ExecutorService executor) {
        return new UnstoppableExecutor(executor);
    }

    public static ThreadPoolExecutor newMultiThreadedExecutor(int maxThreadCount,
            boolean isDaemon) {

        return newMultiThreadedExecutor(maxThreadCount,
                DEFAULT_THREAD_KEEPALIVE_TIME, isDaemon, null);
    }

    public static ThreadPoolExecutor newMultiThreadedExecutor(int maxThreadCount,
            boolean isDaemon, String poolName) {

        return newMultiThreadedExecutor(maxThreadCount,
                DEFAULT_THREAD_KEEPALIVE_TIME, isDaemon, poolName);
    }

    public static ThreadPoolExecutor newMultiThreadedExecutor(int maxThreadCount,
            long threadKeepAliveTime, boolean isDaemon) {

        return newMultiThreadedExecutor(maxThreadCount, threadKeepAliveTime,
                isDaemon, null);
    }

    public static ThreadPoolExecutor newMultiThreadedExecutor(int maxThreadCount,
            long threadKeepAliveTime, boolean isDaemon, String poolName) {

        ThreadPoolExecutor result;

        result = new ThreadPoolExecutor(maxThreadCount, maxThreadCount,
                threadKeepAliveTime, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory(isDaemon, poolName),
                REJECT_POLICY);

        result.allowCoreThreadTimeOut(true);

        return result;
    }

    public static ScheduledThreadPoolExecutor newSchedulerThreadedExecutor(
            int maxThreadCount, boolean isDaemon) {

        return newSchedulerThreadedExecutor(maxThreadCount, isDaemon, null);
    }

    public static ScheduledThreadPoolExecutor newSchedulerThreadedExecutor(
            int maxThreadCount, boolean isDaemon, String poolName) {

        ScheduledThreadPoolExecutor result;

        result = new ScheduledThreadPoolExecutor(maxThreadCount,
                new NamedThreadFactory(isDaemon, poolName),
                REJECT_POLICY);

        return result;
    }

    public static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean isDaemon;

        private static String getDefaultName(boolean isDaemon) {
            return "generic " + (isDaemon ? "daemon " : "") + "pool-"
                    + poolNumber.getAndIncrement();
        }

        public NamedThreadFactory(boolean isDaemon) {
            this(isDaemon, getDefaultName(isDaemon));
        }

        public NamedThreadFactory(boolean isDaemon, String poolName) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            this.namePrefix = (poolName != null ? poolName : getDefaultName(isDaemon))
                    + "-thread-";
            this.isDaemon = isDaemon;

        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon() != isDaemon)
                t.setDaemon(isDaemon);

            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private enum DiscardOnShutdownPolicy implements RejectedExecutionHandler {
        INSTANCE;

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                throw new RejectedExecutionException("Task cannot be executed.");
            }
        }
    }
}
