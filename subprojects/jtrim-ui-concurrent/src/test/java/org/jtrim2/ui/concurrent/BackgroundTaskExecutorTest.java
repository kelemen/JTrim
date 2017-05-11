package org.jtrim2.ui.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.jtrim2.access.AccessChangeListener;
import org.jtrim2.access.AccessManager;
import org.jtrim2.access.AccessRequest;
import org.jtrim2.access.AccessResult;
import org.jtrim2.access.AccessToken;
import org.jtrim2.access.HierarchicalAccessManager;
import org.jtrim2.access.HierarchicalRight;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.testutils.concurrent.AbstractUiExecutorProvider;
import org.jtrim2.testutils.concurrent.ManualUiExecutorProvider;
import org.jtrim2.testutils.concurrent.SyncUiExecutorProvider;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class BackgroundTaskExecutorTest {
    private static AccessManager<Object, HierarchicalRight> createManager() {
        // DelegatedAccessManager is here to allow spying over the access manager.
        return new DelegatedAccessManager(new HierarchicalAccessManager<>(
                SyncTaskExecutor.getSimpleExecutor()));
    }

    private static BackgroundTaskExecutor<Object, HierarchicalRight> create(
            AccessManager<Object, HierarchicalRight> manager,
            TaskExecutor executor,
            UiExecutorProvider uiExecutorProvider) {
        return new BackgroundTaskExecutor<>(manager, executor, uiExecutorProvider);
    }

    private static BackgroundTaskExecutor<Object, HierarchicalRight> create(
            AccessManager<Object, HierarchicalRight> manager, TaskExecutor executor) {
        return create(manager, executor, new SyncUiExecutorProvider());
    }

    private static BackgroundTaskExecutor<Object, HierarchicalRight> create(
            AccessManager<Object, HierarchicalRight> manager) {
        return create(manager, SyncTaskExecutor.getSimpleExecutor());
    }

    private static Set<Object> getTokenIDs(Collection<AccessToken<Object>> tokens) {
        Set<Object> result = CollectionsEx.newHashSet(tokens.size());
        for (AccessToken<Object> token: tokens) {
            result.add(token.getAccessID());
        }
        return result;
    }

    private static AccessRequest<Object, HierarchicalRight> createRequest() {
        return AccessRequest.getWriteRequest(new Object(), HierarchicalRight.create(new Object()));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(createManager(), null);
    }

    @Test
    public void testTryExecute1() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.tryExecute(request, task);
        assertNull(blockingTokens);
        verify(manager).tryGetAccess(same(request));
        verifyNoMoreInteractions(manager);

        verify(task).execute(any(CancellationToken.class), any(UiReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testTryExecute2() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.tryExecute(
                Cancellation.UNCANCELABLE_TOKEN, request, task);
        assertNull(blockingTokens);
        verify(manager).tryGetAccess(same(request));
        verifyNoMoreInteractions(manager);

        verify(task).execute(any(CancellationToken.class), any(UiReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testTryExecuteFails1() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        AccessResult<Object> blockingAccess = manager.tryGetAccess(request);
        assertTrue(blockingAccess.isAvailable());

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.tryExecute(request, task);
        assertEquals(Collections.singleton(request.getRequestID()), getTokenIDs(blockingTokens));
        verifyZeroInteractions(task);
    }

    @Test
    public void testTryExecuteFails2() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        AccessResult<Object> blockingAccess = manager.tryGetAccess(request);
        assertTrue(blockingAccess.isAvailable());

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens
                = executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task);
        assertEquals(Collections.singleton(request.getRequestID()), getTokenIDs(blockingTokens));
        verifyZeroInteractions(task);
    }

    @Test
    public void testScheduleExecute1() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.scheduleToExecute(request, task);
        assertTrue(blockingTokens.isEmpty());
        verify(manager).getScheduledAccess(same(request));
        verifyNoMoreInteractions(manager);

        verify(task).execute(any(CancellationToken.class), any(UiReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testScheduleExecute2() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.scheduleToExecute(
                Cancellation.UNCANCELABLE_TOKEN, request, task);
        assertTrue(blockingTokens.isEmpty());
        verify(manager).getScheduledAccess(same(request));
        verifyNoMoreInteractions(manager);

        verify(task).execute(any(CancellationToken.class), any(UiReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testScheduleExecuteFails1() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        AccessResult<Object> blockingAccess = manager.tryGetAccess(request);
        assertTrue(blockingAccess.isAvailable());

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.scheduleToExecute(request, task);
        assertEquals(Collections.singleton(request.getRequestID()), getTokenIDs(blockingTokens));
        verifyZeroInteractions(task);

        blockingAccess.getAccessToken().release();

        verify(task).execute(any(CancellationToken.class), any(UiReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testScheduleExecuteFails2() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        AccessResult<Object> blockingAccess = manager.tryGetAccess(request);
        assertTrue(blockingAccess.isAvailable());

        BackgroundTask task = mock(BackgroundTask.class);
        Collection<AccessToken<Object>> blockingTokens = executor.scheduleToExecute(
                Cancellation.UNCANCELABLE_TOKEN, request, task);
        assertEquals(Collections.singleton(request.getRequestID()), getTokenIDs(blockingTokens));
        verifyZeroInteractions(task);

        blockingAccess.getAccessToken().release();

        verify(task).execute(any(CancellationToken.class), any(UiReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testTryExecuteCanceled() throws Throwable {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        TaskWrapper task = new TaskWrapper((CancellationToken cancelToken, UiReporter reporter) -> {
            assertFalse(cancelToken.isCanceled());
            cancelSource.getController().cancel();
            assertTrue(cancelToken.isCanceled());
            throw new OperationCanceledException();
        });
        executor.tryExecute(cancelSource.getToken(), request, task);
        task.expectLastException(OperationCanceledException.class);

        BackgroundTask task2 = mock(BackgroundTask.class);
        executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task2);
        verify(task2).execute(any(CancellationToken.class), any(UiReporter.class));
    }

    @Test
    public void testTryExecuteWithException() throws Throwable {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        TaskWrapper task = new TaskWrapper((CancellationToken cancelToken, UiReporter reporter) -> {
            throw new TestException();
        });
        executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task);
        task.expectLastException(TestException.class);

        BackgroundTask task2 = mock(BackgroundTask.class);
        executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task2);
        verify(task2).execute(any(CancellationToken.class), any(UiReporter.class));
    }

    private static Runnable mockUiTask(AbstractUiExecutorProvider ui, Runnable onWrongThread) {
        Runnable result = mock(Runnable.class);
        doAnswer((InvocationOnMock invocation) -> {
            if (!ui.isInContext()) {
                onWrongThread.run();
            }
            return null;
        }).when(result).run();
        return result;
    }

    @Test
    public void testUiReporterOnUiThread() throws Exception {
        ManualUiExecutorProvider ui = new ManualUiExecutorProvider(true);

        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        final BackgroundTaskExecutor<Object, HierarchicalRight> executor
                = create(manager, SyncTaskExecutor.getSimpleExecutor(), ui);
        final AccessRequest<Object, HierarchicalRight> request = createRequest();

        Runnable wrongThreadCallback = mock(Runnable.class);

        final Runnable data1 = mockUiTask(ui, wrongThreadCallback);
        final Runnable progress1 = mockUiTask(ui, wrongThreadCallback);
        final Runnable progress2 = mockUiTask(ui, wrongThreadCallback);
        final Runnable data2 = mockUiTask(ui, wrongThreadCallback);
        final Runnable progress3 = mockUiTask(ui, wrongThreadCallback);

        ui.runOnUi(() -> {
            executor.tryExecute(request, (CancellationToken cancelToken, UiReporter reporter) -> {
                reporter.writeData(data1);
                reporter.updateProgress(progress1);
                reporter.updateProgress(progress2);
                reporter.writeData(data2);
                reporter.updateProgress(progress3);
            });
        });
        ui.executeAll();

        InOrder inOrder = inOrder(data1, data2);
        inOrder.verify(data1).run();
        inOrder.verify(data2).run();
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(progress1, progress2);
        verify(progress3).run();

        verifyZeroInteractions(wrongThreadCallback);
    }

    @Test
    public void testUiReporterFromBackgroundThread() throws Exception {
        ManualUiExecutorProvider ui = new ManualUiExecutorProvider(true);

        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        final BackgroundTaskExecutor<Object, HierarchicalRight> executor
                = create(manager, SyncTaskExecutor.getSimpleExecutor(), ui);
        final AccessRequest<Object, HierarchicalRight> request = createRequest();

        Runnable wrongThreadCallback = mock(Runnable.class);

        Runnable data1 = mockUiTask(ui, wrongThreadCallback);
        Runnable progress1 = mockUiTask(ui, wrongThreadCallback);

        executor.tryExecute(request, (CancellationToken cancelToken, UiReporter reporter) -> {
            reporter.writeData(data1);
            reporter.updateProgress(progress1);
        });
        ui.executeAll();

        verify(data1).run();
        verify(progress1).run();

        verifyZeroInteractions(wrongThreadCallback);
    }

    private static class TaskWrapper implements BackgroundTask {
        private final BackgroundTask task;
        private volatile Throwable lastException;

        public TaskWrapper(BackgroundTask task) {
            assert task != null;
            this.task = task;
            this.lastException = null;
        }

        @Override
        public void execute(CancellationToken cancelToken, UiReporter reporter) throws Exception {
            try {
                task.execute(cancelToken, reporter);
            } catch (Throwable ex) {
                lastException = ex;
            }
        }

        public Throwable getLastException() {
            return lastException;
        }

        public void expectLastException(Class<? extends Throwable> exClass) throws Throwable {
            Throwable toThrow = lastException;
            if (toThrow == null) {
                throw new AssertionError("Expected: " + exClass.getName());
            }

            if (!exClass.isAssignableFrom(toThrow.getClass())) {
                throw toThrow;
            }
        }

        public void rethrowLastException() throws Throwable {
            Throwable toThrow = lastException;
            if (toThrow != null) {
                throw toThrow;
            }
        }
    }

    private static class DelegatedAccessManager implements AccessManager<Object, HierarchicalRight> {
        private final AccessManager<Object, HierarchicalRight> wrapped;

        public DelegatedAccessManager(AccessManager<Object, HierarchicalRight> wrapped) {
            assert wrapped != null;
            this.wrapped = wrapped;
        }

        @Override
        public ListenerRef addAccessChangeListener(AccessChangeListener<Object, HierarchicalRight> listener) {
            return wrapped.addAccessChangeListener(listener);
        }

        @Override
        public Collection<AccessToken<Object>> getBlockingTokens(
                Collection<? extends HierarchicalRight> requestedReadRights,
                Collection<? extends HierarchicalRight> requestedWriteRights) {
            return wrapped.getBlockingTokens(requestedReadRights, requestedWriteRights);
        }

        @Override
        public boolean isAvailable(
                Collection<? extends HierarchicalRight> requestedReadRights,
                Collection<? extends HierarchicalRight> requestedWriteRights) {
            return wrapped.isAvailable(requestedReadRights, requestedWriteRights);
        }

        @Override
        public AccessResult<Object> tryGetAccess(
                AccessRequest<? extends Object, ? extends HierarchicalRight> request) {
            return wrapped.tryGetAccess(request);
        }

        @Override
        public AccessResult<Object> getScheduledAccess(
                AccessRequest<? extends Object, ? extends HierarchicalRight> request) {
            return wrapped.getScheduledAccess(request);
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 133103314394498930L;
    }
}
