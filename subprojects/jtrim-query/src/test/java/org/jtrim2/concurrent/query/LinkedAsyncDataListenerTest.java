package org.jtrim2.concurrent.query;

import java.util.Arrays;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class LinkedAsyncDataListenerTest {
    private static <DataType, SourceDataType> LinkedAsyncDataListener<DataType> create(
            CancellationToken cancelToken,
            AsyncDataState firstState,
            AsyncDataQuery<? super DataType, ? extends SourceDataType> query,
            AsyncDataListener<? super SourceDataType> outputListener) {
        return new LinkedAsyncDataListener<>(cancelToken, firstState, query, outputListener);
    }

    private static void verifyOnDoneReceive(AsyncDataListener<?> mock, AsyncReport expected) {
        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);
        verify(mock).onDoneReceive(receivedReport.capture());

        AsyncReport actual = receivedReport.getValue();

        assertEquals(expected.isCanceled(), actual.isCanceled());
        assertSame(expected.getException(), actual.getException());
    }

    @Test
    public void testNoDataProvided() {
        AsyncReport[] reports = new AsyncReport[]{
            AsyncReport.SUCCESS,
            AsyncReport.CANCELED,
            AsyncReport.getReport(new RuntimeException(), false),
            AsyncReport.getReport(new RuntimeException(), true),
        };

        for (AsyncReport report: reports) {
            AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
            AsyncDataListener<Object> wrappedListener = mockListener();

            CancellationSource cancelSource = Cancellation.createCancellationSource();
            LinkedAsyncDataListener<Object> listener = create(
                    cancelSource.getToken(),
                    mock(AsyncDataState.class),
                    wrappedQuery,
                    wrappedListener);

            listener.onDoneReceive(report);
            verifyOnDoneReceive(wrappedListener, report);

            verifyZeroInteractions(wrappedQuery);
        }
    }

    @Test
    public void testQueryCreateDataLinkFailure() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        Throwable failure = new RuntimeException("testQueryCreateDataLinkFailure.TestFailure");
        stub(wrappedQuery.createDataLink(any())).toThrow(failure);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        LinkedAsyncDataListener<Object> listener = create(
                cancelSource.getToken(),
                mock(AsyncDataState.class),
                wrappedQuery,
                wrappedListener);

        try {
            listener.onDataArrive(new Object());
        } catch (Throwable ex) {
            assertSame(failure, ex);
        }

        listener.onDoneReceive(AsyncReport.SUCCESS);
        verifyOnDoneReceive(wrappedListener, AsyncReport.getReport(failure, false));
    }

    @Test
    public void testCancellation() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>();

        stub(wrappedQuery.createDataLink(any()))
                .toReturn(wrappedLink1);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        LinkedAsyncDataListener<Object> listener = create(
                cancelSource.getToken(),
                mock(AsyncDataState.class),
                wrappedQuery,
                wrappedListener);

        Object input1 = new Object();
        Object output1 = new Object();

        listener.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        wrappedLink1.onDataArrive(output1);

        assertFalse(wrappedLink1.hasLastRequestBeenCanceled());
        cancelSource.getController().cancel();
        assertTrue(wrappedLink1.hasLastRequestBeenCanceled());

        wrappedLink1.onDoneReceive(AsyncReport.CANCELED);

        listener.onDataArrive(new Object());
        // The listener must not create a new data link for performance reasons.
        verifyNoMoreInteractions(wrappedQuery);

        listener.onDoneReceive(AsyncReport.SUCCESS);

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

        LinkedAsyncDataListener<Object> listener = create(
                Cancellation.UNCANCELABLE_TOKEN,
                mock(AsyncDataState.class),
                wrappedQuery,
                wrappedListener);

        Object input1 = new Object();
        Object input2 = new Object();

        Object controlArg1 = new Object();
        Object controlArg2 = new Object();
        Object controlArg3 = new Object();
        Object controlArg4 = new Object();

        listener.controlData(controlArg1);

        listener.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        listener.controlData(controlArg2);

        wrappedLink1.onDataArrive(new Object());
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);

        listener.controlData(controlArg3);

        listener.onDataArrive(input2);
        verify(wrappedQuery).createDataLink(same(input2));

        listener.controlData(controlArg4);

        wrappedLink2.onDataArrive(new Object());
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);

        listener.onDoneReceive(AsyncReport.SUCCESS);

        assertArrayEquals(
                new Object[]{controlArg1, controlArg2, controlArg3},
                wrappedLink1.getReceivedControlArgs().toArray());
        assertArrayEquals(new Object[]{controlArg4}, wrappedLink2.getReceivedControlArgs().toArray());
    }

    @Test
    public void testAsyncStates() {
        AsyncDataState initialState = mock(AsyncDataState.class);
        AsyncDataState state1 = mock(AsyncDataState.class);
        AsyncDataState state2 = mock(AsyncDataState.class);

        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        AsyncDataListener<Object> wrappedListener = mockListener();

        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>(state1);
        ManualDataLink<Object> wrappedLink2 = new ManualDataLink<>(state2);

        stub(wrappedQuery.createDataLink(any()))
                .toReturn(wrappedLink1)
                .toReturn(wrappedLink2);

        LinkedAsyncDataListener<Object> listener = create(
                Cancellation.UNCANCELABLE_TOKEN,
                initialState,
                wrappedQuery,
                wrappedListener);

        assertSame(initialState, listener.getDataState());

        Object input1 = new Object();
        Object input2 = new Object();

        listener.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        assertSame(state1, listener.getDataState());
        wrappedLink1.onDataArrive(new Object());

        assertSame(state1, listener.getDataState());
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);

        assertSame(state1, listener.getDataState());
        listener.onDataArrive(input2);
        verify(wrappedQuery).createDataLink(same(input2));

        assertSame(state2, listener.getDataState());
        wrappedLink2.onDataArrive(new Object());
        assertSame(state2, listener.getDataState());
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);
        assertSame(state2, listener.getDataState());

        listener.onDoneReceive(AsyncReport.SUCCESS);
        assertSame(state2, listener.getDataState());
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

        LinkedAsyncDataListener<Object> listener = create(
                Cancellation.UNCANCELABLE_TOKEN,
                mock(AsyncDataState.class),
                wrappedQuery,
                wrappedListener);

        Object input1 = new Object();
        Object input2 = new Object();

        Object output1 = new Object();
        Object output2 = new Object();

        listener.onDataArrive(input1);
        verify(wrappedQuery).createDataLink(same(input1));

        wrappedLink1.onDataArrive(output1);

        listener.onDataArrive(input2);
        verify(wrappedQuery).createDataLink(same(input2));

        wrappedLink2.onDataArrive(output2);
        wrappedLink1.onDataArrive(new Object()); // Late data, should be ignored
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);

        listener.onDoneReceive(AsyncReport.SUCCESS);

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

                        stub(wrappedQuery.createDataLink(any())).toAnswer((InvocationOnMock invocation) -> {
                            Object arg = invocation.getArguments()[0];
                            Object partial = new TestPartialData(arg);
                            Object converted = new ConvertedData(arg);
                            AsyncReport report = AsyncReport.getReport(queryException, queryCanceled);
                            return new PreparedLinkWithReport(report, partial, converted);
                        });

                        LinkedAsyncDataListener<Object> listener = create(
                                Cancellation.UNCANCELABLE_TOKEN,
                                mock(AsyncDataState.class),
                                wrappedQuery,
                                wrappedListener);

                        Object[] datas = new Object[]{new Object(), new Object(), new Object()};

                        for (Object data: datas) {
                            listener.onDataArrive(data);
                        }
                        listener.onDoneReceive(AsyncReport.getReport(exception, canceled));

                        Object[] expected = new Object[datas.length * 2];
                        for (int i = 0; i < datas.length; i++) {
                            expected[2 * i] = new TestPartialData(datas[i]);
                            expected[2 * i + 1] = new ConvertedData(datas[i]);
                        }

                        ArgumentCaptor<Object> receivedDatas
                                = ArgumentCaptor.forClass(Object.class);
                        ArgumentCaptor<AsyncReport> receivedReport
                                = ArgumentCaptor.forClass(AsyncReport.class);

                        InOrder inOrder = inOrder(wrappedListener);
                        inOrder.verify(wrappedListener, times(expected.length))
                                .onDataArrive(receivedDatas.capture());
                        inOrder.verify(wrappedListener).onDoneReceive(receivedReport.capture());
                        inOrder.verifyNoMoreInteractions();

                        assertArrayEquals(expected, receivedDatas.getAllValues().toArray());

                        Throwable receviedException = receivedReport.getValue().getException();
                        if (exception == null) {
                            assertSame(queryException, receviedException);
                        } else if (queryException == null) {
                            assertSame(exception, receviedException);
                        } else {
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
     * Test of toString method, of class LinkedAsyncDataListener.
     */
    @Test
    public void testToString() {
        LinkedAsyncDataListener<Object> listener = create(
                mock(CancellationToken.class),
                mock(AsyncDataState.class),
                mockQuery(),
                mockListener());
        assertNotNull(listener.toString());
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

            final TestPartialData other = (TestPartialData) obj;
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

            final ConvertedData other = (ConvertedData) obj;
            return this.data == other.data;
        }
    }
}
