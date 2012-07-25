package org.jtrim.concurrent.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cache.GenericReference;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

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

    private <T> int findInList(T toFind, List<T> list, int startOffset) {
        int size = list.size();
        for (int i = startOffset; i < size; i++) {
            if (list.get(i) == toFind) {
                return i;
            }
        }
        return -1;
    }

    private <ResultType> void checkValidResults(ResultType[] expectedResults, List<ResultType> actualResults) {
        int expectedIndex = 0;
        for (ResultType actual: actualResults) {
            int foundIndex = findInList(actual, actualResults, expectedIndex);
            if (foundIndex < 0) {
                fail("Unexpected results: " + actualResults
                        + " (expected = " + Arrays.toString(expectedResults) + ")");
            }
        }
    }

    private <ResultType> void checkValidCompleteResults(ResultType[] expectedResults, List<ResultType> actualResults) {
        if (expectedResults.length == 0) {
            assertEquals("Expected no results.", 0, actualResults.size());
            return;
        }

        if (actualResults.isEmpty()) {
            fail("Need at least one result.");
        }

        assertEquals(
                "The final result must match the final expected result.",
                expectedResults[expectedResults.length - 1],
                actualResults.get(actualResults.size() - 1));

        checkValidResults(expectedResults, actualResults);
    }

    private <ResultType> List<ResultType> extractResults(List<RefCachedData<ResultType>> resultRefs) {
        List<ResultType> result = new LinkedList<>();
        for (RefCachedData<ResultType> dataRef: resultRefs) {
            result.add(dataRef.getData());
        }
        return result;
    }

    @Test
    public void testSingleTransfer() {
        final String[] toSend = new String[] {
            "DATA1", "DATA2", "DATA3"
        };

        ManualCache cache = new ManualCache();
        ManualDataLink<String> wrappedLink = new ManualDataLink<>();
        AsyncDataLink<RefCachedData<String>> cachedLink
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        for (String data: toSend) {
            wrappedLink.onDataArrive(data);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertTrue(listener.isCompleted());

        assertSame(report, listener.getReport());

        List<String> results = extractResults(listener.getResults());
        assertEquals(toSend.length, results.size());
        checkValidCompleteResults(toSend, results);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache);

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

        List<String> results1 = extractResults(listener1.getResults());
        List<String> results2 = extractResults(listener2.getResults());

        assertEquals(toSend.length, results1.size());
        assertEquals(toSend.length, results2.size());

        checkValidCompleteResults(toSend, results1);
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.HardRefType, cache);

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

        List<String> results1 = extractResults(listener1.getResults());
        List<String> results2 = extractResults(listener2.getResults());

        // listener2 must have received the first data as well because
        // it must have been cached.
        assertEquals(toSend.length, results1.size());
        assertEquals(toSend.length, results2.size());

        checkValidCompleteResults(toSend, results1);
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.HardRefType, cache);

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

        List<String> results1 = extractResults(listener1.getResults());
        List<String> results2 = extractResults(listener2.getResults());

        assertEquals(toSend.length, results1.size());
        assertEquals(toSend.length - 1, results2.size());

        checkValidCompleteResults(toSend, results1);
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache);

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

        List<String> results1 = extractResults(listener1.getResults());
        assertEquals(toSend.length, results1.size());
        checkValidCompleteResults(toSend, results1);

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }
        wrappedLink.onDoneReceive(report);

        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());

        List<String> results2 = extractResults(listener1.getResults());
        assertEquals(toSend.length, results2.size());
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.HardRefType, cache);

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

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        List<String> results1 = extractResults(listener1.getResults());
        List<String> results2 = extractResults(listener2.getResults());

        assertEquals(toSend.length, results1.size());
        assertEquals(1, results2.size());

        checkValidCompleteResults(toSend, results1);
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.HardRefType, cache);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        assertTrue(listener1.isCompleted());
        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());
        assertSame(report, listener2.getReport());

        List<String> results1 = extractResults(listener1.getResults());
        List<String> results2 = extractResults(listener2.getResults());

        assertEquals(toSend.length, results1.size());
        assertEquals(1, results2.size());

        checkValidCompleteResults(toSend, results1);
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CollectListener<RefCachedData<String>> listener2 = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        assertTrue(listener1.isCompleted());
        assertFalse(listener2.isCompleted());

        assertEquals(0, listener2.getResults().size());

        assertSame(report, listener1.getReport());

        List<String> results1 = extractResults(listener1.getResults());
        assertEquals(toSend.length, results1.size());
        checkValidCompleteResults(toSend, results1);

        for (int i = 0; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }
        wrappedLink.onDoneReceive(report);

        assertTrue(listener2.isCompleted());

        assertSame(report, listener1.getReport());

        List<String> results2 = extractResults(listener1.getResults());
        assertEquals(toSend.length, results2.size());
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

        CollectListener<RefCachedData<String>> listener1 = new CollectListener<>();
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cachedLink.getData(cancelSource.getToken(), listener1);

        wrappedLink.onDataArrive(toSend[0]);
        cancelSource.getController().cancel();

        // The RefCachedDataLink must be eager to cancel the current transfer.
        assertTrue(listener1.isCompleted());

        assertTrue(listener1.getReport().isCanceled());

        List<String> results1 = extractResults(listener1.getResults());
        assertEquals(1, results1.size());
        checkValidResults(toSend, results1);

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

        List<String> results2 = extractResults(listener2.getResults());
        assertEquals(toSend.length, results2.size());
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache, 0L, TimeUnit.MILLISECONDS);

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

        List<String> results1 = extractResults(listener1.getResults());
        assertEquals(1, results1.size());
        checkValidResults(toSend, results1);

        for (int i = 1; i < toSend.length; i++) {
            wrappedLink.onDataArrive(toSend[i]);
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        wrappedLink.onDoneReceive(report);

        assertFalse(wrappedLink.hasLastRequestBeenCanceled());

        assertTrue(listener2.isCompleted());

        assertSame(report, listener2.getReport());

        List<String> results2 = extractResults(listener2.getResults());
        assertEquals(toSend.length, results2.size());
        checkValidCompleteResults(toSend, results2);

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();
        AsyncDataController controller = cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        for (Object controlArg: controlArgs) {
            controller.controlData(controlArg);
        }

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertTrue(listener.isCompleted());

        assertArrayEquals(controlArgs, wrappedLink.getForwardedDatas().toArray());

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
                = AsyncLinks.refCacheResult(wrappedLink, ReferenceType.NoRefType, cache);

        CollectListener<RefCachedData<String>> listener = new CollectListener<>();
        cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, new CollectListener<>());
        wrappedLink.onDataArrive("DUMMY DATA");

        AsyncDataController controller = cachedLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        for (Object controlArg: controlArgs) {
            controller.controlData(controlArg);
        }

        assertArrayEquals(controlArgs, wrappedLink.getForwardedDatas().toArray());

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertFalse(listener.isCompleted());

        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        assertArrayEquals(concatArrays(controlArgs, controlArgs), wrappedLink.getForwardedDatas().toArray());

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

    private static class ManualDataLink<DataType>
    implements
            AsyncDataLink<DataType>,
            AsyncDataListener<DataType> {
        private final Queue<AsyncDataListener<? super DataType>> listeners;
        private volatile CancellationToken lastCancelToken;

        private final Queue<Object> forwardedDatas;

        public ManualDataLink() {
            this.listeners = new ConcurrentLinkedQueue<>();
            this.forwardedDatas = new ConcurrentLinkedQueue<>();
            this.lastCancelToken = null;
        }

        public boolean hasLastRequestBeenCanceled() {
            CancellationToken cancelToken = lastCancelToken;
            return cancelToken != null
                    ? cancelToken.isCanceled()
                    : false;
        }

        public List<Object> getForwardedDatas() {
            return new ArrayList<>(forwardedDatas);
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                AsyncDataListener<? super DataType> dataListener) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

            lastCancelToken = cancelToken;
            listeners.add(dataListener);

            return new AsyncDataController() {
                @Override
                public void controlData(Object controlArg) {
                    forwardedDatas.add(controlArg);
                }

                @Override
                public AsyncDataState getDataState() {
                    return null;
                }
            };
        }

        @Override
        public boolean requireData() {
            return true;
        }

        @Override
        public void onDataArrive(DataType data) {
            for (AsyncDataListener<? super DataType> listener: listeners) {
                listener.onDataArrive(data);
            }
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            List<AsyncDataListener<? super DataType>> doneListeners = new LinkedList<>();

            while (true) {
                AsyncDataListener<? super DataType> listener = listeners.poll();
                if (listener != null) {
                    doneListeners.add(listener);
                }
                else {
                    break;
                }
            }

            for (AsyncDataListener<? super DataType> listener: doneListeners) {
                listener.onDoneReceive(report);
            }
        }
    }

    private static class ManualCache implements ObjectCache {
        private final ConcurrentMap<Object, AtomicReference<?>> cachedValues;

        public ManualCache() {
            this.cachedValues = new ConcurrentHashMap<>();
        }

        public void removeFromCache(Object value) {
            AtomicReference<?> valueRef = cachedValues.remove(value);
            if (valueRef != null) {
                valueRef.set(null);
            }
        }

        @Override
        public <V> VolatileReference<V> getReference(V obj, ReferenceType refType) {
            if (refType == ReferenceType.NoRefType) {
                return GenericReference.getNoReference();
            }
            if (refType == ReferenceType.HardRefType) {
                return GenericReference.createHardReference(obj);
            }

            AtomicReference<V> storedRef = null;
            do {
                // Safe since the atomic reference holds "obj" or null.
                @SuppressWarnings("unchecked")
                AtomicReference<V> objRef = (AtomicReference<V>)cachedValues.get(obj);

                if (objRef == null) {
                    objRef = new AtomicReference<>(obj);
                    if (cachedValues.putIfAbsent(obj, objRef) == null) {
                        storedRef = objRef;
                    }
                }
            } while (storedRef == null);

            final VolatileReference<V> javaRef = GenericReference.createReference(obj, refType);
            final AtomicReference<AtomicReference<V>> resultRef = new AtomicReference<>(storedRef);
            return new VolatileReference<V>() {
                @Override
                public V get() {
                    V result = javaRef.get();
                    if (result == null) {
                        AtomicReference<V> ref = resultRef.get();
                        result = ref != null ? ref.get() : null;
                    }
                    return result;
                }

                @Override
                public void clear() {
                    resultRef.set(null);
                }
            };
        }
    }
}
