package org.jtrim2.swing.concurrent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CleanupTask;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.ui.concurrent.UiExecutorProvider;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public final class SwingExecutorsTest {
    private static void testExecuteTask(TaskExecutor executor, final Runnable... checks) {
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
        final Runnable task = mock(Runnable.class);
        final WaitableSignal doneSignal = new WaitableSignal();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
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
        }, null);
        doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        ExceptionHelper.rethrowIfNotNull(errorRef.get());

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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
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
        }, (boolean canceled, Throwable error) -> {
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
        });
        doneSignal1.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        doneSignal2.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        ExceptionHelper.rethrowIfNotNull(errorRef.get());

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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
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
        }, (boolean canceled, Throwable error) -> {
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
        });
        doneSignal1.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        doneSignal2.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        ExceptionHelper.rethrowIfNotNull(errorRef.get());

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).run();
        inOrder.verify(cleanup).run();
        inOrder.verifyNoMoreInteractions();
    }

    private static Runnable edtCheck() {
        return () -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
        };
    }

    private static void testGeneralTaskExecutor(TaskExecutor executor, Runnable... checks) {
        testExecuteTask(executor, checks);
        testExecuteTaskWithCleanup(executor, checks);
        testExecuteTaskWithCleanupAndError(executor, checks);
    }

    private static List<TaskExecutor> simpleExecutors() {
        UiExecutorProvider provider = SwingExecutors.swingExecutorProvider();
        return Arrays.asList(
                provider.getSimpleExecutor(true),
                provider.getSimpleExecutor(false),
                provider.getStrictExecutor(true),
                provider.getStrictExecutor(false),
                SwingExecutors.getSimpleExecutor(true),
                SwingExecutors.getSimpleExecutor(false),
                SwingExecutors.getStrictExecutor(true),
                SwingExecutors.getStrictExecutor(false));
    }

    private static List<TaskExecutorService> allExecutorServices() {
        return Arrays.asList(
                SwingExecutors.getDefaultInstance(),
                SwingExecutors.swingExecutorService(true),
                SwingExecutors.swingExecutorService(false));
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
            SwingUtilities.invokeAndWait(() -> {
                final ThreadLocal<Object> inContext = new ThreadLocal<>();
                inContext.set(Boolean.TRUE);
                try {
                    List<TaskExecutor> eagerExecutors = Arrays.asList(
                            SwingExecutors.swingExecutorProvider().getSimpleExecutor(false),
                            SwingExecutors.getSimpleExecutor(false),
                            SwingExecutors.getStrictExecutor(false),
                            SwingExecutors.swingExecutorService(false));

                    Runnable inContextCheck = () -> {
                        assertNotNull(inContext.get());
                    };
                    for (TaskExecutor executor: eagerExecutors) {
                        testGeneralTaskExecutor(executor, edtCheck(), inContextCheck);
                    }

                    List<TaskExecutor> lazyExecutors = Arrays.asList(
                            SwingExecutors.swingExecutorProvider().getSimpleExecutor(true),
                            SwingExecutors.getSimpleExecutor(true),
                            SwingExecutors.getStrictExecutor(true),
                            SwingExecutors.swingExecutorService(true));
                    for (TaskExecutor executor: lazyExecutors) {
                        CancelableTask task = mock(CancelableTask.class);
                        CleanupTask cleanup = mock(CleanupTask.class);
                        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
                        verifyZeroInteractions(task, cleanup);
                    }
                } finally {
                    inContext.remove();
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

    private static void testStrictness(
            final TaskExecutor executor,
            int requestedNumberOfThreads) throws Exception {

        int numberOfThreads = Math.max(3, requestedNumberOfThreads);

        final CancelableTask[] tasks = new CancelableTask[numberOfThreads];
        final WaitableSignal[] doneSignals = new WaitableSignal[numberOfThreads];
        final Runnable[] taskThreads = new Runnable[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            tasks[i] = mock(CancelableTask.class);
            doneSignals[i] = new WaitableSignal();

            final int taskIndex = i;
            final Runnable waitPrevAndSchedule = () -> {
                if (taskIndex > 0) {
                    doneSignals[taskIndex - 1].waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                }
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, tasks[taskIndex], null);
                doneSignals[taskIndex].signal();
            };
            if (taskIndex == 0) {
                taskThreads[i] = () -> {
                    SwingUtilities.invokeLater(waitPrevAndSchedule);
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            completeSignal.signal();
        }, null);
        completeSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        InOrder inOrder = inOrder((Object[])tasks);
        for (CancelableTask task: tasks) {
            inOrder.verify(task).execute(any(CancellationToken.class));
        }
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 60000)
    public void testStrictness1() throws Exception {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        TaskExecutor executor = SwingExecutors.getStrictExecutor(false);
        for (int i = 0; i < 100; i++) {
            testStrictness(executor, numberOfThreads);
        }
    }

    @Test(timeout = 60000)
    public void testStrictness2() throws Exception {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        TaskExecutor executor = SwingExecutors.getStrictExecutor(true);
        for (int i = 0; i < 100; i++) {
            testStrictness(executor, numberOfThreads);
        }
    }

    @Test(timeout = 20000)
    public void testIllegalShutdown1() throws Exception {
        TaskExecutorService executor = SwingExecutors.getDefaultInstance();

        try {
            executor.shutdown();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException ex) {
        }

        testGeneralTaskExecutor(executor, edtCheck());
    }

    @Test(timeout = 20000)
    public void testIllegalShutdown2() throws Exception {
        TaskExecutorService executor = SwingExecutors.getDefaultInstance();

        try {
            executor.shutdownAndCancel();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException ex) {
        }

        testGeneralTaskExecutor(executor, edtCheck());
    }

    private static UpdateTaskExecutor[] allUpdateExecutors() {
        return new UpdateTaskExecutor[]{
            SwingExecutors.newSwingUpdateExecutor(),
            SwingExecutors.newSwingUpdateExecutor(false),
            SwingExecutors.newSwingUpdateExecutor(true)
        };
    }

    private static UpdateTaskExecutor[] lazyUpdateExecutors() {
        return new UpdateTaskExecutor[]{
            SwingExecutors.newSwingUpdateExecutor(),
            SwingExecutors.newSwingUpdateExecutor(true)
        };
    }

    private static UpdateTaskExecutor[] eagerUpdateExecutors() {
        return new UpdateTaskExecutor[]{
            SwingExecutors.newSwingUpdateExecutor(false)
        };
    }

    @Test
    public void testExecuteLazyFromEdt() throws Exception {
        for (final UpdateTaskExecutor executor: lazyUpdateExecutors()) {
            final Runnable firstTask = mock(Runnable.class);
            final Runnable lastTask = mock(Runnable.class);

            SwingUtilities.invokeAndWait(() -> {
                executor.execute(firstTask);
                executor.execute(lastTask);
            });

            SwingUtilities.invokeAndWait(() -> {
                verifyZeroInteractions(firstTask);
                verify(lastTask).run();
            });
        }
    }

    @Test
    public void testExecuteEagerFromEdt() throws Exception {
        for (final UpdateTaskExecutor executor: eagerUpdateExecutors()) {
            SwingUtilities.invokeAndWait(() -> {
                Runnable firstTask = mock(Runnable.class);
                Runnable lastTask = mock(Runnable.class);

                executor.execute(firstTask);
                verify(firstTask).run();

                executor.execute(lastTask);
                verify(lastTask).run();
            });
        }
    }

    @Test
    public void testExecuteAfterShutdown() throws Exception {
        for (final UpdateTaskExecutor executor: allUpdateExecutors()) {
            final Runnable task = mock(Runnable.class);
            executor.shutdown();
            executor.execute(task);

            SwingUtilities.invokeAndWait(() -> {
                verifyZeroInteractions(task);
            });
        }
    }

    @Test
    public void testExecuteAfterShutdownFromEdt() throws Exception {
        for (final UpdateTaskExecutor executor: allUpdateExecutors()) {
            final Runnable task = mock(Runnable.class);
            SwingUtilities.invokeAndWait(() -> {
                executor.shutdown();
                executor.execute(task);
            });

            SwingUtilities.invokeAndWait(() -> {
                verifyZeroInteractions(task);
            });
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -7739627449902612689L;
    }
}
