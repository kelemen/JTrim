package org.jtrim2.access;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.property.PropertySource;
import org.jtrim2.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccessPropertiesTest {
    private HierarchicalAccessManager<String> accessManager;

    @Before
    public void setUp() {
         accessManager = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AccessProperties.class);
    }

    private static Collection<HierarchicalRight> singletonRight(TestRight right) {
        return Collections.singleton(HierarchicalRight.create(right));
    }

    private static AccessRequest<String, HierarchicalRight> readRequest(TestRight right) {
        return AccessRequest.getReadRequest("READ-REQUEST", HierarchicalRight.create(right));
    }

    private static AccessRequest<String, HierarchicalRight> writeRequest(TestRight right) {
        return AccessRequest.getWriteRequest("WRITE-REQUEST", HierarchicalRight.create(right));
    }

    private static AccessRequest<String, HierarchicalRight> readWriteRequest() {

        return new AccessRequest<>("READ-WRITE-REQUEST",
                singletonRight(TestRight.READ_RIGHT),
                singletonRight(TestRight.WRITE_RIGHT));
    }

    private AccessResult<String> acquireReadRight(TestRight right) {
        return accessManager.tryGetAccess(readRequest(right));
    }

    private AccessResult<String> acquireWriteRight(TestRight right) {
        return accessManager.tryGetAccess(writeRequest(right));
    }

    private void doTestsForTrackingReadAndWriteRights(PropertySource<Boolean> property) {
        assertTrue(property.getValue());

        TestListener listener = new TestListener();
        ListenerRef listenerRef = property.addChangeListener(listener);
        assertTrue(listenerRef.isRegistered());

        assertEquals(0, listener.getAndResetCallCount());

        AccessResult<String> result;

        result = acquireReadRight(TestRight.READ_RIGHT);
        assertTrue(property.getValue());

        result.release();
        listener.getAndResetCallCount();
        assertTrue(property.getValue());

        result = acquireWriteRight(TestRight.READ_RIGHT);
        listener.verifyCalledAndReset();
        assertFalse(property.getValue());

        result.release();
        listener.verifyCalledAndReset();
        assertTrue(property.getValue());

        result = acquireReadRight(TestRight.WRITE_RIGHT);
        listener.verifyCalledAndReset();
        assertFalse(property.getValue());

        result.release();
        listener.verifyCalledAndReset();
        assertTrue(property.getValue());

        result = acquireWriteRight(TestRight.WRITE_RIGHT);
        listener.verifyCalledAndReset();
        assertFalse(property.getValue());

        result.release();
        listener.verifyCalledAndReset();
        assertTrue(property.getValue());

        assertTrue(listenerRef.isRegistered());

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
        acquireWriteRight(TestRight.WRITE_RIGHT);
        assertEquals(0, listener.getAndResetCallCount());
    }

    /**
     * Test of trackRightsAvailable method, of class AccessProperties.
     */
    @Test
    public void testTrackRightsAvailable() {
        PropertySource<Boolean> property = AccessProperties.trackRightsAvailable(accessManager,
                singletonRight(TestRight.READ_RIGHT),
                singletonRight(TestRight.WRITE_RIGHT));
        doTestsForTrackingReadAndWriteRights(property);
    }


    /**
     * Test of trackRequestAvailable method, of class AccessProperties.
     */
    @Test
    public void testTrackRequestAvailable() {
        PropertySource<Boolean> property
                = AccessProperties.trackRequestAvailable(accessManager, readWriteRequest());
        doTestsForTrackingReadAndWriteRights(property);
    }

    private void doTestsForTrackingReadRight(PropertySource<Boolean> property) {
        assertTrue(property.getValue());

        TestListener listener = new TestListener();
        ListenerRef listenerRef = property.addChangeListener(listener);
        assertTrue(listenerRef.isRegistered());

        assertEquals(0, listener.getAndResetCallCount());

        AccessResult<String> result;

        result = acquireReadRight(TestRight.READ_RIGHT);
        assertTrue(property.getValue());

        result.release();
        listener.getAndResetCallCount();
        assertTrue(property.getValue());

        result = acquireWriteRight(TestRight.READ_RIGHT);
        listener.verifyCalledAndReset();
        assertFalse(property.getValue());

        result.release();
        listener.verifyCalledAndReset();
        assertTrue(property.getValue());

        assertTrue(listenerRef.isRegistered());

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
        acquireWriteRight(TestRight.READ_RIGHT);
        assertEquals(0, listener.getAndResetCallCount());
    }

    /**
     * Test of trackReadRightsAvailable method, of class AccessProperties.
     */
    @Test
    public void testTrackReadRightsAvailable() {
        PropertySource<Boolean> property
                = AccessProperties.trackReadRightsAvailable(accessManager, singletonRight(TestRight.READ_RIGHT));
        doTestsForTrackingReadRight(property);
    }

    /**
     * Test of trackReadRightAvailable method, of class AccessProperties.
     */
    @Test
    public void testTrackReadRightAvailable() {
        PropertySource<Boolean> property = AccessProperties.trackReadRightAvailable(
                accessManager,
                HierarchicalRight.create(TestRight.READ_RIGHT));

        doTestsForTrackingReadRight(property);
    }

    private void doTestsForTrackingWriteRight(PropertySource<Boolean> property) {
        assertTrue(property.getValue());

        TestListener listener = new TestListener();
        ListenerRef listenerRef = property.addChangeListener(listener);
        assertTrue(listenerRef.isRegistered());

        assertEquals(0, listener.getAndResetCallCount());

        AccessResult<String> result;

        result = acquireReadRight(TestRight.WRITE_RIGHT);
        listener.verifyCalledAndReset();
        assertFalse(property.getValue());

        result.release();
        listener.verifyCalledAndReset();
        assertTrue(property.getValue());

        result = acquireWriteRight(TestRight.WRITE_RIGHT);
        listener.verifyCalledAndReset();
        assertFalse(property.getValue());

        result.release();
        listener.verifyCalledAndReset();
        assertTrue(property.getValue());

        assertTrue(listenerRef.isRegistered());

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
        acquireWriteRight(TestRight.WRITE_RIGHT);
        assertEquals(0, listener.getAndResetCallCount());
    }

    /**
     * Test of trackWriteRightsAvailable method, of class AccessProperties.
     */
    @Test
    public void testTrackWriteRightsAvailable() {
        PropertySource<Boolean> property
                = AccessProperties.trackWriteRightsAvailable(accessManager, singletonRight(TestRight.WRITE_RIGHT));
        doTestsForTrackingWriteRight(property);
    }

    /**
     * Test of trackWriteRightAvailable method, of class AccessProperties.
     */
    @Test
    public void testTrackWriteRightAvailable() {
        PropertySource<Boolean> property
                = AccessProperties.trackWriteRightAvailable(
                accessManager,
                HierarchicalRight.create(TestRight.WRITE_RIGHT));

        doTestsForTrackingWriteRight(property);
    }

    private enum TestRight {
        READ_RIGHT,
        WRITE_RIGHT
    }

    private static class TestListener implements Runnable {
        private final AtomicInteger callCount;

        public TestListener() {
            this.callCount = new AtomicInteger(0);
        }

        public int getAndResetCallCount() {
            return callCount.getAndSet(0);
        }

        public void verifyCalledAndReset() {
            assertTrue("Listener must have been notified.", getAndResetCallCount() > 0);
        }

        @Override
        public void run() {
            callCount.incrementAndGet();
        }
    }
}
