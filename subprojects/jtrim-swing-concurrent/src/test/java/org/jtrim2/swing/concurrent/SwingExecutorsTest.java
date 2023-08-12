package org.jtrim2.swing.concurrent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim2.access.AccessManager;
import org.jtrim2.access.AccessRequest;
import org.jtrim2.access.HierarchicalAccessManager;
import org.jtrim2.access.HierarchicalRight;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.ui.concurrent.BackgroundTaskExecutor;
import org.jtrim2.ui.concurrent.UiExecutorProvider;
import org.jtrim2.ui.concurrent.UiReporter;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public final class SwingExecutorsTest {
    public static class GenericTest extends GenericExecutorServiceTests {
        public GenericTest() {
            super(executorServices(Arrays.asList(
                    () -> SwingExecutors.getSwingExecutorService(false),
                    () -> SwingExecutors.getSwingExecutorService(true)
            )));
        }
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(SwingExecutors.class);
    }

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
        });
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
        }).whenComplete((result, error) -> {
            try {
                cleanup.run();
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
        }).whenComplete((result, error) -> {
            if (!(error instanceof TestException)) {
                errorRef.set(error);
                return;
            }

            try {
                cleanup.run();
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
        UiExecutorProvider provider = SwingExecutors.getSwingExecutorProvider();
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
                SwingExecutors.getSwingExecutorService(true),
                SwingExecutors.getSwingExecutorService(false));
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
                            SwingExecutors.getSwingExecutorProvider().getSimpleExecutor(false),
                            SwingExecutors.getSimpleExecutor(false),
                            SwingExecutors.getStrictExecutor(false),
                            SwingExecutors.getSwingExecutorService(false));

                    Runnable inContextCheck = () -> {
                        assertNotNull(inContext.get());
                    };
                    for (TaskExecutor executor: eagerExecutors) {
                        testGeneralTaskExecutor(executor, edtCheck(), inContextCheck);
                    }

                    List<TaskExecutor> lazyExecutors = Arrays.asList(
                            SwingExecutors.getSwingExecutorProvider().getSimpleExecutor(true),
                            SwingExecutors.getSimpleExecutor(true),
                            SwingExecutors.getStrictExecutor(true),
                            SwingExecutors.getSwingExecutorService(true));
                    for (TaskExecutor executor: lazyExecutors) {
                        CancelableTask task = mock(CancelableTask.class);
                        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
                        verifyNoInteractions(task);
                    }
                } finally {
                    inContext.remove();
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            throw new RuntimeException("Unexpected exception", ex);
        }
    }

    private static AccessManager<Object, HierarchicalRight> createManager() {
        return new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
    }

    private static AccessRequest<Object, HierarchicalRight> createRequest() {
        return AccessRequest.getWriteRequest(new Object(), HierarchicalRight.create(new Object()));
    }

    private static Runnable mockSwingTask(Runnable onWrongThread) {
        Runnable result = mock(Runnable.class);
        doAnswer((InvocationOnMock invocation) -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                onWrongThread.run();
            }
            return null;
        }).when(result).run();
        return result;
    }

    @Test(timeout = 20000)
    public void testSwingBackgroundTaskExecutor() throws Exception {
        // This is just a basic test verifying that the factory works fine.
        // The better tests are in the tests of BackgroundTaskExecutor.

        BackgroundTaskExecutor<Object, HierarchicalRight> bckgExecutor
                = SwingExecutors.getSwingBackgroundTaskExecutor(createManager(), SyncTaskExecutor.getSimpleExecutor());

        Runnable wrongThreadCallback = mock(Runnable.class);

        Runnable data1 = mockSwingTask(wrongThreadCallback);
        Runnable progress1 = mockSwingTask(wrongThreadCallback);

        bckgExecutor.tryExecute(createRequest(), (CancellationToken cancelToken, UiReporter reporter) -> {
            reporter.writeData(data1);
            reporter.updateProgress(progress1);
        });

        SwingUtilities.invokeAndWait(() -> {
            verify(data1).run();
            verify(progress1).run();

            verifyNoInteractions(wrongThreadCallback);
        });
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
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, tasks[taskIndex]);
                doneSignals[taskIndex].signal();
            };
            if (taskIndex == 0) {
                taskThreads[i] = () -> {
                    SwingUtilities.invokeLater(waitPrevAndSchedule);
                };
            } else {
                taskThreads[i] = waitPrevAndSchedule;
            }
        }

        Tasks.runConcurrently(taskThreads);

        for (WaitableSignal doneSignal: doneSignals) {
            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        }

        final WaitableSignal completeSignal = new WaitableSignal();
        executor.execute(completeSignal::signal);
        completeSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

        InOrder inOrder = inOrder((Object[]) tasks);
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
            SwingExecutors.getSwingUpdateExecutor(),
            SwingExecutors.getSwingUpdateExecutor(false),
            SwingExecutors.getSwingUpdateExecutor(true)
        };
    }

    private static UpdateTaskExecutor[] lazyUpdateExecutors() {
        return new UpdateTaskExecutor[]{
            SwingExecutors.getSwingUpdateExecutor(),
            SwingExecutors.getSwingUpdateExecutor(true)
        };
    }

    private static UpdateTaskExecutor[] eagerUpdateExecutors() {
        return new UpdateTaskExecutor[]{
            SwingExecutors.getSwingUpdateExecutor(false)
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
                verifyNoInteractions(firstTask);
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

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -7739627449902612689L;
    }
}
