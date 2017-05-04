package org.jtrim2.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.testutils.LogTests;
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
public class ContextAwareWrapperTest {
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

    private static ContextAwareWrapper create() {
        return new ContextAwareWrapper(SyncTaskExecutor.getSimpleExecutor());
    }

    @Test
    public void testShotcutOfSameContextExecutor() {
        TaskExecutor wrapped = SyncTaskExecutor.getSimpleExecutor();
        ContextAwareWrapper executor1 = new ContextAwareWrapper(wrapped);
        ContextAwareWrapper executor2 = executor1.sameContextExecutor(wrapped);
        assertSame(executor1, executor2);
    }

    @Test
    public void testContextAwareness() {
        final ContextAwareWrapper executor = create();

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContextTask.set(executor.isExecutingInThis());
        }, (boolean canceled, Throwable error) -> {
            inContextCleanupTask.set(executor.isExecutingInThis());
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInTask() {
        final ContextAwareWrapper executor = create();
        final ContextAwareWrapper sibling = executor.sameContextExecutor(new SyncTaskExecutor());

        final AtomicBoolean inContextTask1 = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask1 = new AtomicBoolean(false);
        final AtomicBoolean inContextTask2 = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask2 = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken subTaskCancelToken) -> {
                inContextTask1.set(executor.isExecutingInThis());
                inContextTask2.set(sibling.isExecutingInThis());
            }, (boolean canceled, Throwable error) -> {
                inContextCleanupTask1.set(executor.isExecutingInThis());
                inContextCleanupTask2.set(sibling.isExecutingInThis());
            });
        }, null);

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask1.get());
        assertTrue(inContextCleanupTask1.get());

        assertFalse(sibling.isExecutingInThis());
        assertTrue(inContextTask2.get());
        assertTrue(inContextCleanupTask2.get());
    }

    @Test
    public void testRecursiveContextAwarenessInCleanup() {
        final ContextAwareWrapper executor = create();
        final ContextAwareWrapper sibling = executor.sameContextExecutor(new SyncTaskExecutor());

        final AtomicBoolean inContextTask1 = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask1 = new AtomicBoolean(false);
        final AtomicBoolean inContextTask2 = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask2 = new AtomicBoolean(false);

        CancelableTask noop = CancelableTasks.noOpCancelableTask();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, noop, (boolean canceled, Throwable error) -> {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                inContextTask1.set(executor.isExecutingInThis());
                inContextTask2.set(sibling.isExecutingInThis());
            }, (boolean subTaskCanceled, Throwable subTaskError) -> {
                inContextCleanupTask1.set(executor.isExecutingInThis());
                inContextCleanupTask2.set(sibling.isExecutingInThis());
            });
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask1.get());
        assertTrue(inContextCleanupTask1.get());

        assertFalse(sibling.isExecutingInThis());
        assertTrue(inContextTask2.get());
        assertTrue(inContextCleanupTask2.get());
    }

    @Test
    public void testContextAwarenessAfterFailedTask() throws Exception {
        final ContextAwareWrapper executor = create();
        final ContextAwareWrapper sibling = executor.sameContextExecutor(new SyncTaskExecutor());

        CancelableTask task = mock(CancelableTask.class);
        doThrow(new TestException()).when(task).execute(any(CancellationToken.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).execute(any(CancellationToken.class));
        assertFalse(executor.isExecutingInThis());
        assertFalse(sibling.isExecutingInThis());
    }

    @Test
    public void testContextAwarenessAfterFailedCleanup() throws Exception {
        final ContextAwareWrapper executor = create();
        final ContextAwareWrapper sibling = executor.sameContextExecutor(new SyncTaskExecutor());

        CleanupTask cleanup = mock(CleanupTask.class);
        doThrow(new TestException()).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask(), cleanup);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        assertFalse(executor.isExecutingInThis());
        assertFalse(sibling.isExecutingInThis());
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
