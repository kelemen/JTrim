package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
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
    public void testContextAwareness() {
        final ContextAwareWrapper executor = create();

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                inContextTask.set(executor.isExecutingInThis());
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                inContextCleanupTask.set(executor.isExecutingInThis());
            }
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInTask() {
        final ContextAwareWrapper executor = create();

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        inContextTask.set(executor.isExecutingInThis());
                    }
                }, new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        inContextCleanupTask.set(executor.isExecutingInThis());
                    }
                });
            }
        }, null);

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInCleanup() {
        final ContextAwareWrapper executor = create();

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        inContextTask.set(executor.isExecutingInThis());
                    }
                }, new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        inContextCleanupTask.set(executor.isExecutingInThis());
                    }
                });
            }
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testContextAwarenessAfterFailedTask() throws Exception {
        final ContextAwareWrapper executor = create();

        CancelableTask task = mock(CancelableTask.class);
        doThrow(RuntimeException.class).when(task).execute(any(CancellationToken.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

        verify(task).execute(any(CancellationToken.class));
        assertFalse(executor.isExecutingInThis());
    }

    @Test
    public void testContextAwarenessAfterFailedCleanup() throws Exception {
        final ContextAwareWrapper executor = create();

        CleanupTask cleanup = mock(CleanupTask.class);
        doThrow(RuntimeException.class).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), cleanup);

        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        assertFalse(executor.isExecutingInThis());
    }
}