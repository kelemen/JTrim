package org.jtrim.concurrent.async;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
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
public class RefCachedDataLinkTest {

    public RefCachedDataLinkTest() {
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

    private static Object[] concatArrays(Object[] array1, Object[] array2) {
        Object[] result = new Object[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    private static <ResultType> DataConverter<RefCachedData<ResultType>, ResultType> resultExtractor() {
        return new DataConverter<RefCachedData<ResultType>, ResultType>() {
            @Override
            public ResultType convertData(RefCachedData<ResultType> data) {
                return data.getData();
            }
        };
    }

    private static DataConverter<RefCachedData<String>, String> stringResultExtractor() {
        return resultExtractor();
    }

    private <ResultType> void checkValidResults(
            CollectListener<RefCachedData<ResultType>> listener,
            ResultType[] expectedResults) {

        listener.checkValidResults(
                expectedResults,
                RefCachedDataLinkTest.<ResultType>resultExtractor());
    }

    private <ResultType> void checkValidCompleteResults(
            CollectListener<RefCachedData<ResultType>> listener,
            ResultType[] expectedResults) {

        listener.checkValidCompleteResults(
                expectedResults,
                RefCachedDataLinkTest.<ResultType>resultExtractor());
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

        doThrow(TestException.class).when(errorListener).onDataArrive(any());
        doThrow(TestException.class).when(errorListener).onDoneReceive(any(AsyncReport.class));

        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, errorListener);
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        for (String data: toSend) {
            wrappedLink.onDataArrive(data);
        }
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(AsyncReport.SUCCESS, listener1.getReport());
        assertSame(AsyncReport.SUCCESS, listener2.getReport());

        List<String> results1 = listener1.extractedResults(stringResultExtractor());
        List<String> results2 = listener2.extractedResults(stringResultExtractor());

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

        List<String> results = listener.extractedResults(stringResultExtractor());

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

        List<String> results1 = listener1.extractedResults(stringResultExtractor());
        List<String> results2 = listener2.extractedResults(stringResultExtractor());

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

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
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

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
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

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
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

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
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

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
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

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
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
