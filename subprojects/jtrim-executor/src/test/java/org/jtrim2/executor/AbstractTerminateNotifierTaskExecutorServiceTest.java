package org.jtrim2.executor;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AbstractTerminateNotifierTaskExecutorServiceTest {
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
        verifyNoInteractions(listener);

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
        verifyNoInteractions(listener1, listener2);

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
        verifyNoInteractions(listener);

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
        protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
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
