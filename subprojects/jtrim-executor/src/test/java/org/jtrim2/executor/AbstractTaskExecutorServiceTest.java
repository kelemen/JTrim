package org.jtrim2.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.jtrim2.testutils.executor.MockTask;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class AbstractTaskExecutorServiceTest {
    @Test
    public void testWithoutUserDefinedCleanup1() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableTask task = mock(CancelableTask.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(AbstractTaskExecutor.SubmittedTask.class));
    }

    @Test
    public void testWithoutUserDefinedCleanup2() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        Runnable task = mock(Runnable.class);

        executor.execute(task);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(AbstractTaskExecutor.SubmittedTask.class));
    }

    @Test
    public void testWithoutUserDefinedCleanup3() {
        ManualExecutorService executor = spy(new ManualExecutorService());
        CancelableFunction<?> task = mock(CancelableFunction.class);

        executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, task);
        verify(executor).submitTask(
                any(CancellationToken.class),
                any(AbstractTaskExecutor.SubmittedTask.class));
    }

    private static <V> MockFunction<V> mockFunction(V result) {
        return MockFunction.mock(result);
    }

    private static CancelableTask toTask(MockTask mockTask) {
        return MockTask.toTask(mockTask);
    }

    private static <V> CancelableFunction<V> toFunction(MockFunction<V> mockFunction) {
        return MockFunction.toFunction(mockFunction);
    }

    private static <V> BiConsumer<V, Throwable> toCleanupTask(MockCleanup mockCleanup) {
        return MockCleanup.toCleanupTask(mockCleanup);
    }

    @Test
    public void testExecuteNoCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task));

        verifyZeroInteractions(task);
        executor.executeSubmittedTasks();
        verify(task).execute(false);
    }

    @Test
    public void testExecuteExceptionTaskWithoutCleanup() throws Exception {
        ManualExecutorService executor = new ManualExecutorService();

        Runnable task = mock(Runnable.class);
        doThrow(new TestException()).when(task).run();

        executor.execute(task);

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.executeSubmittedTasks();
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
    }

    @Test
    public void testExecuteCanceledExceptionTaskWithoutCleanup() throws Exception {
        ManualExecutorService executor = new ManualExecutorService();

        Runnable task = mock(Runnable.class);
        doThrow(new OperationCanceledException()).when(task).run();

        executor.execute(task);

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.executeSubmittedTasks();
            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    @Test
    public void testExecuteWithCleanup() {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task))
                .whenComplete(toCleanupTask(cleanup));

        verifyZeroInteractions(task, cleanup);
        executor.executeSubmittedTasks();

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(false);
        inOrder.verify(cleanup).cleanup(null, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteFunctionNoCleanup() throws Exception {
        ManualExecutorService executor = new ManualExecutorService();

        Object taskResult = "TASK-RESULT";
        MockFunction<Object> function = mockFunction(taskResult);

        executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function));

        verifyZeroInteractions(function);
        executor.executeSubmittedTasks();
        verify(function).execute(false);
    }

    @Test
    public void testExecuteFunctionWithCleanup() throws Exception {
        ManualExecutorService executor = new ManualExecutorService();

        Object taskResult = "TASK-RESULT";
        MockFunction<Object> function = mockFunction(taskResult);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function))
                .whenComplete(toCleanupTask(cleanup));

        verifyZeroInteractions(function, cleanup);
        executor.executeSubmittedTasks();

        InOrder inOrder = inOrder(function, cleanup);
        inOrder.verify(function).execute(false);
        inOrder.verify(cleanup).cleanup(taskResult, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testCanceledBeforeExecute() {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.execute(Cancellation.CANCELED_TOKEN, toTask(task))
                .whenComplete(toCleanupTask(cleanup));

        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));

        executor.executeSubmittedTasks();
        verifyZeroInteractions(task);
    }

    @Test
    public void testCanceledBeforeExecuteFunction() {
        ManualExecutorService executor = new ManualExecutorService();

        Object result = "My-Test-Result-1";
        MockFunction<Object> function = mockFunction(result);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.executeFunction(Cancellation.CANCELED_TOKEN, toFunction(function))
                .whenComplete(toCleanupTask(cleanup));

        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));

        executor.executeSubmittedTasks();
        verifyZeroInteractions(function);
    }

    @Test
    public void testPostExecuteCanceledExecute() {
        ManualExecutorService executor = new ManualExecutorService();

        CancellationSource cancelSource = Cancellation.createCancellationSource();

        MockTask task = mock(MockTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.execute(cancelSource.getToken(), toTask(task))
                .whenComplete(toCleanupTask(cleanup));

        cancelSource.getController().cancel();

        executor.executeSubmittedTasks();
        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
    }

    @Test
    public void testExecuteError() {
        ManualExecutorService executor = new ManualExecutorService();

        MockCleanup cleanup = mock(MockCleanup.class);

        TestException taskError = new TestException();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            throw taskError;
        }).whenComplete(toCleanupTask(cleanup));

        verifyZeroInteractions(cleanup);
        executor.executeSubmittedTasks();

        verify(cleanup).cleanup(isNull(), same(taskError));
    }

    private void testUnregisterListener(CancellationToken initCancelToken) {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);

        RegCounterCancelToken cancelToken = new RegCounterCancelToken(initCancelToken);
        executor.execute(cancelToken, toTask(task));

        executor.executeSubmittedTasks();
        verify(task).execute(anyBoolean());

        assertEquals("Remaining registered listener.", 0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListener() {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);

        RegCounterCancelToken cancelToken = new RegCounterCancelToken(Cancellation.UNCANCELABLE_TOKEN);
        executor.execute(cancelToken, toTask(task));

        executor.executeSubmittedTasks();
        verify(task).execute(false);

        assertEquals("Remaining registered listener.", 0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListenerPreCancel() {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);

        RegCounterCancelToken cancelToken = new RegCounterCancelToken(Cancellation.CANCELED_TOKEN);
        executor.execute(cancelToken, toTask(task));

        executor.executeSubmittedTasks();
        verifyZeroInteractions(task);

        assertEquals("Remaining registered listener.", 0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListenerPostCancel() {
        ManualExecutorService executor = new ManualExecutorService();

        MockTask task = mock(MockTask.class);

        CancellationSource cancel = Cancellation.createCancellationSource();
        RegCounterCancelToken cancelToken = new RegCounterCancelToken(cancel.getToken());
        executor.execute(cancelToken, toTask(task));

        cancel.getController().cancel();

        executor.executeSubmittedTasks();
        verifyZeroInteractions(task);

        assertEquals("Remaining registered listener.", 0, cancelToken.getRegistrationCount());
    }

    @Test(timeout = 5000)
    public void testAwaitTerminate() {
        ManualExecutorService executor = new ManualExecutorService();
        executor.shutdown();
        executor.awaitTermination(Cancellation.CANCELED_TOKEN);
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testExecuteAfterShutdownWithCleanup() throws Exception {
        MockTask task = mock(MockTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        ManualExecutorService executor = new ManualExecutorService();
        executor.shutdown();

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task))
                .whenComplete(toCleanupTask(cleanup));

        executor.executeSubmittedTasks();

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
    }

    @Test
    public void testExecuteCanceledTask() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        doThrow(OperationCanceledException.class)
                .when(task).execute(any(CancellationToken.class));

        ManualExecutorService executor = new ManualExecutorService();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                .whenComplete(toCleanupTask(cleanup));

        verifyZeroInteractions(task, cleanup);
        executor.executeSubmittedTasks();

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testProtectionAgainstMultipleExecute() throws Exception {
        MockTask task = mock(MockTask.class);

        ManualExecutorService executor = new ManualExecutorService();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task));

        verifyZeroInteractions(task);
        for (int i = 0; i < 2; i++) {
            executor.executeSubmittedTasksWithoutRemoving();
            verify(task).execute(false);
        }
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

            AtomicBoolean registered = new AtomicBoolean(true);
            return () -> {
                if (registered.getAndSet(false)) {
                    result.unregister();
                    regCounter.decrementAndGet();
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
        private final List<SubmittedTaskDef> submittedTasks = new LinkedList<>();

        public void executeSubmittedTasksWithoutRemoving() {
            try {
                for (SubmittedTaskDef task : submittedTasks) {
                    task.execute();
                }
            } catch (Exception ex) {
                ExceptionHelper.rethrow(ex);
            }
        }

        public void executeOne() throws Exception {
            SubmittedTaskDef task = submittedTasks.remove(0);
            task.execute();
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
        protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(submittedTask, "submittedTask");
            submittedTasks.add(new SubmittedTaskDef(cancelToken, submittedTask));
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
                return ListenerRefs.unregistered();
            } else {
                return listeners.registerListener(listener);
            }
        }

        @Override
        public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            return shuttedDown;
        }

        private static class SubmittedTaskDef {
            public final CancellationToken cancelToken;
            public final SubmittedTask<?> task;

            public SubmittedTaskDef(CancellationToken cancelToken, SubmittedTask<?> task) {
                this.cancelToken = cancelToken;
                this.task = task;
            }

            public void execute() {
                task.execute(cancelToken);
            }
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
