package org.jtrim2.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.TaskExecutionException;
import org.jtrim2.concurrent.TaskState;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.UnregisteredListenerRef;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.LogCollector;
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
public class AbstractTaskExecutorServiceTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testWithoutUserDefinedCleanup1() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableTask task = mock(CancelableTask.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(Runnable.class),
                eq(false));
    }

    @Test
    public void testWithoutUserDefinedCleanup2() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableTask task = mock(CancelableTask.class);

        executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(Runnable.class),
                eq(false));
    }

    @Test
    public void testWithoutUserDefinedCleanup3() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableFunction<?> task = mock(CancelableFunction.class);

        executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(Runnable.class),
                eq(false));
    }

    @Test
    public void testWithUserDefinedCleanup1() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(Runnable.class),
                eq(true));
    }

    @Test
    public void testWithUserDefinedCleanup2() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(Runnable.class),
                eq(true));
    }

    @Test
    public void testWithUserDefinedCleanup3() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableFunction<?> task = mock(CancelableFunction.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(Runnable.class),
                eq(true));
    }

    @Test
    public void testExecuteNoCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            assertFalse(cancelToken.isCanceled());
            executeCount.incrementAndGet();
        }, null);

        executor.executeSubmittedTasks();
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
    }

    @Test
    public void testExecuteExceptionTaskWithoutCleanup() throws Exception {
        ManualExecutorService executor = new ManualExecutorService();

        CancelableTask task = mock(CancelableTask.class);
        doThrow(new TestException()).when(task).execute(any(CancellationToken.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.executeSubmittedTasks();
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
    }

    @Test
    public void testExecuteCanceledExceptionTaskWithoutCleanup() throws Exception {
        ManualExecutorService executor = new ManualExecutorService();

        CancelableTask task = mock(CancelableTask.class);
        doThrow(new OperationCanceledException()).when(task).execute(any(CancellationToken.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.executeSubmittedTasks();
            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    @Test
    public void testExecuteWithCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final AtomicInteger cleanupCount = new AtomicInteger(0);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            assertFalse(cancelToken.isCanceled());
            assertEquals("Task was executed after cleanup", 0, cleanupCount.intValue());
            executeCount.incrementAndGet();
        }, (boolean canceled, Throwable error) -> {
            assertFalse(canceled);
            assertNull(error);
            assertEquals("Cleanup was executed before task", 1, executeCount.intValue());
            cleanupCount.incrementAndGet();
        });

        executor.executeSubmittedTasks();
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        assertEquals("Unexpected number of cleanup", 1, cleanupCount.intValue());
    }

    @Test
    public void testSubmitNoCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            assertFalse(cancelToken.isCanceled());
            executeCount.incrementAndGet();
        }, null);

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertNull(future.tryGetResult());
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitWithCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final AtomicInteger cleanupCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            assertFalse(cancelToken.isCanceled());
            assertEquals("Task was executed after cleanup", 0, cleanupCount.intValue());
            executeCount.incrementAndGet();
        }, (boolean canceled, Throwable error) -> {
            assertFalse(canceled);
            assertNull(error);
            assertEquals("Cleanup was executed before task", 1, executeCount.intValue());
            cleanupCount.incrementAndGet();
        });

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertNull(future.tryGetResult());
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        assertEquals("Unexpected number of cleanup", 1, cleanupCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitFunctionNoCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final Object taskResult = "TASK-RESULT";

        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            assertFalse(cancelToken.isCanceled());
            executeCount.incrementAndGet();
            return taskResult;
        }, null);

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertSame(taskResult, future.tryGetResult());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitFunctionWithCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final AtomicInteger cleanupCount = new AtomicInteger(0);
        final Object taskResult = "TASK-RESULT";

        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            assertFalse(cancelToken.isCanceled());
            assertEquals("Task was executed after cleanup", 0, cleanupCount.intValue());
            executeCount.incrementAndGet();
            return taskResult;
        }, (boolean canceled, Throwable error) -> {
            assertFalse(canceled);
            assertNull(error);
            assertEquals("Cleanup was executed before task", 1, executeCount.intValue());
            cleanupCount.incrementAndGet();
        });

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertSame(taskResult, future.tryGetResult());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        assertEquals("Unexpected number of cleanup", 1, cleanupCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testCanceledSubmit() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.CANCELED_TOKEN, (CancellationToken cancelToken) -> {
            executeCount.incrementAndGet();
        }, null);

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Unexpected number of execution", 0, executeCount.intValue());
    }

    @Test(expected = OperationCanceledException.class)
    public void testCanceledSubmitFuture() {
        ManualExecutorService executor = new ManualExecutorService();

        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.CANCELED_TOKEN, (CancellationToken cancelToken) -> {
            executeCount.incrementAndGet();
        }, null);

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        future.tryGetResult();
    }

    @Test
    public void testPostSubmitCanceledSubmit() {
        ManualExecutorService executor = new ManualExecutorService();

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(cancelSource.getToken(), (CancellationToken cancelToken) -> {
            executeCount.incrementAndGet();
        }, null);
        cancelSource.getController().cancel();

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Unexpected number of execution", 0, executeCount.intValue());
    }

    @Test(expected = TaskExecutionException.class)
    public void testSubmitError() {
        ManualExecutorService executor = new ManualExecutorService();

        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            throw new UnsupportedOperationException();
        }, null);

        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_ERROR, future.getTaskState());

        try {
            future.tryGetResult();
        } catch (TaskExecutionException ex) {
            assertEquals(UnsupportedOperationException.class, ex.getCause().getClass());
            throw ex;
        }
    }

    @Test
    public void testUnregisterListener() {
        ManualExecutorService executor = new ManualExecutorService();

        RegCounterCancelToken cancelToken = new RegCounterCancelToken();
        TaskFuture<?> future = executor.submit(cancelToken, (CancellationToken taskCancelToken) -> {
        }, null);

        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertEquals("Remaining registered listener.",
                0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListenerPreCancel() {
        ManualExecutorService executor = new ManualExecutorService();

        RegCounterCancelToken cancelToken = new RegCounterCancelToken(
                Cancellation.CANCELED_TOKEN);

        TaskFuture<?> future = executor.submit(cancelToken, (CancellationToken taskCancelToken) -> {
        }, null);

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Remaining registered listener.",
                0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListenerPostCancel() {
        ManualExecutorService executor = new ManualExecutorService();

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        RegCounterCancelToken cancelToken = new RegCounterCancelToken(
                cancelSource.getToken());

        TaskFuture<?> future = executor.submit(cancelToken, (CancellationToken taskCancelToken) -> {
        }, null);
        cancelSource.getController().cancel();

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Remaining registered listener.",
                0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testAwaitTerminate() {
        ManualExecutorService executor = new ManualExecutorService();
        executor.shutdown();
        executor.awaitTermination(Cancellation.CANCELED_TOKEN);
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitAfterShutdownWithCleanup() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        executor.shutdown();

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);
        executor.executeSubmittedTasks();

        verifyZeroInteractions(task);
        verify(cleanupTask).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(cleanupTask);
    }

    @Test
    public void testSubmitAfterShutdownWithCleanupGetState() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        executor.shutdown();

        TaskFuture<?> taskState = executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);
        assertEquals(TaskState.DONE_CANCELED, taskState.getTaskState());
        executor.executeSubmittedTasks();

        verifyZeroInteractions(task);
        verify(cleanupTask).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(cleanupTask);
    }

    @Test(timeout = 5000)
    public void testTryGetResultOfNotExecutedTask() throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        Object expectedResult = "RESULT-OF-CancelableFunction";
        stub(task.execute(any(CancellationToken.class))).toReturn(expectedResult);

        ManualExecutorService executor = new ManualExecutorService();
        TaskFuture<?> taskState = executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);
        assertNull(taskState.tryGetResult());
    }

    // testSimpleGetResult is added only to ensure, that waitAndGet does not
    // throw an OperationCanceledException when the submitted task has not been
    // canceled. This is needed because the subsequent tests check if waitAndGet
    // throws OperationCanceledException when the task is canceled.

    @Test(timeout = 5000)
    public void testSimpleGetResult() throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        Object expectedResult = "RESULT-OF-CancelableFunction";
        stub(task.execute(any(CancellationToken.class))).toReturn(expectedResult);

        ManualExecutorService executor = new ManualExecutorService();
        TaskFuture<?> taskState = executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);
        executor.executeSubmittedTasks();

        Object result1 = taskState.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
        assertEquals(expectedResult, result1);

        Object result2 = taskState.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
        assertEquals(expectedResult, result2);
    }

    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testTimeoutWaitResult() throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        TaskFuture<?> taskState = executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);
        taskState.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = OperationCanceledException.class)
    public void testResultOfCanceledTask() throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskFuture<?> taskState = executor.submit(cancelSource.getToken(), task, cleanupTask);
        cancelSource.getController().cancel();
        executor.executeSubmittedTasks();
        taskState.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(expected = OperationCanceledException.class)
    public void testResultOfCanceledTaskWithTimeout() throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskFuture<?> taskState = executor.submit(cancelSource.getToken(), task, cleanupTask);
        cancelSource.getController().cancel();
        executor.executeSubmittedTasks();
        taskState.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @Test
    public void testExecuteCanceledTask() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        doThrow(OperationCanceledException.class)
                .when(task).execute(any(CancellationToken.class));

        ManualExecutorService executor = new ManualExecutorService();
        TaskFuture<?> taskState = executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);
        assertEquals(TaskState.NOT_STARTED, taskState.getTaskState());

        executor.executeSubmittedTasks();
        assertEquals(TaskState.DONE_CANCELED, taskState.getTaskState());

        verify(task).execute(any(CancellationToken.class));
        verify(cleanupTask).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(task, cleanupTask);
    }

    @Test
    public void testExecuteAfterFailedCleanup() throws Exception {
        CancelableTask task1 = mock(CancelableTask.class);
        CleanupTask cleanupTask1 = mock(CleanupTask.class);

        doThrow(new TestException())
                .when(cleanupTask1)
                .cleanup(anyBoolean(), any(Throwable.class));

        CancelableTask task2 = mock(CancelableTask.class);
        CleanupTask cleanupTask2 = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, cleanupTask1);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, cleanupTask2);

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.executeSubmittedTasks();

            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task1).execute(any(CancellationToken.class));
        verify(cleanupTask1).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(task1, cleanupTask1);

        verify(task2).execute(any(CancellationToken.class));
        verify(cleanupTask2).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(task2, cleanupTask2);
    }

    @Test(expected = IllegalStateException.class)
    public void testMisuseMultipleExecute() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanupTask);

        executor.executeSubmittedTasksWithoutRemoving();
        executor.executeSubmittedTasksWithoutRemoving();
    }

    private static class RegCounterCancelToken implements CancellationToken {
        private final AtomicLong regCounter;
        private final CancellationToken wrappedToken;

        public RegCounterCancelToken() {
            this(Cancellation.UNCANCELABLE_TOKEN);
        }

        public RegCounterCancelToken(CancellationToken wrappedToken) {
            this.regCounter = new AtomicLong(0);
            this.wrappedToken = wrappedToken;
        }

        @Override
        public ListenerRef addCancellationListener(Runnable listener) {
            final ListenerRef result
                    = wrappedToken.addCancellationListener(listener);

            regCounter.incrementAndGet();

            return new ListenerRef() {
                private final AtomicBoolean registered
                        = new AtomicBoolean(true);

                @Override
                public boolean isRegistered() {
                    return result.isRegistered();
                }

                @Override
                public void unregister() {
                    if (registered.getAndSet(false)) {
                        result.unregister();
                        regCounter.decrementAndGet();
                    }
                }
            };
        }

        @Override
        public boolean isCanceled() {
            return wrappedToken.isCanceled();
        }

        @Override
        public void checkCanceled() {
            wrappedToken.checkCanceled();
        }

        public long getRegistrationCount() {
            return regCounter.get();
        }
    }

    private static class ManualExecutorService extends AbstractTaskExecutorService {
        private ListenerManager<Runnable> listeners = new CopyOnTriggerListenerManager<>();
        private boolean shuttedDown = false;
        private final List<SubmittedTask> submittedTasks = new LinkedList<>();

        public void executeSubmittedTasksWithoutRemoving() {
            try {
                for (SubmittedTask task : submittedTasks) {
                    task.task.execute(task.cancelToken);
                    task.cleanupTask.run();
                }
            } catch (Exception ex) {
                ExceptionHelper.rethrow(ex);
            }
        }

        public void executeOne() throws Exception {
            SubmittedTask task = submittedTasks.remove(0);
            task.task.execute(task.cancelToken);
            task.cleanupTask.run();
        }

        public void executeSubmittedTasks() {
            try {
                executeSubmittedTasksMayFail();
            } catch (Exception ex) {
                ExceptionHelper.rethrow(ex);
            }
        }

        private void executeSubmittedTasksMayFail() throws Exception {
            while (!submittedTasks.isEmpty()) {
                executeOne();
            }
        }

        @Override
        protected void submitTask(
                CancellationToken cancelToken,
                CancelableTask task,
                Runnable cleanupTask,
                boolean hasUserDefinedCleanup) {

            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");
            ExceptionHelper.checkNotNullArgument(cleanupTask, "cleanupTask");
            submittedTasks.add(new SubmittedTask(cancelToken, task, cleanupTask));
        }

        @Override
        public void shutdown() {
            shuttedDown = true;
            ListenerManager<Runnable> currentListeners = listeners;
            if (currentListeners != null) {
                listeners = null;
                currentListeners.onEvent((Runnable eventListener, Void arg) -> {
                    eventListener.run();
                }, null);
            }
        }

        @Override
        public void shutdownAndCancel() {
            shutdown();
        }

        @Override
        public boolean isShutdown() {
            return shuttedDown;
        }

        @Override
        public boolean isTerminated() {
            return shuttedDown;
        }

        @Override
        public ListenerRef addTerminateListener(Runnable listener) {
            if (listeners == null) {
                listener.run();
                return UnregisteredListenerRef.INSTANCE;
            }
            else {
                return listeners.registerListener(listener);
            }
        }

        @Override
        public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            return shuttedDown;
        }

        private static class SubmittedTask {
            public final CancellationToken cancelToken;
            public final CancelableTask task;
            public final Runnable cleanupTask;

            public SubmittedTask(CancellationToken cancelToken, CancelableTask task, Runnable cleanupTask) {
                this.cancelToken = cancelToken;
                this.task = task;
                this.cleanupTask = cleanupTask;
            }
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
