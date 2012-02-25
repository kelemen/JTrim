/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Kelemen Attila
 */
public class ShutdownHelper {
    private ShutdownHelper() {
        throw new AssertionError();
    }

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

    public static void haltLater(final int status, final int msToWait) {
        if (msToWait == 0) {
            Runtime.getRuntime().halt(status);
        }

        startShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trySleep(msToWait);
                } finally {
                    Runtime.getRuntime().halt(status);
                }
            }
        }, status, true);
    }

    public static void exitLater(final int status, final int msToWait) {
        if (msToWait == 0) {
            System.exit(status);
        }

        startShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trySleep(msToWait);
                } finally {
                    System.exit(status);
                }
            }
        }, status, true);
    }

    public static void exitLater(final Runnable exitTask, final int status,
            final int msToWait) {

        startShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (exitTask != null) {
                        exitTask.run();
                    }
                } finally {
                    exitLater(status, msToWait);
                }
            }
        }, status, false);
    }

    public static void exit(final Runnable exitTask, final int status) {
        startShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (exitTask != null) {
                        exitTask.run();
                    }
                } finally {
                    System.exit(status);
                }
            }
        }, status, false);
    }

    public static void shutdownExecutors(ExecutorService... executors) {
        for (ExecutorService executor: executors) {
            executor.shutdown();
        }
    }

    public static List<Runnable> shutdownNowExecutors(ExecutorService... executors) {
        List<Runnable> result = new LinkedList<>();

        for (ExecutorService executor: executors) {
            result.addAll(executor.shutdownNow());
        }

        return result;
    }

    public static void awaitTerminateExecutorsSilently(
            ExecutorService... executors) {

        try {
            awaitTerminateExecutors(executors);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void awaitTerminateExecutorsSilently(
            long timeout, TimeUnit timeunit, ExecutorService... executors) {

        try {
            awaitTerminateExecutors(timeout, timeunit, executors);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void awaitTerminateExecutors(ExecutorService... executors)
            throws InterruptedException {

        for (ExecutorService executor: executors) {
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                // After waiting about 300 years we should interrupt ourselves
                // and not wait another century.
                throw new InterruptedException();
            }
        }
    }

    public static boolean awaitTerminateExecutors(long timeout, TimeUnit timeunit,
            ExecutorService... executors) throws InterruptedException {

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
}
