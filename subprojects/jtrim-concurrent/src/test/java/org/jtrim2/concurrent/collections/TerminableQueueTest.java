package org.jtrim2.concurrent.collections;

import java.util.concurrent.TimeUnit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ReservedElementRef;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TerminableQueueTest {
    private Matcher<CancellationToken> cancellationToken(boolean canceled) {
        return new BaseMatcher<CancellationToken>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof CancellationToken) {
                    return ((CancellationToken) item).isCanceled() == canceled;
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("CancellationToken must " + (canceled ? "be canceled" : "not be canceled"));
            }
        };
    }

    private void testOffset(Integer data, boolean result) {
        TestTerminableQueue testQueue = new TestTerminableQueue();
        stub(testQueue.mockObj().put(any(CancellationToken.class), any(Integer.class), anyInt(), any(TimeUnit.class)))
                .toReturn(result);

        assertEquals("offer", result, testQueue.offer(data));

        verify(testQueue.mockObj()).put(
                argThat(cancellationToken(false)),
                same(data),
                eq(0L),
                notNull(TimeUnit.class)
        );
        verifyNoMoreInteractions(testQueue.mockObj());
    }

    @Test
    public void testOffer() {
        testOffset(324675, true);
        testOffset(324675, false);
    }

    private void testTryTakeButKeepReserved(ReservedElementRef<Integer> elementRef) {
        TestTerminableQueue testQueue = new TestTerminableQueue();
        stub(testQueue.mockObj().tryTakeButKeepReserved(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(elementRef);

        assertSame("tryTakeButKeepReserved", elementRef, testQueue.tryTakeButKeepReserved());

        verify(testQueue.mockObj()).tryTakeButKeepReserved(
                argThat(cancellationToken(false)),
                eq(0L),
                notNull(TimeUnit.class)
        );
        verifyNoMoreInteractions(testQueue.mockObj());
    }

    @Test
    public void testTryTakeButKeepReserved() {
        @SuppressWarnings("unchecked")
        ReservedElementRef<Integer> elementRef = (ReservedElementRef<Integer>) mock(ReservedElementRef.class);

        testTryTakeButKeepReserved(elementRef);
        verifyZeroInteractions(elementRef);
    }

    private void testTryTake0(Integer data) {
        @SuppressWarnings("unchecked")
        ReservedElementRef<Integer> elementRef = (ReservedElementRef<Integer>) mock(ReservedElementRef.class);

        stub(elementRef.element())
                .toReturn(data);

        // FIXME: We should test the call to tryTakeButKeepReserved() instead, since that is the defined behaviour.
        TestTerminableQueue testQueue = new TestTerminableQueue();
        stub(testQueue.mockObj().tryTakeButKeepReserved(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(elementRef);

        assertEquals("tryTake", data, testQueue.tryTake());

        verify(elementRef, times(1)).element();
        verify(elementRef, times(1)).release();
        verifyNoMoreInteractions(elementRef);

        verify(testQueue.mockObj()).tryTakeButKeepReserved(
                argThat(cancellationToken(false)),
                eq(0L),
                notNull(TimeUnit.class)
        );
        verifyNoMoreInteractions(testQueue.mockObj());
    }

    @Test
    public void testTryTake0() {
        testTryTake0(654265);
    }

    @Test
    public void testTryTake3() {
        CancellationToken cancelToken = mock(CancellationToken.class);
        long timeout = 364365544;
        TimeUnit timeoutUnit = TimeUnit.MICROSECONDS;

        @SuppressWarnings("unchecked")
        ReservedElementRef<Integer> elementRef = (ReservedElementRef<Integer>) mock(ReservedElementRef.class);

        TestTerminableQueue testQueue = new TestTerminableQueue();
        stub(testQueue.mockObj().tryTakeButKeepReserved(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(elementRef);

        assertSame("tryTakeButKeepReserved",
                elementRef,
                testQueue.tryTakeButKeepReserved(cancelToken, timeout, timeoutUnit));

        verifyZeroInteractions(cancelToken);
        verifyZeroInteractions(elementRef);

        verify(testQueue.mockObj()).tryTakeButKeepReserved(
                same(cancelToken),
                eq(timeout),
                same(timeoutUnit)
        );
        verifyNoMoreInteractions(testQueue.mockObj());
    }

    @Test
    public void testTakeButKeepReserved1() {
        CancellationToken cancelToken = mock(CancellationToken.class);

        @SuppressWarnings("unchecked")
        ReservedElementRef<Integer> elementRef = (ReservedElementRef<Integer>) mock(ReservedElementRef.class);

        TestTerminableQueue testQueue = new TestTerminableQueue();
        stub(testQueue.mockObj().tryTakeButKeepReserved(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(null)
                .toReturn(elementRef);

        assertSame("takeButKeepReserved", elementRef, testQueue.takeButKeepReserved(cancelToken));

        verifyZeroInteractions(cancelToken);
        verifyZeroInteractions(elementRef);

        verify(testQueue.mockObj(), times(2)).tryTakeButKeepReserved(
                same(cancelToken),
                eq(Long.MAX_VALUE),
                notNull(TimeUnit.class)
        );
        verifyNoMoreInteractions(testQueue.mockObj());
    }

    private void testTake1(Integer data) {
        CancellationToken cancelToken = mock(CancellationToken.class);

        @SuppressWarnings("unchecked")
        ReservedElementRef<Integer> elementRef = (ReservedElementRef<Integer>) mock(ReservedElementRef.class);

        stub(elementRef.element())
                .toReturn(data);

        // FIXME: We should test the call to takeButKeepReserved(CancellationToken) instead,
        //        since that is the defined behaviour.
        TestTerminableQueue testQueue = new TestTerminableQueue();
        stub(testQueue.mockObj().tryTakeButKeepReserved(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                .toReturn(null)
                .toReturn(elementRef);

        assertEquals("take", data, testQueue.take(cancelToken));

        verifyZeroInteractions(cancelToken);

        verify(elementRef, times(1)).element();
        verify(elementRef, times(1)).release();
        verifyNoMoreInteractions(elementRef);

        verify(testQueue.mockObj(), times(2)).tryTakeButKeepReserved(
                same(cancelToken),
                eq(Long.MAX_VALUE),
                notNull(TimeUnit.class)
        );
        verifyNoMoreInteractions(testQueue.mockObj());
    }

    @Test
    public void testTake1() {
        testTake1(654265);
    }

    private static final class TestTerminableQueue implements TerminableQueue<Integer> {
        private final TerminableQueue<Integer> mockObj;

        @SuppressWarnings("unchecked")
        public TestTerminableQueue() {
            this.mockObj = (TerminableQueue<Integer>) mock(TerminableQueue.class);
        }

        public TerminableQueue<Integer> mockObj() {
            return mockObj;
        }

        @Override
        public void put(CancellationToken cancelToken, Integer entry) {
            mockObj.put(cancelToken, entry);
        }

        @Override
        public boolean put(CancellationToken cancelToken, Integer entry, long timeout, TimeUnit timeoutUnit) {
            return mockObj.put(cancelToken, entry, timeout, timeoutUnit);
        }

        @Override
        public ReservedElementRef<Integer> tryTakeButKeepReserved(
                CancellationToken cancelToken,
                long timeout,
                TimeUnit timeoutUnit) {

            return mockObj.tryTakeButKeepReserved(cancelToken, timeout, timeoutUnit);
        }

        @Override
        public void shutdown() {
            mockObj.shutdown();
        }

        @Override
        public void shutdownAndWaitUntilEmpty(CancellationToken cancelToken) {
            mockObj.shutdownAndWaitUntilEmpty(cancelToken);
        }

        @Override
        public boolean shutdownAndTryWaitUntilEmpty(
                CancellationToken cancelToken,
                long timeout,
                TimeUnit timeoutUnit) {

            return mockObj.shutdownAndTryWaitUntilEmpty(cancelToken, timeout, timeoutUnit);
        }
    }
}
