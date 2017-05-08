package org.jtrim2.concurrent.async;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ImproverTasksLinkTest {
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

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockConverter() {
        return mock(DataConverter.class);
    }

    private static <InputType, ResultType> ImproverTasksLink<InputType, ResultType> create(
            InputType input,
            List<? extends AsyncDataConverter<InputType, ResultType>> transformers) {
        return new ImproverTasksLink<>(input, transformers);
    }

    private static ImproverTasksLink<Object, TestData> createTestLink(
            Object input,
            Object... converterBases) {

        List<AsyncDataConverter<Object, TestData>> converters = new LinkedList<>();
        for (Object base: converterBases) {
            converters.add(createConverter(base));
        }
        return new ImproverTasksLink<>(input, converters);
    }

    private static AsyncDataConverter<Object, TestData> createConverter(Object baseData) {
        TestDataConverter converter = new TestDataConverter(baseData);
        return new AsyncDataConverter<>(converter, new SyncTaskExecutor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor1() {
        create(new Object(), Collections.<AsyncDataConverter<Object, Object>>emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(new Object(), Arrays.asList(null, createConverter(new Object())));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor3() {
        create(new Object(), Arrays.asList(createConverter(new Object()), null));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor4() {
        create(new Object(), null);
    }

    @Test
    public void testConversionFailure() {
        Object input = new Object();
        Object converterBase = new Object();
        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        DataConverter<Object, TestData> firstConverter = mockConverter();

        TestException conversionError = new TestException();
        stub(firstConverter.convertData(any())).toThrow(conversionError);

        List<AsyncDataConverter<Object, TestData>> converters = Arrays.asList(
                new AsyncDataConverter<>(firstConverter, new SyncTaskExecutor()),
                createConverter(converterBase));
        ImproverTasksLink<Object, TestData> link = create(input, converters);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(cancelSource.getToken(), listener);

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, atLeast(0)).onDataArrive(any(TestData.class));
        inOrder.verify(listener).onDoneReceive(receivedReport.capture());
        inOrder.verifyNoMoreInteractions();

        assertFalse(receivedReport.getValue().isCanceled());
        assertSame(conversionError, receivedReport.getValue().getException());
    }

    @Test
    public void testPreCancellation() {
        Object input = new Object();
        Object converterBase = new Object();

        ImproverTasksLink<Object, TestData> link = createTestLink(input, converterBase);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.CANCELED_TOKEN, listener);

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);

        verify(listener).onDoneReceive(receivedReport.capture());
        verifyNoMoreInteractions(listener);

        assertTrue(receivedReport.getValue().isCanceled());
        assertNull(receivedReport.getValue().getException());
    }

    @Test
    public void testCancellation() {
        Object input = new Object();
        Object converterBase = new Object();
        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        DataConverter<Object, TestData> firstConverter = mockConverter();

        doAnswer((InvocationOnMock invocation) -> {
            cancelSource.getController().cancel();
            return new TestData(new Object(), invocation.getArguments()[0]);
        }).when(firstConverter).convertData(any());

        List<AsyncDataConverter<Object, TestData>> converters = Arrays.asList(
                new AsyncDataConverter<>(firstConverter, new SyncTaskExecutor()),
                createConverter(converterBase));
        ImproverTasksLink<Object, TestData> link = create(input, converters);

        AsyncDataListener<Object> listener = mockListener();
        AsyncDataController controller = link.getData(cancelSource.getToken(), listener);
        assertNotNull(controller.getDataState().toString());

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, atLeast(0)).onDataArrive(any(TestData.class));
        inOrder.verify(listener).onDoneReceive(receivedReport.capture());
        inOrder.verifyNoMoreInteractions();

        assertTrue(receivedReport.getValue().isCanceled());
        assertNull(receivedReport.getValue().getException());
    }

    @Test(timeout = 10000)
    public void testState() {
        Object input = new Object();

        List<DataConverter<Object, TestData>> converters = Arrays.asList(
                ImproverTasksLinkTest.<Object, TestData>mockConverter(),
                ImproverTasksLinkTest.<Object, TestData>mockConverter(),
                ImproverTasksLinkTest.<Object, TestData>mockConverter());

        final List<AsyncDataState> states = new LinkedList<>();
        final AtomicReference<AsyncDataController> controllerRef = new AtomicReference<>(null);

        for (DataConverter<Object, TestData> converter: converters) {
            stub(converter.convertData(any())).toAnswer((InvocationOnMock invocation) -> {
                states.add(controllerRef.get().getDataState());
                return new TestData(new Object(), invocation.getArguments()[0]);
            });
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(
                "ImproverTasksLinkTest.testState", 1, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS);
        try {
            final WaitableSignal startSignal = new WaitableSignal();
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, startSignal::waitSignal, null);

            List<AsyncDataConverter<Object, TestData>> asyncConverters = new LinkedList<>();
            for (DataConverter<Object, TestData> converter: converters) {
                asyncConverters.add(new AsyncDataConverter<>(converter, executor));
            }

            ImproverTasksLink<Object, TestData> link = create(input, asyncConverters);

            AsyncDataListener<Object> listener = mockListener();

            final WaitableSignal endSignal = new WaitableSignal();
            doAnswer((InvocationOnMock invocation) -> {
                endSignal.signal();
                return null;
            }).when(listener).onDoneReceive(any(AsyncReport.class));

            AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
            controllerRef.set(controller);

            // It should have no effect but call it anyway for a minimal test.
            controller.controlData(new Object());

            startSignal.signal();
            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

            AsyncDataState[] receivedStates = states.toArray(new AsyncDataState[0]);
            assertEquals(states.size(), receivedStates.length);
            for (int i = 0; i < receivedStates.length; i++) {
                double expectedProgress = (double)i / (double)receivedStates.length;
                assertEquals(expectedProgress, receivedStates[i].getProgress(), 0.0001);
            }
            assertEquals(1.0, controller.getDataState().getProgress(), 0.0001);

        } finally {
            executor.shutdownAndCancel();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test
    public void testMultiConversion() {
        Object input = new Object();
        Object[] converterBases = new Object[]{new Object(), new Object(), new Object()};

        ImproverTasksLink<Object, TestData> link = createTestLink(input, converterBases);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);
        ArgumentCaptor<TestData> receivedDatas = ArgumentCaptor.forClass(TestData.class);

        Object[] expected = new TestData[converterBases.length];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = new TestData(converterBases[i], input);
        }

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(expected.length)).onDataArrive(receivedDatas.capture());
        inOrder.verify(listener).onDoneReceive(receivedReport.capture());
        inOrder.verifyNoMoreInteractions();

        assertArrayEquals(expected, receivedDatas.getAllValues().toArray());
        assertTrue(receivedReport.getValue().isSuccess());
    }

    @Test
    public void testSingleConversion() {
        Object input = new Object();
        Object converterBase = new Object();

        ImproverTasksLink<Object, TestData> link = createTestLink(input, converterBase);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(eq(new TestData(converterBase, input)));
        inOrder.verify(listener).onDoneReceive(receivedReport.capture());
        inOrder.verifyNoMoreInteractions();

        assertTrue(receivedReport.getValue().isSuccess());
    }

    /**
     * Test of toString method, of class ImproverTasksLink.
     */
    @Test
    public void testToString() {
        ImproverTasksLink<Object, TestData> link = create(
                new Object(),
                Arrays.asList(createConverter(new Object())));

        assertNotNull(link.toString());
    }

    private static class TestData {
        private final Object converterBase;
        private final Object input;

        public TestData(Object converterBase, Object input) {
            this.converterBase = converterBase;
            this.input = input;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 67 * hash + System.identityHashCode(this.converterBase);
            hash = 67 * hash + System.identityHashCode(this.input);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TestData other = (TestData)obj;
            return this.converterBase == other.converterBase && this.input == other.input;
        }
    }

    private static class TestDataConverter implements DataConverter<Object, TestData> {
        private final Object baseData;

        public TestDataConverter(Object baseData) {
            this.baseData = baseData;
        }

        @Override
        public TestData convertData(Object data) {
            return new TestData(baseData, data);
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 3428351171238821595L;
    }
}
