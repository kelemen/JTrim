package org.jtrim2.concurrent.query;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class PeriodicStateReporterLinkTest {
    @SuppressWarnings("unchecked")
    private static <DataType> AsyncStateReporter<DataType> mockStateReporter() {
        return mock(AsyncStateReporter.class);
    }

    @Test(timeout = 10000)
    @SuppressWarnings("unchecked") // Needs because of stubing with generics
    public void testStateReportingDirect() {
        AsyncDataLink<Object> wrappedLink = mockLink();
        AsyncStateReporter<Object> stateReporter = mockStateReporter();
        AsyncDataController wrappedController = mock(AsyncDataController.class);

        final WaitableSignal endSignal = new WaitableSignal();
        final AtomicInteger callCount = new AtomicInteger(0);
        doAnswer((InvocationOnMock invocation) -> {
            if (callCount.incrementAndGet() >= 3) {
                endSignal.signal();
            }
            return null;
        }).when(stateReporter).reportState(
                any(AsyncDataLink.class),
                any(AsyncDataListener.class),
                any(AsyncDataController.class));

        stubController(wrappedLink, wrappedController);

        PeriodicStateReporterLink<Object> link = new PeriodicStateReporterLink<>(
                wrappedLink, stateReporter, 10L, TimeUnit.MILLISECONDS);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        verify(stateReporter, atLeast(3)).reportState(
                same(wrappedLink),
                same(listener),
                same(wrappedController));
    }

    private <DataType> PeriodicStateReporterLink<DataType> create(
            UpdateTaskExecutor reportExecutor,
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {
        return new PeriodicStateReporterLink<>(reportExecutor, wrappedLink, reporter, period, periodUnit);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(mock(UpdateTaskExecutor.class), null, mockStateReporter(), 1L, TimeUnit.DAYS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mock(UpdateTaskExecutor.class), mockLink(), null, 1L, TimeUnit.DAYS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor3() {
        create(mock(UpdateTaskExecutor.class), mockLink(), mockStateReporter(), -1L, TimeUnit.DAYS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor4() {
        create(mock(UpdateTaskExecutor.class), mockLink(), mockStateReporter(), 1L, null);
    }

    @Test(timeout = 10000)
    @SuppressWarnings("unchecked") // Needs because of stubing with generics
    public void testStateReportingThrouhExecutor() {
        final SyncTaskExecutor syncExecutor = new SyncTaskExecutor();
        UpdateTaskExecutor executor = new GenericUpdateTaskExecutor(syncExecutor);

        AsyncDataLink<Object> wrappedLink = mockLink();
        AsyncStateReporter<Object> stateReporter = mockStateReporter();
        AsyncDataController wrappedController = mock(AsyncDataController.class);

        final WaitableSignal endSignal = new WaitableSignal();
        final AtomicInteger callCount = new AtomicInteger(0);
        final AtomicReference<String> error = new AtomicReference<>(null);

        doAnswer((InvocationOnMock invocation) -> {
            if (!syncExecutor.isExecutingInThis()) {
                error.set("Not executed by the UpdateTaskExecutor. Call index: " + callCount.get());
            }

            if (callCount.incrementAndGet() >= 3) {
                endSignal.signal();
            }
            return null;
        }).when(stateReporter).reportState(
                any(AsyncDataLink.class),
                any(AsyncDataListener.class),
                any(AsyncDataController.class));

        stubController(wrappedLink, wrappedController);

        PeriodicStateReporterLink<Object> link = new PeriodicStateReporterLink<>(
                executor, wrappedLink, stateReporter, 10L, TimeUnit.MILLISECONDS);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        assertNull(error.get());
        verify(stateReporter, atLeast(3)).reportState(same(wrappedLink), same(listener), same(wrappedController));
    }

    @Test
    public void testControlDatas() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        AsyncStateReporter<Object> stateReporter = mockStateReporter();
        PeriodicStateReporterLink<Object> link = new PeriodicStateReporterLink<>(
                wrappedLink, stateReporter, 1L, TimeUnit.DAYS);

        AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, mockListener());

        Object[] controlArgs = new Object[]{new Object(), new Object(), new Object()};
        for (Object controlArg: controlArgs) {
            controller.controlData(controlArg);
        }

        assertArrayEquals(controlArgs, wrappedLink.getReceivedControlArgs().toArray());
    }

    @Test
    public void testDataForwarding() {
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>();
        AsyncStateReporter<Object> stateReporter = mockStateReporter();
        PeriodicStateReporterLink<Object> link = new PeriodicStateReporterLink<>(
                wrappedLink, stateReporter, 1L, TimeUnit.DAYS);

        CollectListener<Object> listener = new CollectListener<>();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        Object[] datas = new Object[]{new Object(), new Object(), new Object()};
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        for (Object data: datas) {
            wrappedLink.onDataArrive(data);
        }

        wrappedLink.onDoneReceive(report);

        assertArrayEquals(datas, listener.getResults().toArray());
        assertSame(report, listener.getReport());
        assertNull(listener.getMiscError());
    }

    @Test
    public void testToString1() {
        PeriodicStateReporterLink<Object> link = new PeriodicStateReporterLink<>(
                mockLink(),
                mockStateReporter(),
                1L,
                TimeUnit.DAYS);
        assertNotNull(link.toString());
    }

    @Test
    public void testToString2() {
        PeriodicStateReporterLink<Object> link = new PeriodicStateReporterLink<>(
                mock(UpdateTaskExecutor.class),
                mockLink(),
                mockStateReporter(),
                1L,
                TimeUnit.DAYS);
        assertNotNull(link.toString());
    }
}
