package org.jtrim2.cancel;

import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class CombinedTokenAllTest {
    private void checkNotCanceled(CombinedTokenAll token) {
        Runnable listener = mock(Runnable.class);

        ListenerRef listenerRef = token.addCancellationListener(listener);

        assertFalse(token.isCanceled());
        token.checkCanceled();

        verifyZeroInteractions(listener);

        listenerRef.unregister();
    }

    private void checkCanceled(CombinedTokenAll token) {
        Runnable listener = mock(Runnable.class);

        ListenerRef listenerRef = token.addCancellationListener(listener);
        verify(listener).run();
        verifyNoMoreInteractions(listener);

        assertTrue(token.isCanceled());

        try {
            token.checkCanceled();
            fail("Expected: OperationCanceledException");
        } catch (OperationCanceledException ex) {
        }

        listenerRef.unregister();
    }

    private static CombinedTokenAll combine(CancellationToken... tokens) {
        return new CombinedTokenAll(tokens);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        combine((CancellationToken[])null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        combine(Cancellation.CANCELED_TOKEN, null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor3() {
        combine(null, Cancellation.CANCELED_TOKEN);
    }

    @Test
    public void testBuggyWrappedTokens() {
        // This method tests that the first exception thrown will be rethrown
        // and subsequent exceptions will be suppressed.

        // This test assumes that tokens are called in the order they are passed
        // to the constructor, this is not strictly necessary but there is no
        // reason for CombinedTokenAny to do otherwise.

        CancellationToken token0 = mock(CancellationToken.class);
        CancellationToken token1 = mock(CancellationToken.class);
        CancellationToken token2 = mock(CancellationToken.class);

        ListenerRef ref1 = mock(ListenerRef.class);

        TestException ex1 = new TestException();
        TestException ex2 = new TestException();

        stub(token0.addCancellationListener(any(Runnable.class))).toReturn(ListenerRefs.unregistered());
        stub(token0.isCanceled()).toReturn(false);

        stub(token1.addCancellationListener(any(Runnable.class))).toReturn(ref1);
        stub(token1.isCanceled()).toReturn(false);

        stub(token2.addCancellationListener(any(Runnable.class))).toThrow(ex1);
        stub(token2.isCanceled()).toReturn(false);

        doThrow(ex2).when(ref1).unregister();

        CombinedTokenAll token = combine(token0, token1, token2);

        try {
            token.addCancellationListener(mock(Runnable.class));
            fail("Exception expected.");
        } catch (TestException ex) {
            assertSame(ex1, ex);
            assertArrayEquals(new Throwable[]{ex2}, ex.getSuppressed());
        }
    }

    @Test
    public void testListenerRef() {
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        CombinedTokenAll token = combine(cancelSource.getToken());

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);

        ListenerRef listenerRef1 = token.addCancellationListener(listener1);
        token.addCancellationListener(listener2);

        listenerRef1.unregister();

        cancelSource.getController().cancel();
        verifyZeroInteractions(listener1);
        verify(listener2).run();
        verifyNoMoreInteractions(listener2);
    }

    @Test
    public void testZeroTokens() {
        checkCanceled(combine());
    }

    @Test
    public void testSingleCanceled() {
        checkCanceled(combine(Cancellation.CANCELED_TOKEN));
    }

    @Test
    public void testTwoCanceled() {
        checkCanceled(combine(Cancellation.CANCELED_TOKEN, Cancellation.CANCELED_TOKEN));
    }

    @Test
    public void testManyCanceled() {
        CancellationToken[] tokens = new CancellationToken[]{
            Cancellation.CANCELED_TOKEN,
            Cancellation.CANCELED_TOKEN,
            Cancellation.CANCELED_TOKEN,
            Cancellation.CANCELED_TOKEN,
            Cancellation.CANCELED_TOKEN
        };
        checkCanceled(combine(tokens));
    }

    private static boolean isBitSet(int value, int bitIndex) {
        return ((value >>> bitIndex) & 1) != 0;
    }

    private void testNotCanceled(int tokenCount) {
        assert tokenCount > 0;

        CancellationToken[] tokens = new CancellationToken[tokenCount];
        // Test all combinations except when each of them is CANCELED_TOKEN
        int testCount = 1 << tokenCount; // The number of combinations
        // We omit where each of them is CANCELED_TOKEN by starting
        // from 1 instead of 0
        for (int combinationIndex = 1; combinationIndex < testCount; combinationIndex++) {
            for (int i = 0; i < tokenCount; i++) {
                tokens[i] = isBitSet(combinationIndex, i)
                        ? Cancellation.UNCANCELABLE_TOKEN
                        : Cancellation.CANCELED_TOKEN;
            }
            checkNotCanceled(combine(tokens));
        }
    }

    @Test
    public void testSingleNotCanceled() {
        testNotCanceled(1);
    }

    @Test
    public void testTwoNotCanceled() {
        testNotCanceled(2);
    }

    @Test
    public void testManyNotCanceled() {
        testNotCanceled(5);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -8002609789424228075L;
    }
}
