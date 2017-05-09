package org.jtrim2.concurrent.query;

import java.util.concurrent.CountDownLatch;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class SafeDataListenerTest {
    @SuppressWarnings("unchecked")
    private static <DataType> AsyncDataListener<DataType> mockListener() {
        return mock(AsyncDataListener.class);
    }

    private static <DataType> SafeDataListener<DataType> create(AsyncDataListener<DataType> wrapped) {
        return new SafeDataListener<>(wrapped);
    }

    private static void forwardOrdered(SafeDataListener<Object> listener, Object... datas) {
        for (int i = 0; i < datas.length; i++) {
            listener.onDataArrive(new OrderedData<>(i, datas[i]));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor() {
        create(null);
    }

    @Test
    public void testSimpleForward() {
        CollectListener<Object> wrapped = new CollectListener<>();
        SafeDataListener<Object> listener = create(wrapped);

        Object[] datas = new Object[]{new Object(), new Object(), new Object()};
        forwardOrdered(listener, datas);

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        listener.onDoneReceive(report);

        assertSame(report, wrapped.getReport());
        assertArrayEquals(datas, wrapped.getResults().toArray());
        assertNull(wrapped.getMiscError());
    }

    @Test
    public void testWrongOrder() {
        CollectListener<Object> wrapped = new CollectListener<>();
        SafeDataListener<Object> listener = create(wrapped);

        Object[] datas = new Object[]{new Object(), new Object(), new Object()};
        forwardOrdered(listener, datas);
        for (int i = -1; i < datas.length; i++) {
            listener.onDataArrive(new OrderedData<>(i, new Object()));
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        listener.onDoneReceive(report);

        assertSame(report, wrapped.getReport());
        assertArrayEquals(datas, wrapped.getResults().toArray());
        assertNull(wrapped.getMiscError());
    }

    @Test
    public void testMultipleDone() {
        AsyncDataListener<Object> wrapped = mockListener();
        SafeDataListener<Object> listener = create(wrapped);

        AsyncReport report1 = AsyncReport.getReport(new Exception(), false);
        AsyncReport report2 = AsyncReport.getReport(new Exception(), false);

        listener.onDoneReceive(report1);
        listener.onDoneReceive(report2);

        verify(wrapped).onDoneReceive(same(report1));
        verifyNoMoreInteractions(wrapped);
    }

    @Test
    public void testDataAfterDone() {
        AsyncDataListener<Object> wrapped = mockListener();
        SafeDataListener<Object> listener = create(wrapped);

        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        listener.onDoneReceive(report);
        listener.onDataArrive(new OrderedData<>(0, new Object()));

        verify(wrapped).onDoneReceive(same(report));
        verifyNoMoreInteractions(wrapped);
    }

    @Test(timeout = 20000)
    public void testConcurrent() throws InterruptedException {
        int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
        int numberOfTests = 200;

        for (int testIndex = 0; testIndex < numberOfTests; testIndex++) {
            CollectListener<Object> wrapped = new CollectListener<>();
            final SafeDataListener<Object> listener = create(wrapped);

            final Object[] datas = new Object[]{new Object(), new Object(), new Object()};
            final AsyncReport report = AsyncReport.getReport(new Exception(), false);

            Thread[] threads = new Thread[numberOfThreads];
            final CountDownLatch startLatch = new CountDownLatch(numberOfThreads);

            try {
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(() -> {
                        startLatch.countDown();
                        try {
                            startLatch.await();
                        } catch (InterruptedException ex) {
                        }

                        forwardOrdered(listener, datas);
                        listener.onDoneReceive(report);
                    });
                    threads[i].start();
                }
            } finally {
                for (Thread thread: threads) {
                    if (thread != null) {
                        try {
                            thread.join();
                        } catch (Throwable ex) {
                            thread.interrupt();
                            throw ex;
                        }
                    }
                }
            }

            assertSame(report, wrapped.getReport());

            wrapped.checkValidCompleteResults(datas);
            assertNull(wrapped.getMiscError());
        }
    }

    /**
     * Test of toString method, of class SafeDataListener.
     */
    @Test
    public void testToString() {
        assertNotNull(create(mockListener()).toString());
    }
}
