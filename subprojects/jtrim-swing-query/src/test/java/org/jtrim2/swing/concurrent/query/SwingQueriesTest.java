package org.jtrim2.swing.concurrent.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim2.access.AccessManager;
import org.jtrim2.access.AccessRequest;
import org.jtrim2.access.HierarchicalAccessManager;
import org.jtrim2.access.HierarchicalRight;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.SimpleDataController;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.swing.component.GuiTestUtils;
import org.jtrim2.ui.concurrent.query.BackgroundDataProvider;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SwingQueriesTest {
    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(SwingQueries.class);
    }

    @Test
    public void testGetSwingBackgroundDataProvider() {
        // This is just a basic test that the factory method is not borken.
        // There are better tests in BackgroundDataProviderTest

        AccessManager<String, HierarchicalRight> manager
                = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        BackgroundDataProvider<String, HierarchicalRight> dataProvider
                = SwingQueries.getSwingBackgroundDataProvider(manager);

        AccessRequest<String, HierarchicalRight> request
                = AccessRequest.getWriteRequest("", HierarchicalRight.create("RIGHT"));

        AsyncReport report = AsyncReport.getReport(new Exception("TestException"), false);
        AsyncDataLink<String> link = dataProvider.createLink(request, (cancelToken, dataListener) -> {
            dataListener.onDataArrive("DATA1");
            dataListener.onDoneReceive(report);

            return new SimpleDataController();
        });

        AtomicReference<AsyncReport> reportRef = new AtomicReference<>();
        List<String> receivedData = new ArrayList<>();
        Runnable wrongThread = mock(Runnable.class);
        link.getData(Cancellation.UNCANCELABLE_TOKEN, new AsyncDataListener<String>() {
            private void checkThread() {
                if (!SwingUtilities.isEventDispatchThread()) {
                    wrongThread.run();
                }
            }

            @Override
            public void onDataArrive(String data) {
                receivedData.add(data);
                checkThread();
            }

            @Override
            public void onDoneReceive(AsyncReport report) {
                reportRef.set(report);
                checkThread();
            }
        });

        GuiTestUtils.waitAllSwingEvents();

        assertEquals(Arrays.asList("DATA1"), receivedData);
        assertSame(report, reportRef.get());

        verifyNoInteractions(wrongThread);
    }
}
