package org.jtrim2.concurrent.query;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cache.GenericReference;
import org.jtrim2.cache.JavaRefObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncLinksTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AsyncLinks.class);
    }

    // Only minimal test, real tests can be found in the test code of the
    // respective class.

    @SuppressWarnings("unchecked")
    private static <DataType> DataInterceptor<DataType> mockInterceptor() {
        return mock(DataInterceptor.class);
    }

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockDataConverter() {
        return mock(DataConverter.class);
    }

    @SuppressWarnings("unchecked")
    private static <DataType> AsyncStateReporter<DataType> mockAsyncStateReporter() {
        return mock(AsyncStateReporter.class);
    }

    /**
     * Test of interceptData method, of class AsyncLinks.
     */
    @Test
    public void testInterceptData() {
        AsyncDataLink<Object> link = AsyncLinks.interceptData(mockLink(), mockInterceptor());
        assertTrue(link instanceof DataInterceptorLink);
    }

    /**
     * Test of convertResultSync method, of class AsyncLinks.
     */
    @Test
    public void testConvertResultSync_AsyncDataLink_DataConverter() {
        AsyncDataLink<Object> link = AsyncLinks.convertResultSync(mockLink(), mockDataConverter());
        assertTrue(link instanceof AsyncDataLinkConverter);
    }

    /**
     * Test of convertResult method, of class AsyncLinks.
     */
    @Test
    public void testConvertResultAsync_AsyncDataLink_AsyncDataQuery() {
        AsyncDataLink<Object> link = AsyncLinks.convertResultAsync(mockLink(), mockQuery());
        assertTrue(link instanceof LinkedAsyncDataLink);
    }

    /**
     * Test of extractCachedResult method, of class AsyncLinks.
     */
    @Test
    public void testExtractCachedResult() {
        ManualDataLink<RefCachedData<Object>> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<Object> link = AsyncLinks.extractCachedResult(wrappedLink);
        AsyncDataListener<Object> listener = mockListener();

        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object data = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(new RefCachedData<>(data, GenericReference.getNoReference()));
        wrappedLink.onDoneReceive(report);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(data));
        inOrder.verify(listener).onDoneReceive(same(report));
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test of removeUidFromResult method, of class AsyncLinks.
     */
    @Test
    public void testRemoveUidFromResult() {
        ManualDataLink<DataWithUid<Object>> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<Object> link = AsyncLinks.removeUidFromResult(wrappedLink);
        AsyncDataListener<Object> listener = mockListener();

        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object data = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(new DataWithUid<>(data, new Object()));
        wrappedLink.onDoneReceive(report);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(data));
        inOrder.verify(listener).onDoneReceive(same(report));
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test of markResultWithUid method, of class AsyncLinks.
     */
    @Test
    public void testMarkResultWithUid() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<DataWithUid<Object>> link = AsyncLinks.markResultWithUid(wrappedLink);
        CollectListener<DataWithUid<Object>> listener = new CollectListener<>();

        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object[] datas = new Object[]{new Object(), new Object()};
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(datas[0]);
        wrappedLink.onDataArrive(datas[1]);
        wrappedLink.onDoneReceive(report);

        DataWithUid<?>[] received = listener.getResults().toArray(new DataWithUid<?>[0]);
        assertEquals(datas.length, received.length);
        assertSame(datas[0], received[0].getData());
        assertSame(datas[1], received[1].getData());

        assertNotSame(datas[0], received[0].getID());
        assertNotSame(datas[1], received[0].getID());
        assertNotSame(datas[0], received[1].getID());
        assertNotSame(datas[1], received[1].getID());
        assertNotSame(received[0].getID(), received[1].getID());

        assertSame(report, listener.getReport());
        assertNull(listener.getMiscError());
    }

    /**
     * Test of cacheResult method, of class AsyncLinks.
     */
    @Test
    public void testCacheResult_3args() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<Object> link = AsyncLinks.cacheResult(
                wrappedLink, ReferenceType.HardRefType, JavaRefObjectCache.INSTANCE);

        AsyncDataListener<Object> listener1 = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        Object data = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(data);
        wrappedLink.onDoneReceive(report);

        AsyncDataListener<Object> listener2 = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        InOrder inOrder = inOrder(listener1, listener2);
        inOrder.verify(listener1).onDataArrive(same(data));
        inOrder.verify(listener1).onDoneReceive(same(report));
        inOrder.verify(listener2).onDataArrive(same(data));
        inOrder.verify(listener2).onDoneReceive(same(report));
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test of cacheResult method, of class AsyncLinks.
     */
    @Test
    public void testCacheResult_5args() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<Object> link = AsyncLinks.cacheResult(
                wrappedLink,
                ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE,
                1L,
                TimeUnit.DAYS);

        AsyncDataListener<Object> listener1 = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        Object data = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(data);
        wrappedLink.onDoneReceive(report);

        AsyncDataListener<Object> listener2 = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        InOrder inOrder = inOrder(listener1, listener2);
        inOrder.verify(listener1).onDataArrive(same(data));
        inOrder.verify(listener1).onDoneReceive(same(report));
        inOrder.verify(listener2).onDataArrive(same(data));
        inOrder.verify(listener2).onDoneReceive(same(report));
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test of refCacheResult method, of class AsyncLinks.
     */
    @Test
    public void testRefCacheResult_3args() {
        AsyncDataLink<?> link = AsyncLinks.refCacheResult(
                mockLink(), ReferenceType.HardRefType, JavaRefObjectCache.INSTANCE);
        assertTrue(link instanceof RefCachedDataLink);
    }

    /**
     * Test of refCacheResult method, of class AsyncLinks.
     */
    @Test
    public void testRefCacheResult_5args() {
        AsyncDataLink<?> link = AsyncLinks.refCacheResult(
                mockLink(),
                ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE,
                1L,
                TimeUnit.DAYS);
        assertTrue(link instanceof RefCachedDataLink);
    }

    private static <InputType, ResultType> AsyncDataConverter<InputType, ResultType> createDummyAsyncConverter() {
        return new AsyncDataConverter<>(
                AsyncLinksTest.<InputType, ResultType>mockDataConverter(),
                mock(TaskExecutorService.class));
    }

    /**
     * Test of convertGradually method, of class AsyncLinks.
     */
    @Test
    public void testConvertGradually() {
        AsyncDataLink<Object> link = AsyncLinks.convertGradually(
                new Object(),
                Arrays.asList(createDummyAsyncConverter(), createDummyAsyncConverter()));
        assertTrue(link instanceof ImproverTasksLink);
    }

    /**
     * Test of createStateReporterLink method, of class AsyncLinks.
     */
    @Test
    public void testCreateStateReporterLink_4args() {
        AsyncDataLink<Object> link = AsyncLinks.createStateReporterLink(
                mockLink(),
                mockAsyncStateReporter(),
                1L,
                TimeUnit.DAYS);
        assertTrue(link instanceof PeriodicStateReporterLink);
    }

    /**
     * Test of createStateReporterLink method, of class AsyncLinks.
     */
    @Test
    public void testCreateStateReporterLink_5args() {
        AsyncDataLink<Object> link = AsyncLinks.createStateReporterLink(
                mock(UpdateTaskExecutor.class),
                mockLink(),
                mockAsyncStateReporter(),
                1L,
                TimeUnit.DAYS);
        assertTrue(link instanceof PeriodicStateReporterLink);
    }

    /**
     * Test of createPreparedLink method, of class AsyncLinks.
     */
    @Test
    public void testCreatePreparedLink() {
        AsyncDataLink<Object> link = AsyncLinks.createPreparedLink(new Object(), mock(AsyncDataState.class));
        assertTrue(link instanceof PreparedDataLink);
    }
}
