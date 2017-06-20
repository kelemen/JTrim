package org.jtrim2.concurrent.query;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class RefCachedDataLinkTest {
    private static Object[] concatArrays(Object[] array1, Object[] array2) {
        Object[] result = new Object[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    private <ResultType> void checkValidResults(
            CollectListener<RefCachedData<ResultType>> listener,
            ResultType[] expectedResults) {

        listener.checkValidResults(expectedResults, RefCachedData::getData);
    }

    private <ResultType> void checkValidCompleteResults(
            CollectListener<RefCachedData<ResultType>> listener,
            ResultType[] expectedResults) {

        listener.checkValidCompleteResults(expectedResults, RefCachedData::getData);
    }

    private static AsyncDataLink<RefCachedData<String>> create(
            AsyncDataLink<? extends String> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeUnit
            ) {

        return new RefCachedDataLink<>(
                wrappedDataLink,
                refType, refCreator,
                dataCancelTimeout, timeUnit);
    }

    @Test
    public void testSingleTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        for (String data: toSend) {
            wrappedLink.onDataArrive(data);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener.isCompleted());

        assertSame(report, listener.getReport());

        assertEquals(toSend.length, listener.getResults().size());
        checkValidCompleteResults(listener, toSend);

        assertNull(listener.getMiscError());
    }

    @Test
    public void testConurrentTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        for (String data: toSend) {
            wrappedLink.onDataArrive(data);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        assertEquals(toSend.length, listener1.getResults().size());
        assertEquals(toSend.length, listener2.getResults().size());

        checkValidCompleteResults(listener1, toSend);
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testAttachToTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        wrappedLink.onDataArrive(toSend[0]);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        for (int i = 1; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        // listener2 must have received the first data as well because
        // it must have been cached.
        assertEquals(toSend.length, listener1.getResults().size());
        assertEquals(toSend.length, listener2.getResults().size());

        checkValidCompleteResults(listener1, toSend);
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testDataState() {
        ManualCache cache = new ManualCache();
        AsyncDataState testState = new SimpleDataState("TEST-STATE", 0.5);
        ManualDataLink<String> wrappedLink = new ManualDataLink<>(testState);
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        @SuppressWarnings("unchecked")
        AsyncDataListener<RefCachedData<String>> listener1 = mock(AsyncDataListener.class);
        @SuppressWarnings("unchecked")
        AsyncDataListener<RefCachedData<String>> listener2 = mock(AsyncDataListener.class);
        @SuppressWarnings("unchecked")
        AsyncDataListener<RefCachedData<String>> listener3 = mock(AsyncDataListener.class);

        TestCancellationSource cancelSource = new TestCancellationSource();
        AsyncDataController controller1 = cachedLink.getData(cancelSource.getToken(), listener1);
        AsyncDataController controller2 = cachedLink.getData(cancelSource.getToken(), listener2);

        assertSame(testState, controller1.getDataState());
        assertSame(testState, controller2.getDataState());

        wrappedLink.onDataArrive("DATA1");

        AsyncDataController controller3 = cachedLink.getData(cancelSource.getToken(), listener3);
        assertSame(testState, controller1.getDataState());
        assertSame(testState, controller2.getDataState());
        assertSame(testState, controller3.getDataState());

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertSame(testState, controller1.getDataState());
        assertSame(testState, controller2.getDataState());
        assertSame(testState, controller3.getDataState());
    }

    @Test
    public void testErroneousListener() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();

        @SuppressWarnings("unchecked")
        AsyncDataListener<Object> errorListener = mock(AsyncDataListener.class);

        // Do not use "doThrow" because we have to throw a unique exception
        // in each call.
        doAnswer((InvocationOnMock invocation) -> {
            throw new TestException();
        }).when(errorListener).onDataArrive(any());
        doThrow(new TestException()).when(errorListener).onDoneReceive(any(AsyncReport.class));

        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, errorListener);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        try (LogCollector logs = LogTests.startCollecting()) {
            for (String data: toSend) {
                wrappedLink.onDataArrive(data);
            }
            wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

            LogTests.verifyLogCount(TestException.class, Level.SEVERE, toSend.length + 1, logs);
        }

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(AsyncReport.SUCCESS, listener1.getReport());
        assertSame(AsyncReport.SUCCESS, listener2.getReport());

        List<String> results1 = listener1.extractedResults(RefCachedData::getData);
        List<String> results2 = listener2.extractedResults(RefCachedData::getData);

        assertArrayEquals(toSend, results1.toArray());
        assertArrayEquals(toSend, results2.toArray());

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testRemoveCancellationListener() {
        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        @SuppressWarnings("unchecked")
        AsyncDataListener<RefCachedData<String>> listener = mock(AsyncDataListener.class);

        TestCancellationSource cancelSource = new TestCancellationSource();
        cachedLink.getData(cancelSource.getToken(), listener);

        wrappedLink.onDataArrive("DATA1");
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        cancelSource.checkNoRegistration();
    }

    @Test(timeout = 5000)
    public void testWaitForAutoCancellation() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 1L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();

        CancellationSource firstSource = Cancellation.createCancellationSource();
        cachedLink.getData(firstSource.getToken(), listener);

        wrappedLink.onDataArrive(toSend[0]);
        firstSource.getController().cancel();
        if (!listener.tryWaitCompletion(5000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Unexpected timeout.");
        }

        wrappedLink.onDataArrive(toSend[1]);
        wrappedLink.onDataArrive(toSend[2]);
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertTrue(listener.isCompleted());

        assertTrue(listener.getReport().isCanceled());

        List<String> results = listener.extractedResults(RefCachedData::getData);

        assertArrayEquals(new Object[]{toSend[0]}, results.toArray());

        assertNull(listener.getMiscError());
    }

    @Test
    public void testReAttachWhileWaitingForCancellation() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 1L, TimeUnit.DAYS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();

        CancellationSource firstSource = Cancellation.createCancellationSource();
        cachedLink.getData(firstSource.getToken(), listener1);

        wrappedLink.onDataArrive(toSend[0]);
        firstSource.getController().cancel();

        wrappedLink.onDataArrive(toSend[1]);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        wrappedLink.onDataArrive(toSend[2]);
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertTrue(listener1.getReport().isCanceled());
        assertSame(AsyncReport.SUCCESS, listener2.getReport());

        List<String> results1 = listener1.extractedResults(RefCachedData::getData);
        List<String> results2 = listener2.extractedResults(RefCachedData::getData);

        assertArrayEquals(new Object[]{toSend[0]}, results1.toArray());
        assertArrayEquals(new Object[]{toSend[1], toSend[2]}, results2.toArray());

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testAttachLaterToTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        wrappedLink.onDataArrive(toSend[0]);
        wrappedLink.onDataArrive(toSend[1]);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        for (int i = 2; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        assertEquals(toSend.length, listener1.getResults().size());
        assertEquals(toSend.length - 1, listener2.getResults().size());

        checkValidCompleteResults(listener1, toSend);
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    /**
     * Test the case when a new listener is attached to the data retrieval
     * process after the last has been sent but before onDoneReceive and the
     * cache also expired.
     */
    @Test
    public void testAttachTooLateToTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        for (String data : toSend) {
            wrappedLink.onDataArrive(data);
        }
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener1.isCompleted());
        assertFalse(listener2.isCompleted());

        assertEquals(0, listener2.getResults().size());

        assertSame(report, listener1.getReport());

        assertEquals(toSend.length, listener1.getResults().size());
        checkValidCompleteResults(listener1, toSend);

        for (String data : toSend) {
            wrappedLink.onDataArrive(data);
        }
        wrappedLink.onDoneReceive(report);

        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());

        assertEquals(toSend.length, listener2.getResults().size());
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testAttachBeforeDoneCachedToTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        for (String data : toSend) {
            wrappedLink.onDataArrive(data);
        }
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());
        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        assertEquals(toSend.length, listener1.getResults().size());
        assertEquals(1, listener2.getResults().size());

        checkValidCompleteResults(listener1, toSend);
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testAttachAfterDoneCachedToTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        for (String data : toSend) {
            wrappedLink.onDataArrive(data);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);
        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);
        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        assertEquals(toSend.length, listener1.getResults().size());
        assertEquals(1, listener2.getResults().size());

        checkValidCompleteResults(listener1, toSend);
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testAttachAfterDoneNotCachedToTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        for (String data : toSend) {
            wrappedLink.onDataArrive(data);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);
        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);
        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        assertTrue(listener1.isCompleted());
        assertFalse(listener2.isCompleted());

        assertEquals(0, listener2.getResults().size());

        assertSame(report, listener1.getReport());

        assertEquals(toSend.length, listener1.getResults().size());
        checkValidCompleteResults(listener1, toSend);

        for (String data : toSend) {
            wrappedLink.onDataArrive(data);
        }
        wrappedLink.onDoneReceive(report);

        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());

        assertEquals(toSend.length, listener2.getResults().size());
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testCancelAfterTimeout() {
        final int maxNumberOfTries = 7;
        long tolarenaceMs = 100;
        for (int i = 0; i < maxNumberOfTries; i++) {
            ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
            ObjectCache cache = ObjectCache.javaRefCache();
            AsyncDataLink<RefCachedData<Object>> cachedLink = new RefCachedDataLink<>(
                    wrappedLink, ReferenceType.HardRefType, cache, 1L, TimeUnit.NANOSECONDS);

            AsyncDataListener<Object> listener = AsyncMocks.mockListener();

            CancellationSource cancelSource = Cancellation.createCancellationSource();
            cachedLink.getData(cancelSource.getToken(), listener);
            cancelSource.getController().cancel();

            CancelableWaits.sleep(Cancellation.UNCANCELABLE_TOKEN, tolarenaceMs, TimeUnit.MILLISECONDS);

            if (wrappedLink.hasLastRequestBeenCanceled()) {
                return;
            }

            tolarenaceMs = tolarenaceMs * 2L;
        }

        fail("Cancellation did not happen after the timeout. Tried " + maxNumberOfTries + " times.");
    }

    @SuppressWarnings("unchecked")
    private <T> ArgumentCaptor<RefCachedData<T>> refCachedCaptor() {
        return (ArgumentCaptor<RefCachedData<T>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(RefCachedData.class);
    }

    @Test
    public void testRetrieveAfterCancellation() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        ObjectCache cache = ObjectCache.javaRefCache();
        AsyncDataLink<RefCachedData<Object>> cachedLink = new RefCachedDataLink<>(
                wrappedLink, ReferenceType.HardRefType, cache, 0L, TimeUnit.NANOSECONDS);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cachedLink.getData(cancelSource.getToken(), AsyncMocks.mockListener());

        wrappedLink.onDataArrive(new Object());
        cancelSource.getController().cancel();
        wrappedLink.onDataArrive(new Object());
        wrappedLink.onDataArrive(AsyncReport.CANCELED);

        AsyncDataListener<RefCachedData<Object>> listener = AsyncMocks.mockListener();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object completeData = new Object();
        wrappedLink.onDataArrive(completeData);
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RefCachedData<Object>> dataArgRef = refCachedCaptor();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(dataArgRef.capture());
        inOrder.verify(listener).onDoneReceive(AsyncReport.SUCCESS);
        inOrder.verifyNoMoreInteractions();

        assertSame(completeData, dataArgRef.getValue().getData());
    }

    @Test
    public void testSimpleCancellation() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cachedLink.getData(cancelSource.getToken(), listener1);

        wrappedLink.onDataArrive(toSend[0]);
        cancelSource.getController().cancel();

        // The RefCachedDataLink must be eager to cancel the current transfer.
        assertTrue(listener1.isCompleted());

        assertTrue(listener1.getReport().isCanceled());

        assertEquals(1, listener1.getResults().size());
        checkValidResults(listener1, toSend);

        wrappedLink.onDataArrive(toSend[1]);
        wrappedLink.onDoneReceive(AsyncReport.CANCELED);

        assertTrue(wrappedLink.hasLastRequestBeenCanceled());

        // Also test that the next data will be retrieved all right.
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        for (String data: toSend) {
            wrappedLink.onDataArrive(data);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener2.isCompleted());

        assertSame(report, listener2.getReport());

        assertEquals(toSend.length, listener2.getResults().size());
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testDoesNotAutoCancelAfterDone() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<Object>> cachedLink = new RefCachedDataLink<>(
                wrappedLink, ReferenceType.HardRefType, null, 500L, TimeUnit.MILLISECONDS);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        AsyncDataListener<RefCachedData<Object>> listener1 = AsyncMocks.mockListener();
        cachedLink.getData(cancelSource.getToken(), listener1);

        Object originalData = new Object();
        wrappedLink.onDataArrive(originalData);
        cancelSource.getController().cancel();
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        cachedLink.getData(cancelSource.getToken(), AsyncMocks.mockListener());

        // These two calls (below) will do nothing in most cases. They are only
        // needed if 500 ms elapsed between the above "cancel()" call and the
        // completion of the onDoneReceive after the cancellation. This is
        // unlikely but possible. In this event, this test won't really test
        // anything but this is unlikely and it will succeed in most run.
        wrappedLink.onDataArrive(originalData);
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        CancelableWaits.sleep(Cancellation.UNCANCELABLE_TOKEN, 1000, TimeUnit.MILLISECONDS);
        // Since the cancellation timer was set to 500 ms, it should have
        // elapsed. In case, it didn't (for some weird reason), the worst
        // thing which could happen is that this test won't test anything.

        AsyncDataListener<RefCachedData<Object>> listener3 = AsyncMocks.mockListener();
        cachedLink.getData(cancelSource.getToken(), listener3);
        // Since the previous data must have been cached, listener2 must
        // have already been invoked.
        InOrder inOrder = inOrder(listener3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RefCachedData<Object>> dataRef =
               (ArgumentCaptor<RefCachedData<Object>>)
               (ArgumentCaptor<?>) ArgumentCaptor.forClass(RefCachedData.class);

        inOrder.verify(listener3).onDataArrive(dataRef.capture());
        assertSame(originalData, dataRef.getValue().getData());

        ArgumentCaptor<AsyncReport> report2Ref = ArgumentCaptor.forClass(AsyncReport.class);
        inOrder.verify(listener3).onDoneReceive(report2Ref.capture());
        assertTrue(report2Ref.getValue().isSuccess());
    }

    /**
     * Test if we cancel one of the two parallel requests of RefCachedDataLink.
     */
    @Test
    public void testPartialCancellation() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cachedLink.getData(cancelSource.getToken(), listener1);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        wrappedLink.onDataArrive(toSend[0]);
        cancelSource.getController().cancel();

        // The RefCachedDataLink must be eager to cancel the canceled transfer.
        assertTrue(listener1.isCompleted());

        assertTrue(listener1.getReport().isCanceled());

        assertEquals(1, listener1.getResults().size());
        checkValidResults(listener1, toSend);

        for (int i = 1; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        assertTrue(listener2.isCompleted());

        assertSame(report, listener2.getReport());

        assertEquals(toSend.length, listener2.getResults().size());
        checkValidCompleteResults(listener2, toSend);

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    @Test
    public void testControlForward() {
        final Object[] controlArgs = new Object[] {
            "ARG1", "ARG2", "ARG3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();
        AsyncDataController controller = cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        for (Object controlArg: controlArgs) {
            controller.controlData(controlArg);
        }

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertTrue(listener.isCompleted());

        assertArrayEquals(controlArgs, wrappedLink.getReceivedControlArgs().toArray());

        assertNull(listener.getMiscError());
    }

    /**
     * Tests if the RefCachedDataLink will forward the control arguments again
     * if the underlying data link must be restarted.
     */
    @Test
    public void testControlForwardAgain() {
        final Object[] controlArgs = new Object[] {
            "ARG1", "ARG2", "ARG3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = create(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, new CollectListener<>());
        wrappedLink.onDataArrive("DUMMY DATA");

        AsyncDataController controller = cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        for (Object controlArg: controlArgs) {
            controller.controlData(controlArg);
        }

        assertArrayEquals(controlArgs, wrappedLink.getReceivedControlArgs().toArray());

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertFalse(listener.isCompleted());

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertArrayEquals(concatArrays(controlArgs, controlArgs), wrappedLink.getReceivedControlArgs().toArray());

        assertNull(listener.getMiscError());
    }

    /**
     * Test of toString method, of class RefCachedDataLink.
     */
    @Test
    public void testToString() {
        AsyncDataLink<String> wrappedLink = AsyncLinks.createPreparedLink("DATA", null);
        AsyncDataLink<String> cachedLink = AsyncLinks.cacheResult(wrappedLink, ReferenceType.UserRefType, null);

        assertNotNull(cachedLink.toString());
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 5604755825215798376L;
    }
}
