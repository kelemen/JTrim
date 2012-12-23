package org.jtrim.concurrent.async;

import org.jtrim.cancel.Cancellation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

import static org.jtrim.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class PreparedDataLinkTest {

    public PreparedDataLinkTest() {
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

    @Test
    public void testWithState() {
        Object testData = new Object();
        AsyncDataState testState = mock(AsyncDataState.class);
        PreparedDataLink<Object> dataLink = new PreparedDataLink<>(testData, testState);
        AsyncDataListener<Object> listener = mockListener();

        AsyncDataController controller = dataLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(testData));
        inOrder.verify(listener).onDoneReceive(argThat(cmpReport(AsyncReport.SUCCESS)));
        inOrder.verifyNoMoreInteractions();

        assertSame(testState, controller.getDataState());

        controller.controlData(new Object());
    }

    @Test
    public void testWithController() {
        Object testData = new Object();
        AsyncDataController testController = mock(AsyncDataController.class);
        PreparedDataLink<Object> dataLink = new PreparedDataLink<>(testData, testController);
        AsyncDataListener<Object> listener = mockListener();

        AsyncDataController controller = dataLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
        assertSame(testController, controller);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(testData));
        inOrder.verify(listener).onDoneReceive(argThat(cmpReport(AsyncReport.SUCCESS)));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testErrorInListener() {
        Object testData = new Object();
        AsyncDataController testController = mock(AsyncDataController.class);
        PreparedDataLink<Object> dataLink = new PreparedDataLink<>(testData, testController);
        AsyncDataListener<Object> listener = mockListener();

        doThrow(TestException.class).when(listener).onDataArrive(any());

        try {
            dataLink.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
        } catch (TestException ex) {
            // PreparedDataLink may hide thrown exception but currently it does
            // not so we catch it
        }

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(testData));
        inOrder.verify(listener).onDoneReceive(argThat(cmpReport(AsyncReport.SUCCESS)));
        inOrder.verifyNoMoreInteractions();
    }

    private static ArgumentMatcher<AsyncReport> cmpReport(AsyncReport expected) {
        return new AsyncReportCompare(expected);
    }

    /**
     * Test of toString method, of class PreparedDataLink.
     */
    @Test
    public void testToString() {
        assertNotNull(new PreparedDataLink<>(null, (AsyncDataState)null).toString());
        assertNotNull(new PreparedDataLink<>(new Object(), (AsyncDataState)null).toString());
        assertNotNull(new PreparedDataLink<>(new Object(), new SimpleDataState("", 0.0)).toString());
    }

    private static class AsyncReportCompare extends ArgumentMatcher<AsyncReport> {
        private final AsyncReport expected;

        public AsyncReportCompare(AsyncReport expected) {
            assert expected != null;
            this.expected = expected;
        }

        @Override
        public boolean matches(Object argument) {
            AsyncReport received = (AsyncReport)argument;
            if (expected == received) {
                return true;
            }

            return expected.isCanceled() == received.isCanceled()
                    && expected.getException() == received.getException();
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -4329153948859986628L;
    }
}
