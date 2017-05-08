package org.jtrim2.concurrent.async;

import java.util.Arrays;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LinkedAsyncDataLinkTest {
    private static MultiAsyncDataState getMultiState(AsyncDataController controller) {
        return (MultiAsyncDataState)controller.getDataState();
    }

    private static void verifyMultiState(AsyncDataController controller, AsyncDataState... states) {
        MultiAsyncDataState state = getMultiState(controller);

        AsyncDataState[] subStates = state.getSubStates();
        assertEquals(states.length, subStates.length);
        for (int i = 0; i < states.length; i++) {
            assertSame("States at position " + i + " must be the same.", states[i], subStates[i]);
        }
    }

    @Test
    public void testCancellation() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>();

        stub(wrappedQuery.createDataLink(any()))
                .toReturn(wrappedLink1);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        LinkedAsyncDataLink<Object> link = new LinkedAsyncDataLink<>(wrappedLink, wrappedQuery);
        link.getData(cancelSource.getToken(), wrappedListener);

        Object input1 = new Object();
        Object output1 = new Object();

        wrappedLink.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        wrappedLink1.onDataArrive(output1);

        assertFalse(wrappedLink1.hasLastRequestBeenCanceled());
        assertFalse(wrappedLink.hasLastRequestBeenCanceled());
        cancelSource.getController().cancel();
        assertTrue(wrappedLink1.hasLastRequestBeenCanceled());
        assertTrue(wrappedLink.hasLastRequestBeenCanceled());

        wrappedLink1.onDoneReceive(AsyncReport.CANCELED);

        wrappedLink.onDataArrive(new Object());
        // The listener must not create a new data link for performance reasons.
        verifyNoMoreInteractions(wrappedQuery);

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);
        InOrder inOrder = inOrder(wrappedListener);
        inOrder.verify(wrappedListener).onDataArrive(same(output1));
        inOrder.verify(wrappedListener).onDoneReceive(receivedReport.capture());
        inOrder.verifyNoMoreInteractions();

        assertNull(receivedReport.getValue().getException());
        assertTrue(receivedReport.getValue().isCanceled());
    }

    @Test
    public void testControlArgs() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>();
        ManualDataLink<Object> wrappedLink2 = new ManualDataLink<>();

        stub(wrappedQuery.createDataLink(any()))
                .toReturn(wrappedLink1)
                .toReturn(wrappedLink2);

        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        LinkedAsyncDataLink<Object> link = new LinkedAsyncDataLink<>(wrappedLink, wrappedQuery);
        AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, wrappedListener);

        Object input1 = new Object();
        Object input2 = new Object();

        Object linkControlArg1 = new Object();
        Object linkControlArg2 = new Object();
        Object linkControlArg3 = new Object();
        Object linkControlArg4 = new Object();
        Object queryControlArg1 = new Object();
        Object queryControlArg2 = new Object();
        Object queryControlArg3 = new Object();
        Object queryControlArg4 = new Object();
        Object plainControlArg1 = new Object();
        Object plainControlArg2 = new Object();
        Object plainControlArg3 = new Object();
        Object plainControlArg4 = new Object();

        controller.controlData(plainControlArg1);
        controller.controlData(new LinkedDataControl(linkControlArg1, queryControlArg1));

        wrappedLink.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        controller.controlData(plainControlArg2);
        controller.controlData(new LinkedDataControl(linkControlArg2, queryControlArg2));

        wrappedLink1.onDataArrive(new Object());
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);

        controller.controlData(plainControlArg3);
        controller.controlData(new LinkedDataControl(linkControlArg3, queryControlArg3));

        wrappedLink.onDataArrive(input2);
        verify(wrappedQuery).createDataLink(same(input2));

        controller.controlData(plainControlArg4);
        controller.controlData(new LinkedDataControl(linkControlArg4, queryControlArg4));

        wrappedLink2.onDataArrive(new Object());
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertArrayEquals(
                new Object[]{queryControlArg1, queryControlArg2, queryControlArg3},
                wrappedLink1.getReceivedControlArgs().toArray());
        assertArrayEquals(
                new Object[]{queryControlArg4},
                wrappedLink2.getReceivedControlArgs().toArray());
        assertArrayEquals(
                new Object[]{plainControlArg1, linkControlArg1, plainControlArg2,
                    linkControlArg2, plainControlArg3, linkControlArg3,
                    plainControlArg4, linkControlArg4},
                wrappedLink.getReceivedControlArgs().toArray());
    }

    @Test
    public void testAsyncStates() {
        AsyncDataState initialState = mock(AsyncDataState.class);
        AsyncDataState state1 = mock(AsyncDataState.class);
        AsyncDataState state2Part1 = mock(AsyncDataState.class);
        AsyncDataState state2Part2 = mock(AsyncDataState.class);
        AsyncDataState state2 = new MultiAsyncDataState(state2Part1, state2Part2);

        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>(state1);
        ManualDataLink<Object> wrappedLink2 = new ManualDataLink<>(state2);

        stub(wrappedQuery.createDataLink(any()))
                .toReturn(wrappedLink1)
                .toReturn(wrappedLink2);

        ManualDataLink<Object> wrappedLink = new ManualDataLink<>(initialState);
        LinkedAsyncDataLink<Object> link = new LinkedAsyncDataLink<>(wrappedLink, wrappedQuery);
        AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, wrappedListener);

        verifyMultiState(controller, initialState, null);

        Object input1 = new Object();
        Object input2 = new Object();

        wrappedLink.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        verifyMultiState(controller, initialState, state1);
        wrappedLink1.onDataArrive(new Object());

        verifyMultiState(controller, initialState, state1);
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);

        verifyMultiState(controller, initialState, state1);
        wrappedLink.onDataArrive(input2);
        verify(wrappedQuery).createDataLink(same(input2));

        verifyMultiState(controller, initialState, state2Part1, state2Part2);
        wrappedLink2.onDataArrive(new Object());
        verifyMultiState(controller, initialState, state2Part1, state2Part2);
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);
        verifyMultiState(controller, initialState, state2Part1, state2Part2);

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);
        verifyMultiState(controller, initialState, state2Part1, state2Part2);
    }

    @Test
    public void testLateQueryAnswer() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>();
        ManualDataLink<Object> wrappedLink2 = new ManualDataLink<>();

        stub(wrappedQuery.createDataLink(any()))
                .toReturn(wrappedLink1)
                .toReturn(wrappedLink2);

        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        LinkedAsyncDataLink<Object> link = new LinkedAsyncDataLink<>(wrappedLink, wrappedQuery);
        link.getData(Cancellation.UNCANCELABLE_TOKEN, wrappedListener);

        Object input1 = new Object();
        Object input2 = new Object();

        Object output1 = new Object();
        Object output2 = new Object();

        wrappedLink.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        wrappedLink1.onDataArrive(output1);

        wrappedLink.onDataArrive(input2);
        verify(wrappedQuery).createDataLink(same(input2));

        wrappedLink2.onDataArrive(output2);
        wrappedLink1.onDataArrive(new Object()); // Late data, should be ignored
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        ArgumentCaptor<Object> receivedDatas = ArgumentCaptor.forClass(Object.class);
        InOrder inOrder = inOrder(wrappedListener);
        inOrder.verify(wrappedListener, times(2)).onDataArrive(receivedDatas.capture());
        inOrder.verify(wrappedListener).onDoneReceive(any(AsyncReport.class));
        inOrder.verifyNoMoreInteractions();

        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{output1, output2}, receivedDatas.getAllValues().toArray());
    }

    @Test
    public void testSimpleConversion() {
        for (Exception exception: Arrays.asList(null, new Exception())) {
            for (boolean canceled: Arrays.asList(false, true)) {
                for (final Exception queryException: Arrays.asList(null, new Exception())) {
                    for (final boolean queryCanceled: Arrays.asList(false, true)) {
                        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
                        AsyncDataListener<Object> wrappedListener = mockListener();
                        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();

                        stub(wrappedQuery.createDataLink(any())).toAnswer((InvocationOnMock invocation) -> {
                            Object arg = invocation.getArguments()[0];
                            Object partial = new TestPartialData(arg);
                            Object converted = new ConvertedData(arg);
                            AsyncReport report = AsyncReport.getReport(queryException, queryCanceled);
                            return new PreparedLinkWithReport(report, partial, converted);
                        });

                        LinkedAsyncDataLink<Object> link = new LinkedAsyncDataLink<>(wrappedLink, wrappedQuery);
                        link.getData(Cancellation.UNCANCELABLE_TOKEN, wrappedListener);

                        Object[] datas = new Object[]{new Object(), new Object(), new Object()};

                        for (Object data: datas) {
                            wrappedLink.onDataArrive(data);
                        }
                        wrappedLink.onDoneReceive(AsyncReport.getReport(exception, canceled));

                        Object[] expected = new Object[datas.length * 2];
                        for (int i = 0; i < datas.length; i++) {
                            expected[2 * i] = new TestPartialData(datas[i]);
                            expected[2 * i + 1] = new ConvertedData(datas[i]);
                        }

                        ArgumentCaptor<Object> receivedDatas = ArgumentCaptor.forClass(Object.class);
                        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);

                        InOrder inOrder = inOrder(wrappedListener);
                        inOrder.verify(wrappedListener, times(expected.length)).onDataArrive(receivedDatas.capture());
                        inOrder.verify(wrappedListener).onDoneReceive(receivedReport.capture());
                        inOrder.verifyNoMoreInteractions();

                        assertArrayEquals(expected, receivedDatas.getAllValues().toArray());

                        Throwable receviedException = receivedReport.getValue().getException();
                        if (exception == null) {
                            assertSame(queryException, receviedException);
                        }
                        else if (queryException == null) {
                            assertSame(exception, receviedException);
                        }
                        else {
                            Throwable[] suppressed = receviedException.getSuppressed();
                            assertTrue((suppressed[0] == exception && suppressed[1] == queryException)
                                    || (suppressed[1] == exception && suppressed[0] == queryException));
                        }
                        assertEquals(canceled || queryCanceled, receivedReport.getValue().isCanceled());
                    }
                }
            }
        }
    }

    /**
     * Test of toString method, of class LinkedAsyncDataLink.
     */
    @Test
    public void testToString() {
        LinkedAsyncDataLink<Object> link = new LinkedAsyncDataLink<>(mockLink(), mockQuery());
        assertNotNull(link.toString());
    }

    private static class PreparedLinkWithReport implements AsyncDataLink<Object> {
        private final Object[] datas;
        private final AsyncReport report;

        public PreparedLinkWithReport(AsyncReport report, Object... datas) {
            this.datas = datas.clone();
            this.report = report;
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                AsyncDataListener<? super Object> dataListener) {

            try {
                for (Object data: datas) {
                    dataListener.onDataArrive(data);
                }
            } finally {
                dataListener.onDoneReceive(report);
            }

            return new SimpleDataController();
        }
    }

    private static class TestPartialData {
        private final Object data;

        public TestPartialData(Object data) {
            this.data = data;
        }

        @Override
        public int hashCode() {
            return 123 + System.identityHashCode(data);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TestPartialData other = (TestPartialData)obj;
            return this.data == other.data;
        }
    }

    private static class ConvertedData {
        private final Object data;

        public ConvertedData(Object data) {
            this.data = data;
        }

        @Override
        public int hashCode() {
            return 123 + System.identityHashCode(data);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ConvertedData other = (ConvertedData)obj;
            return this.data == other.data;
        }
    }
}
