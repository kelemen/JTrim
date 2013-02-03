package org.jtrim.access;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class RequestGrabberTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSimple() {
        AccessManager<Object, HierarchicalRight> manager = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        HierarchicalRight right = HierarchicalRight.create(new Object());
        AccessRequest<String, HierarchicalRight> request = AccessRequest.getWriteRequest("REQUEST", right);

        RequestGrabber grabber = new RequestGrabber(manager, request);
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.acquire();
        assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.release();
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.acquire();
        assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));
        grabber.acquire();
        assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.release();
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));
    }

    @Test(timeout = 20000)
    public void testConcurrent() {
        for (int testIndex = 0; testIndex < 100; testIndex++) {
            AccessManager<Object, HierarchicalRight> manager = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
            HierarchicalRight right = HierarchicalRight.create(new Object());
            AccessRequest<String, HierarchicalRight> request = AccessRequest.getWriteRequest("REQUEST", right);

            final RequestGrabber grabber = new RequestGrabber(manager, request);

            Runnable[] tasks = new Runnable[2 * Runtime.getRuntime().availableProcessors()];
            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = new Runnable() {
                    @Override
                    public void run() {
                        grabber.acquire();
                    }
                };
            }
            runConcurrently(tasks);

            assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

            grabber.release();
            assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));
        }
    }

    private static void runConcurrently(Runnable... tasks) {
        final CountDownLatch latch = new CountDownLatch(tasks.length);
        Thread[] threads = new Thread[tasks.length];
        final Throwable[] exceptions = new Throwable[tasks.length];

        for (int i = 0; i < threads.length; i++) {
            final Runnable task = tasks[i];
            final int threadIndex = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.countDown();
                        latch.await();

                        task.run();
                    } catch (Throwable ex) {
                        exceptions[threadIndex] = ex;
                    }
                }
            });
        }

        try {
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
        } finally {
            boolean interrupted = false;
            for (int i = 0; i < threads.length; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }

            if (interrupted) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unexpected interrupt.");
            }
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
}