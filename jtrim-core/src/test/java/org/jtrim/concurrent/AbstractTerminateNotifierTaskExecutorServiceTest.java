package org.jtrim.concurrent;

import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationController;
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
public class AbstractTerminateNotifierTaskExecutorServiceTest {

    public AbstractTerminateNotifierTaskExecutorServiceTest() {
    }

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

    @Test(expected = IllegalStateException.class)
    public void testIllegalNotify1() {
        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(false, false));

        executor.notifyTerminateListeners();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalNotify2() {
        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(true, false));

        executor.notifyTerminateListeners();
    }

    @Test
    public void testNotifyEmpty() {
        // Simply verify that no exception is thrown when there are no
        // registered listeners.

        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(true, true));

        executor.notifyTerminateListeners();
    }

    @Test
    public void testNotifySingleListener() {
        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(true, true));

        Runnable listener = mock(Runnable.class);

        executor.addTerminateListener(listener);
        verifyZeroInteractions(listener);

        executor.notifyTerminateListeners();

        verify(listener).run();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testNotifyTwoListeners() {
        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(true, true));

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);

        executor.addTerminateListener(listener1);
        executor.addTerminateListener(listener2);
        verifyZeroInteractions(listener1, listener2);

        executor.notifyTerminateListeners();

        verify(listener1).run();
        verify(listener2).run();
        verifyNoMoreInteractions(listener1, listener2);
    }

    @Test
    public void testMultipleNotify() {
        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(true, true));

        Runnable listener = mock(Runnable.class);

        executor.addTerminateListener(listener);
        verifyZeroInteractions(listener);

        executor.notifyTerminateListeners();
        executor.notifyTerminateListeners();

        verify(listener).run();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testNotifyPriorAdd() {
        AbstractTerminateNotifierTaskExecutorService executor
                = spy(new AbstractTerminateNotifierTaskExecutorServiceImpl(true, true));

        executor.notifyTerminateListeners();

        Runnable listener = mock(Runnable.class);
        executor.addTerminateListener(listener);

        verify(listener).run();
        verifyNoMoreInteractions(listener);
    }

    private static class AbstractTerminateNotifierTaskExecutorServiceImpl
    extends
            AbstractTerminateNotifierTaskExecutorService {
        private final boolean shuttedDown;
        private final boolean terminated;

        public AbstractTerminateNotifierTaskExecutorServiceImpl(boolean shuttedDown, boolean terminated) {
            this.shuttedDown = shuttedDown;
            this.terminated = terminated;
        }

        @Override
        protected void submitTask(CancellationToken cancelToken, CancellationController cancelController, CancelableTask task, Runnable cleanupTask, boolean hasUserDefinedCleanup) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void shutdownAndCancel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isShutdown() {
            return shuttedDown;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }

        @Override
        public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
