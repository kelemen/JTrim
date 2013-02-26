package org.jtrim.cancel;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
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
public class CombinedTokenAnyTest {

    public CombinedTokenAnyTest() {
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

    private void checkNotCanceled(CombinedTokenAny token) {
        Runnable listener = mock(Runnable.class);

        ListenerRef listenerRef = token.addCancellationListener(listener);

        assertFalse(token.isCanceled());
        token.checkCanceled();

        verifyZeroInteractions(listener);

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
    }

    private void checkCanceled(CombinedTokenAny token) {
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
        assertFalse(listenerRef.isRegistered());
    }

    private static CombinedTokenAny combine(CancellationToken... tokens) {
        return new CombinedTokenAny(tokens);
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
        ListenerRef ref2 = mock(ListenerRef.class);

        TestException ex1 = new TestException();
        TestException ex2 = new TestException();

        stub(token0.addCancellationListener(any(Runnable.class))).toReturn(UnregisteredListenerRef.INSTANCE);
        stub(token0.isCanceled()).toReturn(false);

        stub(token1.addCancellationListener(any(Runnable.class))).toReturn(ref1);
        stub(token1.isCanceled()).toReturn(false);

        stub(token2.addCancellationListener(any(Runnable.class))).toThrow(ex1);
        stub(token2.isCanceled()).toReturn(false);

        stub(ref1.isRegistered()).toReturn(true);
        stub(ref2.isRegistered()).toReturn(true);

        doThrow(ex2).when(ref1).unregister();

        CombinedTokenAny token = combine(token0, token1, token2);

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

        CombinedTokenAny token = combine(cancelSource.getToken());

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);

        ListenerRef listenerRef1 = token.addCancellationListener(listener1);
        token.addCancellationListener(listener2);

        assertTrue(listenerRef1.isRegistered());
        listenerRef1.unregister();
        assertFalse(listenerRef1.isRegistered());

        cancelSource.getController().cancel();
        verifyZeroInteractions(listener1);
        verify(listener2).run();
        verifyNoMoreInteractions(listener2);
    }

    @Test
    public void testZeroTokens() {
        checkNotCanceled(combine());
    }

    @Test
    public void testSingleNotCanceled() {
        checkNotCanceled(combine(Cancellation.UNCANCELABLE_TOKEN));
    }

    @Test
    public void testTwoNotCanceled() {
        checkNotCanceled(combine(Cancellation.UNCANCELABLE_TOKEN, Cancellation.UNCANCELABLE_TOKEN));
    }

    @Test
    public void testManyNotCanceled() {
        CancellationToken[] tokens = new CancellationToken[]{
            Cancellation.UNCANCELABLE_TOKEN,
            Cancellation.UNCANCELABLE_TOKEN,
            Cancellation.UNCANCELABLE_TOKEN,
            Cancellation.UNCANCELABLE_TOKEN,
            Cancellation.UNCANCELABLE_TOKEN
        };
        checkNotCanceled(combine(tokens));
    }

    private static boolean isBitSet(int value, int bitIndex) {
        return ((value >>> bitIndex) & 1) != 0;
    }

    private void testCanceled(int tokenCount) {
        assert tokenCount > 0;

        CancellationToken[] tokens = new CancellationToken[tokenCount];
        // Test all combinations except when each of them is UNCANCELABLE_TOKEN
        int testCount = 1 << tokenCount; // The number of combinations
        // We omit where each of them is UNCANCELABLE_TOKEN by starting
        // from 1 instead of 0
        for (int combinationIndex = 1; combinationIndex < testCount; combinationIndex++) {
            for (int i = 0; i < tokenCount; i++) {
                tokens[i] = isBitSet(combinationIndex, i)
                        ? Cancellation.CANCELED_TOKEN
                        : Cancellation.UNCANCELABLE_TOKEN;
            }
            checkCanceled(combine(tokens));
        }
    }

    @Test
    public void testSingleCanceled() {
        testCanceled(1);
    }

    @Test
    public void testTwoCanceled() {
        testCanceled(2);
    }

    @Test
    public void testManyCanceled() {
        testCanceled(5);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -8002609789424228075L;
    }
}
