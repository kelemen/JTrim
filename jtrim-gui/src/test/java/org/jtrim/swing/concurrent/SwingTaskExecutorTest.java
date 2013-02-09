package org.jtrim.swing.concurrent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class SwingTaskExecutorTest {
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

    private static void testExecuteTask(TaskExecutor executor, final Runnable... checks) {
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
        final Runnable task = mock(Runnable.class);
        final WaitableSignal doneSignal = new WaitableSignal();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                try {
                    for (Runnable check: checks) {
                        check.run();
                    }
                    task.run();
                } catch (Throwable ex) {
                    errorRef.set(ex);
                } finally {
                    doneSignal.signal();
                }
            }
        }, null);
        doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        Throwable error = errorRef.get();
        if (error != null) {
            ExceptionHelper.rethrow(error);
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    private static void testExecuteTaskWithCleanup(
            TaskExecutor executor,
            final Runnable... checks) {
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        final Runnable task = mock(Runnable.class);
        final Runnable cleanup = mock(Runnable.class);

        final WaitableSignal doneSignal1 = new WaitableSignal();
        final WaitableSignal doneSignal2 = new WaitableSignal();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                try {
                    for (Runnable check: checks) {
                        check.run();
                    }
                    task.run();
                } catch (Throwable ex) {
                    errorRef.set(ex);
                } finally {
                    doneSignal1.signal();
                }
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                try {
                    for (Runnable check: checks) {
                        check.run();
                    }
                    cleanup.run();
                } catch (Throwable ex) {
                    errorRef.set(ex);
                } finally {
                    doneSignal2.signal();
                }
            }
        });
        doneSignal1.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        doneSignal2.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        Throwable error = errorRef.get();
        if (error != null) {
            ExceptionHelper.rethrow(error);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).run();
        inOrder.verify(cleanup).run();
        inOrder.verifyNoMoreInteractions();
    }

    private static void testExecuteTaskWithCleanupAndError(
            TaskExecutor executor,
            final Runnable... checks) {
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        final Runnable task = mock(Runnable.class);
        final Runnable cleanup = mock(Runnable.class);

        final WaitableSignal doneSignal1 = new WaitableSignal();
        final WaitableSignal doneSignal2 = new WaitableSignal();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                try {
                    for (Runnable check: checks) {
                        check.run();
                    }
                    task.run();
                } catch (Throwable ex) {
                    errorRef.set(ex);
                } finally {
                    doneSignal1.signal();
                }
                throw new TestException();
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                if (!(error instanceof TestException)) {
                    errorRef.set(error);
                    return;
                }

                try {
                    for (Runnable check: checks) {
                        check.run();
                    }
                    cleanup.run();
                } catch (Throwable ex) {
                    errorRef.set(ex);
                } finally {
                    doneSignal2.signal();
                }
            }
        });
        doneSignal1.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        doneSignal2.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        Throwable error = errorRef.get();
        if (error != null) {
            ExceptionHelper.rethrow(error);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).run();
        inOrder.verify(cleanup).run();
        inOrder.verifyNoMoreInteractions();
    }

    private static Runnable edtCheck() {
        return new Runnable() {
            @Override
            public void run() {
                assertTrue(SwingUtilities.isEventDispatchThread());
            }
        };
    }

    private static void testGeneralTaskExecutor(TaskExecutor executor, Runnable... checks) {
        testExecuteTask(executor, checks);
        testExecuteTaskWithCleanup(executor, checks);
        testExecuteTaskWithCleanupAndError(executor, checks);
    }

    private static List<TaskExecutor> simpleExecutors() {
        return Arrays.asList(
                SwingTaskExecutor.getSimpleExecutor(true),
                SwingTaskExecutor.getSimpleExecutor(false),
                SwingTaskExecutor.getStrictExecutor(true),
                SwingTaskExecutor.getStrictExecutor(false));
    }

    private static List<TaskExecutorService> allExecutorServices() {
        return Arrays.asList(
                SwingTaskExecutor.getDefaultInstance(),
                new SwingTaskExecutor(),
                new SwingTaskExecutor(true),
                new SwingTaskExecutor(false));
    }

    private static List<TaskExecutor> allExecutors() {
        List<TaskExecutor> executors = new ArrayList<>();
        executors.addAll(simpleExecutors());
        executors.addAll(allExecutorServices());
        return executors;
    }

    @Test(timeout = 20000)
    public void testTaskExecutorFromEDT() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final ThreadLocal<Object> inContext = new ThreadLocal<>();
                    inContext.set(Boolean.TRUE);
                    try {
                        List<TaskExecutor> eagerExecutors = Arrays.asList(
                                SwingTaskExecutor.getSimpleExecutor(false),
                                SwingTaskExecutor.getStrictExecutor(false),
                                new SwingTaskExecutor(false));

                        Runnable inContextCheck = new Runnable() {
                            @Override
                            public void run() {
                                assertNotNull(inContext.get());
                            }
                        };
                        for (TaskExecutor executor: eagerExecutors) {
                            testGeneralTaskExecutor(executor, edtCheck(), inContextCheck);
                        }

                        List<TaskExecutor> lazyExecutors = Arrays.asList(
                                SwingTaskExecutor.getSimpleExecutor(true),
                                SwingTaskExecutor.getStrictExecutor(true),
                                new SwingTaskExecutor(),
                                new SwingTaskExecutor(true));
                        for (TaskExecutor executor: lazyExecutors) {
                            CancelableTask task = mock(CancelableTask.class);
                            CleanupTask cleanup = mock(CleanupTask.class);
                            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
                            verifyZeroInteractions(task, cleanup);
                        }
                    } finally {
                        inContext.remove();
                    }
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            throw new RuntimeException("Unexpected exception", ex);
        }
    }

    @Test(timeout = 20000)
    public void testGeneralTaskExecutor() {
        for (TaskExecutor executor: allExecutors()) {
            testGeneralTaskExecutor(executor, edtCheck());
        }
    }

    private static void testStrictness(final TaskExecutor executor, int requestedNumberOfThreads) throws Exception {
        int numberOfThreads = Math.max(3, requestedNumberOfThreads);

        final CancelableTask[] tasks = new CancelableTask[numberOfThreads];
        final WaitableSignal[] doneSignals = new WaitableSignal[numberOfThreads];
        final Runnable[] taskThreads = new Runnable[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            tasks[i] = mock(CancelableTask.class);
            doneSignals[i] = new WaitableSignal();

            final int taskIndex = i;
            final Runnable waitPrevAndSchedule = new Runnable() {
                @Override
                public void run() {
                    if (taskIndex > 0) {
                        doneSignals[taskIndex - 1].waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                    }
                    executor.execute(Cancellation.UNCANCELABLE_TOKEN, tasks[taskIndex], null);
                    doneSignals[taskIndex].signal();
                }
            };
            if (taskIndex == 0) {
                taskThreads[i] = new Runnable() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(waitPrevAndSchedule);
                    }
                };
            }
            else {
                taskThreads[i] = waitPrevAndSchedule;
            }
        }

        Tasks.runConcurrently(taskThreads);

        for (WaitableSignal doneSignal: doneSignals) {
            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        }

        final WaitableSignal completeSignal = new WaitableSignal();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                completeSignal.signal();
            }
        }, null);
        completeSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        InOrder inOrder = inOrder((Object[])tasks);
        for (CancelableTask task: tasks) {
            inOrder.verify(task).execute(any(CancellationToken.class));
        }
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testStrictness1() throws Exception {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        TaskExecutor executor = SwingTaskExecutor.getStrictExecutor(false);
        for (int i = 0; i < 500; i++) {
            testStrictness(executor, numberOfThreads);
        }
    }

    @Test(timeout = 30000)
    public void testStrictness2() throws Exception {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        TaskExecutor executor = SwingTaskExecutor.getStrictExecutor(true);
        for (int i = 0; i < 500; i++) {
            testStrictness(executor, numberOfThreads);
        }
    }

    @Test(timeout = 20000)
    public void testIllegalShutdown1() throws Exception {
        TaskExecutorService executor = SwingTaskExecutor.getDefaultInstance();

        try {
            executor.shutdown();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException ex) {
        }

        testGeneralTaskExecutor(executor, edtCheck());
    }

    @Test(timeout = 20000)
    public void testIllegalShutdown2() throws Exception {
        TaskExecutorService executor = SwingTaskExecutor.getDefaultInstance();

        try {
            executor.shutdownAndCancel();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException ex) {
        }

        testGeneralTaskExecutor(executor, edtCheck());
    }

    @Test(timeout = 5000)
    public void testIllegalEDT1() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    SwingTaskExecutor executor = new SwingTaskExecutor(true);
                    executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
                    fail("Expected: IllegalStateException");
                } catch (IllegalStateException ex) {
                }
            }
        });
    }

    @Test(timeout = 5000)
    public void testIllegalEDT2() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    SwingTaskExecutor executor = new SwingTaskExecutor(false);
                    executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
                    fail("Expected: IllegalStateException");
                } catch (IllegalStateException ex) {
                }
            }
        });
    }

    @Test(timeout = 5000)
    public void testIllegalEDT3() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    SwingTaskExecutor executor = new SwingTaskExecutor(true);
                    executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
                    fail("Expected: IllegalStateException");
                } catch (IllegalStateException ex) {
                }
            }
        });
    }

    @Test(timeout = 5000)
    public void testIllegalEDT4() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    SwingTaskExecutor executor = new SwingTaskExecutor(false);
                    executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
                    fail("Expected: IllegalStateException");
                } catch (IllegalStateException ex) {
                }
            }
        });
    }

    @Test(timeout = 5000)
    public void testAwaitTermination() {
        for (boolean lazy: Arrays.asList(false, true)) {
            SwingTaskExecutor executor = new SwingTaskExecutor(lazy);
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test(timeout = 5000)
    public void testTryAwaitTermination1() {
        for (boolean lazy: Arrays.asList(false, true)) {
            SwingTaskExecutor executor = new SwingTaskExecutor(lazy);
            executor.shutdown();
            assertTrue(executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
        }
    }

    @Test(timeout = 5000)
    public void testTryAwaitTermination2() {
        for (boolean lazy: Arrays.asList(false, true)) {
            SwingTaskExecutor executor = new SwingTaskExecutor(lazy);
            assertFalse(executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 1, TimeUnit.NANOSECONDS));
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -7739627449902612689L;
    }
}