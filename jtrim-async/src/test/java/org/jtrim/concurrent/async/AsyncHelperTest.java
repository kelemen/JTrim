package org.jtrim.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.jtrim.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncHelperTest {
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

    @Test(expected = NullPointerException.class)
    public void testGetTransferExceptionIllegal1() {
        AsyncHelper.getTransferException((Throwable[])null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetTransferExceptionIllegal2() {
        AsyncHelper.getTransferException((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetTransferExceptionIllegal3() {
        AsyncHelper.getTransferException(new Exception(), null);
    }

    @Test
    public void testGetTransferExceptionNoException() {
        assertNull(AsyncHelper.getTransferException());
    }

    @Test
    public void testGetTransferExceptionSingleException() {
        Exception exception = new Exception();
        assertSame(exception, AsyncHelper.getTransferException(exception).getCause());
    }

    @Test
    public void testGetTransferExceptionSingleTransferException() {
        Throwable exception = new DataTransferException();
        assertSame(exception, AsyncHelper.getTransferException(exception));
    }

    @Test
    public void testGetTransferExceptionMultipleExceptions() {
        Throwable[] exceptions = new Throwable[]{new Exception(), new Exception(), new Exception()};

        DataTransferException transferException = AsyncHelper.getTransferException(exceptions);
        assertArrayEquals(exceptions, transferException.getSuppressed());
    }

    /**
     * Test of makeSafeOrderedListener method, of class AsyncHelper.
     */
    @Test
    public void testMakeSafeOrderedListener() {
        AsyncDataListener<OrderedData<Object>> orderedListener
                = AsyncHelper.makeSafeOrderedListener(mockListener());
        assertTrue(orderedListener instanceof SafeDataListener);
    }

    @Test(expected = NullPointerException.class)
    public void testMakeSafeOrderedListenerIllegal() {
        AsyncHelper.makeSafeOrderedListener(null);
    }

    /**
     * Test of makeSafeListener method, of class AsyncHelper.
     */
    @Test
    public void testMakeSafeListener() {
        AsyncDataListener<Object> wrappedListener = mockListener();
        AsyncDataListener<Object> safeListener = AsyncHelper.makeSafeListener(wrappedListener);

        Object[] datas = new Object[]{new Object(), new Object(), new Object()};
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        for (Object data: datas) {
            safeListener.onDataArrive(data);
        }
        safeListener.onDoneReceive(report);

        safeListener.onDataArrive(new Object());
        safeListener.onDoneReceive(AsyncReport.getReport(new RuntimeException(), true));

        ArgumentCaptor<Object> receviedArgs = ArgumentCaptor.forClass(Object.class);

        InOrder inOrder = inOrder(wrappedListener);
        inOrder.verify(wrappedListener, times(datas.length)).onDataArrive(receviedArgs.capture());
        inOrder.verify(wrappedListener).onDoneReceive(same(report));
        inOrder.verifyNoMoreInteractions();

        assertArrayEquals(datas, receviedArgs.getAllValues().toArray());
    }

    @Test
    public void testMakeSafeListenerAlreadySafe1() {
        AsyncDataListener<Object> wrappedListener = new AsyncDataListenerConverter<>(
                AsyncHelper.makeSafeListener(mockListener()),
                new MarkWithIDConverter<>());
        AsyncDataListener<?> safeListener = AsyncHelper.makeSafeListener(wrappedListener);

        assertSame(wrappedListener, safeListener);
    }

    @Test
    public void testMakeSafeListenerAlreadySafe2() {
        AsyncDataListener<RefCachedData<? extends Object>> wrappedListener = new AsyncDataListenerConverter<>(
                AsyncHelper.makeSafeListener(AsyncMocks.mockListener()),
                new CachedDataExtractor<>());
        AsyncDataListener<?> safeListener = AsyncHelper.makeSafeListener(wrappedListener);

        assertSame(wrappedListener, safeListener);
    }

    @Test
    public void testMakeSafeListenerAlreadySafe3() {
        AsyncDataListener<DataWithUid<? extends Object>> wrappedListener = new AsyncDataListenerConverter<>(
                AsyncHelper.makeSafeListener(AsyncMocks.mockListener()),
                new DataIDRemover<>());
        AsyncDataListener<?> safeListener = AsyncHelper.makeSafeListener(wrappedListener);

        assertSame(wrappedListener, safeListener);
    }

    @Test(expected = NullPointerException.class)
    public void testMakeSafeListenerIllegal() {
        AsyncHelper.makeSafeListener(null);
    }
}
