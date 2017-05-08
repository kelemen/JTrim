package org.jtrim2.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;

public class DataOrdererListenerTest {
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

    private void testDataForward(int dataCount) {
        CollectListener<OrderedData<Object>> wrappedListener = new CollectListener<>();
        DataOrdererListener<Object> listener = new DataOrdererListener<>(wrappedListener);

        if (dataCount > 0) {
            Object[] datas = new Object[dataCount];
            for (int i = 0; i < dataCount; i++) {
                datas[i] = new Object();
                listener.onDataArrive(datas[i]);
            }

            OrderedData<?>[] received = wrappedListener.getResults().toArray(new OrderedData<?>[0]);
            assertEquals("The number of received and sent data objects must equal",
                    datas.length, received.length);

            assertSame(datas[0], received[0].getRawData());
            long prevIndex = received[0].getIndex();
            for (int i = 1; i < dataCount; i++) {
                assertSame(datas[i], received[i].getRawData());
                assertTrue(prevIndex < received[i].getIndex());
            }
        }

        AsyncReport report = AsyncReport.getReport(new Exception(), false);
        listener.onDoneReceive(report);

        assertSame(report, wrappedListener.getReport());
    }

    @Test
    public void testForwardZeroData() {
        testDataForward(0);
    }

    @Test
    public void testForwardSingleData() {
        testDataForward(1);
    }

    @Test
    public void testForwardMultipleData() {
        for (int i = 2; i < 10; i++) {
            testDataForward(i);
        }
    }

    /**
     * Test of toString method, of class DataOrdererListener.
     */
    @Test
    public void testToString() {
        assertNotNull(new DataOrdererListener<>(mockListener()).toString());
    }
}
