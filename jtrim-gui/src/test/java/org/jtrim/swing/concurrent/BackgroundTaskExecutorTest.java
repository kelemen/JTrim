package org.jtrim.swing.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jtrim.access.AccessChangeListener;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResult;
import org.jtrim.access.AccessToken;
import org.jtrim.access.HierarchicalAccessManager;
import org.jtrim.access.HierarchicalRight;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.ListenerRef;
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
public class BackgroundTaskExecutorTest {
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

    private static AccessManager<Object, HierarchicalRight> createManager() {
        // DelegatedAccessManager is here to allow spying over the access manager.
        return new DelegatedAccessManager(new HierarchicalAccessManager<>(
                SyncTaskExecutor.getSimpleExecutor()));
    }

    private static BackgroundTaskExecutor<Object, HierarchicalRight> create(
            AccessManager<Object, HierarchicalRight> manager, TaskExecutor executor) {
        return new BackgroundTaskExecutor<>(manager, executor);
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

        verify(task).execute(any(CancellationToken.class), any(SwingReporter.class));
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

        verify(task).execute(any(CancellationToken.class), any(SwingReporter.class));
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

        verify(task).execute(any(CancellationToken.class), any(SwingReporter.class));
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

        verify(task).execute(any(CancellationToken.class), any(SwingReporter.class));
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

        verify(task).execute(any(CancellationToken.class), any(SwingReporter.class));
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

        verify(task).execute(any(CancellationToken.class), any(SwingReporter.class));
        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test
    public void testTryExecuteCanceled() throws Throwable {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        TaskWrapper task = new TaskWrapper((CancellationToken cancelToken, SwingReporter reporter) -> {
            assertFalse(cancelToken.isCanceled());
            cancelSource.getController().cancel();
            assertTrue(cancelToken.isCanceled());
            throw new OperationCanceledException();
        });
        executor.tryExecute(cancelSource.getToken(), request, task);
        task.expectLastException(OperationCanceledException.class);

        BackgroundTask task2 = mock(BackgroundTask.class);
        executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task2);
        verify(task2).execute(any(CancellationToken.class), any(SwingReporter.class));
    }

    @Test
    public void testTryExecuteWithException() throws Throwable {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        AccessRequest<Object, HierarchicalRight> request = createRequest();

        TaskWrapper task = new TaskWrapper((CancellationToken cancelToken, SwingReporter reporter) -> {
            throw new TestException();
        });
        executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task);
        task.expectLastException(TestException.class);

        BackgroundTask task2 = mock(BackgroundTask.class);
        executor.tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task2);
        verify(task2).execute(any(CancellationToken.class), any(SwingReporter.class));
    }

    @Test
    public void testSwingReporter() throws Exception {
        AccessManager<Object, HierarchicalRight> manager = spy(createManager());
        final BackgroundTaskExecutor<Object, HierarchicalRight> executor = create(manager);
        final AccessRequest<Object, HierarchicalRight> request = createRequest();

        final Runnable data1 = mock(Runnable.class);
        final Runnable progress1 = mock(Runnable.class);
        final Runnable progress2 = mock(Runnable.class);
        final Runnable data2 = mock(Runnable.class);
        final Runnable progress3 = mock(Runnable.class);

        SwingUtilities.invokeAndWait(() -> {
            executor.tryExecute(request, (CancellationToken cancelToken, SwingReporter reporter) -> {
                reporter.writeData(data1);
                reporter.updateProgress(progress1);
                reporter.updateProgress(progress2);
                reporter.writeData(data2);
                reporter.updateProgress(progress3);
            });
        });

        SwingUtilities.invokeAndWait(() -> {
            InOrder inOrder = inOrder(data1, data2);
            inOrder.verify(data1).run();
            inOrder.verify(data2).run();
            inOrder.verifyNoMoreInteractions();

            verifyZeroInteractions(progress1, progress2);
            verify(progress3).run();
        });
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
        public void execute(CancellationToken cancelToken, SwingReporter reporter) throws Exception {
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
