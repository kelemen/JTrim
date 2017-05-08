package org.jtrim2.access;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AbstractAccessTokenTest {
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

    private static <T> AbstractAccessToken<T> mockAbstractToken() {
        return spy(new AbstractAccessTokenImpl<T>());
    }

    /**
     * Test of notifyReleaseListeners method, of class AbstractAccessToken.
     */
    @Test
    public void testNotifyReleaseListeners() {
        for (int numberOfListeners = 0; numberOfListeners < 5; numberOfListeners++) {
            AbstractAccessToken<Object> token = mockAbstractToken();
            stub(token.isReleased()).toReturn(true);

            Runnable[] listeners = new Runnable[numberOfListeners];
            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = mock(Runnable.class);
                token.addReleaseListener(listeners[i]);
            }

            token.notifyReleaseListeners();

            for (int i = 0; i < listeners.length; i++) {
                verify(listeners[i]).run();
                verifyNoMoreInteractions(listeners[i]);
            }
        }
    }

    @Test
    public void testNotifyReleaseListenersIdempotent() {
        AbstractAccessToken<Object> token = mockAbstractToken();
        stub(token.isReleased()).toReturn(true);

        Runnable listener = mock(Runnable.class);
        token.addReleaseListener(listener);

        token.notifyReleaseListeners();
        token.notifyReleaseListeners();

        verify(listener).run();
        verifyNoMoreInteractions(listener);
    }

    @Test(expected = IllegalStateException.class)
    public void testNotifyReleaseListenersNotReleased() {
        AbstractAccessToken<Object> token = mockAbstractToken();
        stub(token.isReleased()).toReturn(false);

        token.notifyReleaseListeners();
    }

    /**
     * Test of awaitRelease method, of class AbstractAccessToken.
     */
    @Test
    public void testAwaitRelease1() {
        AbstractAccessToken<Object> token = mockAbstractToken();
        stub(token.tryAwaitRelease(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(true);

        token.awaitRelease(mock(CancellationToken.class));

        verify(token).awaitRelease(any(CancellationToken.class));
        verify(token).tryAwaitRelease(any(CancellationToken.class), anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(token);
    }

    @Test
    public void testAwaitRelease2() {
        AbstractAccessToken<Object> token = mockAbstractToken();
        stub(token.tryAwaitRelease(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(false)
                .toReturn(true);

        token.awaitRelease(mock(CancellationToken.class));

        verify(token).awaitRelease(any(CancellationToken.class));
        verify(token, times(2)).tryAwaitRelease(any(CancellationToken.class), anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(token);
    }

    public static class AbstractAccessTokenImpl<T> extends AbstractAccessToken<T> {
        @Override
        public T getAccessID() {
            return null;
        }

        @Override
        public ContextAwareTaskExecutor createExecutor(TaskExecutor executor) {
            return null;
        }

        @Override
        public boolean isReleased() {
            return false;
        }

        @Override
        public void release() {
        }

        @Override
        public void releaseAndCancel() {
        }

        @Override
        public boolean tryAwaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public boolean isExecutingInThis() {
            return false;
        }
    }
}
