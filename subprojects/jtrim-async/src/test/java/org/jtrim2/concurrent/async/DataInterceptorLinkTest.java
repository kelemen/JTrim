package org.jtrim2.concurrent.async;

import org.jtrim2.cancel.Cancellation;
import org.junit.Test;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DataInterceptorLinkTest {
    private static <DataType> DataInterceptorLink<DataType> create(
            AsyncDataLink<? extends DataType> wrappedLink,
            DataInterceptor<? super DataType> interceptor) {
        return new DataInterceptorLink<>(wrappedLink, interceptor);
    }

    @SuppressWarnings("unchecked")
    private static <DataType> DataInterceptor<DataType> mockInterceptor() {
        return mock(DataInterceptor.class);
    }

    @Test
    public void testInterceptAndForward() {
        AsyncDataListener<Object> listener = mockListener();
        DataInterceptor<Object> interceptor = mockInterceptor();
        AsyncDataState state = mock(AsyncDataState.class);
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>(state);

        stub(interceptor.onDataArrive(any())).toReturn(true);

        DataInterceptorLink<Object> link = create(wrappedLink, interceptor);

        AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object[] controlArgs = new Object[]{new Object(), new Object()};
        controller.controlData(controlArgs[0]);
        controller.controlData(controlArgs[1]);
        assertSame(state, controller.getDataState());
        assertArrayEquals(controlArgs, wrappedLink.getReceivedControlArgs().toArray());

        Object data = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(data);
        verify(listener).onDataArrive(same(data));
        verifyNoMoreInteractions(listener);

        wrappedLink.onDoneReceive(report);
        verify(listener).onDoneReceive(same(report));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testInterceptAndSkip() {
        CollectListener<Object> listener = new CollectListener<>();
        DataInterceptor<Object> interceptor = mockInterceptor();
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();

        stub(interceptor.onDataArrive(any()))
                .toReturn(true)
                .toReturn(false)
                .toReturn(true);

        DataInterceptorLink<Object> link = create(wrappedLink, interceptor);

        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object data1 = new TestObject("DATA1");
        Object data2 = new TestObject("DATA2");
        Object data3 = new TestObject("DATA3");

        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(data1);
        wrappedLink.onDataArrive(data2);
        wrappedLink.onDataArrive(data3);
        wrappedLink.onDoneReceive(report);

        listener.checkValidCompleteResults(new Object[]{data1, data3});
        assertSame(report, listener.getReport());
        assertNull(listener.getMiscError());
    }

    @Test
    public void testInterceptAndDoneFails() {
        AsyncDataListener<Object> listener = mockListener();
        DataInterceptor<Object> interceptor = mockInterceptor();
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();

        stub(interceptor.onDataArrive(any())).toReturn(true);
        doThrow(TestException.class).when(interceptor).onDoneReceive(any(AsyncReport.class));

        DataInterceptorLink<Object> link = create(wrappedLink, interceptor);

        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object data = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(data);
        try {
            wrappedLink.onDoneReceive(report);
        } catch (TestException ex) {
            // The current implementation simply rethrows the exception but
            // it is not necessary for it to do so.
        }

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(data));
        inOrder.verify(listener).onDoneReceive(same(report));
    }

    @Test
    public void testToString() {
        assertNotNull(create(mockLink(), mockInterceptor()).toString());
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -8178089854587758606L;
    }

    private static class TestObject {
        private final String str;

        public TestObject(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
