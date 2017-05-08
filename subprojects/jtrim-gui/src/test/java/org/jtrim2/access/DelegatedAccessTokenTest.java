package org.jtrim2.access;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelegatedAccessTokenTest {
    @SuppressWarnings("unchecked")
    private static <T> AccessToken<T> mockToken() {
        return mock(AccessToken.class);
    }

    private static DelegatedAccessToken<Object> create(AccessToken<Object> wrapped) {
        return new DelegatedAccessToken<>(wrapped);
    }

    @Test(expected = NullPointerException.class)
    public void testNullConstructor() {
        create(null);
    }

    /**
     * Test of getAccessID method, of class DelegatedAccessToken.
     */
    @Test
    public void testGetAccessID() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        Object result = new Object();
        stub(wrapped.getAccessID()).toReturn(result);

        assertSame(result, token.getAccessID());

        verify(wrapped).getAccessID();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of createExecutor method, of class DelegatedAccessToken.
     */
    @Test
    public void testCreateExecutor() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        TaskExecutor arg = mock(TaskExecutor.class);
        ContextAwareTaskExecutor result = mock(ContextAwareTaskExecutor.class);
        stub(wrapped.createExecutor(arg)).toReturn(result);

        assertSame(result, token.createExecutor(arg));

        verify(wrapped).createExecutor(same(arg));
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of addReleaseListener method, of class DelegatedAccessToken.
     */
    @Test
    public void testAddReleaseListener() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        Runnable arg = mock(Runnable.class);
        ListenerRef result = mock(ListenerRef.class);
        stub(wrapped.addReleaseListener(arg)).toReturn(result);

        assertSame(result, token.addReleaseListener(arg));

        verify(wrapped).addReleaseListener(same(arg));
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of isReleased method, of class DelegatedAccessToken.
     */
    @Test
    public void testIsReleased() {
        for (boolean result: Arrays.asList(false, true)) {
            AccessToken<Object> wrapped = mockToken();
            DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

            stub(wrapped.isReleased()).toReturn(result);

            assertEquals(result, token.isReleased());

            verify(wrapped).isReleased();
            verifyNoMoreInteractions(wrapped);
        }
    }

    /**
     * Test of release method, of class DelegatedAccessToken.
     */
    @Test
    public void testRelease() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        token.release();

        verify(wrapped).release();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of releaseAndCancel method, of class DelegatedAccessToken.
     */
    @Test
    public void testReleaseAndCancel() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        token.releaseAndCancel();

        verify(wrapped).releaseAndCancel();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of awaitRelease method, of class DelegatedAccessToken.
     */
    @Test
    public void testAwaitRelease() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        CancellationToken arg = mock(CancellationToken.class);

        token.awaitRelease(arg);

        verify(wrapped).awaitRelease(same(arg));
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of tryAwaitRelease method, of class DelegatedAccessToken.
     */
    @Test
    public void testTryAwaitRelease() {
        for (boolean result: Arrays.asList(false, true)) {
            for (TimeUnit arg3: TimeUnit.values()) {
                AccessToken<Object> wrapped = mockToken();
                DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

                CancellationToken arg1 = mock(CancellationToken.class);
                long arg2 = 4634675;
                stub(wrapped.tryAwaitRelease(arg1, arg2, arg3)).toReturn(result);

                assertEquals(result, token.tryAwaitRelease(arg1, arg2, arg3));

                verify(wrapped).tryAwaitRelease(arg1, arg2, arg3);
                verifyNoMoreInteractions(wrapped);
            }
        }
    }

    /**
     * Test of toString method, of class DelegatedAccessToken.
     */
    @Test
    public void testToString() {
        AccessToken<Object> wrapped = mockToken();
        DelegatedAccessToken<Object> token = new DelegatedAccessToken<>(wrapped);

        String result = "rewkl4343ijg43imwi4gi4gn;w;qm";
        stub(wrapped.toString()).toReturn(result);

        assertSame(result, token.toString());

        // toString is not checked by mockito
        verifyNoMoreInteractions(wrapped);
    }
}
