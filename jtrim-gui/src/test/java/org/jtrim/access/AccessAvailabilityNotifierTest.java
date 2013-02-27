package org.jtrim.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AccessAvailabilityNotifierTest {
    private static final Collection<HierarchicalRight> NO_RIGHTS = Collections.emptySet();

    private static final Collection<HierarchicalRight> RIGHTS1
            = Arrays.asList(HierarchicalRight.create(new Object()));

    private static final Collection<HierarchicalRight> RIGHTS2
            = Arrays.asList(HierarchicalRight.create(new Object()));

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

    private static AccessManager<Object, HierarchicalRight> createManager() {
        return new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
    }

    private static <RightType> AccessAvailabilityNotifier<RightType> create(AccessManager<?, RightType> manager) {
        return AccessAvailabilityNotifier.attach(manager);
    }

    private static void verifyAction(AccessChangeAction action, boolean... states) {
        ArgumentCaptor<Boolean> argCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(action, times(states.length)).onChangeAccess(argCaptor.capture());

        Boolean[] received = argCaptor.getAllValues().toArray(new Boolean[0]);
        Boolean[] expected = new Boolean[states.length];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = states[i];
        }
        assertArrayEquals(expected, received);
    }

    @Test
    public void testUnrelatedChangesWrite() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(NO_RIGHTS, RIGHTS1, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS2));
        assertTrue(access1.isAvailable());
        verifyAction(action, true);
        access1.release();
        verifyAction(action, true);
    }

    @Test
    public void testBecomeUnavailableThenAvailableWrite() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(NO_RIGHTS, RIGHTS1, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyAction(action, false);
        access1.release();
        verifyAction(action, false, true);
    }

    @Test
    public void testBecomeUnavailableThenAvailableWriteReadNull() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(null, RIGHTS1, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyAction(action, false);
        access1.release();
        verifyAction(action, false, true);
    }

    @Test
    public void testUnrelatedChangesRead() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(RIGHTS1, NO_RIGHTS, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", RIGHTS1, NO_RIGHTS));
        assertTrue(access1.isAvailable());
        verifyAction(action, true);
        access1.release();
        verifyAction(action, true);
    }

    @Test
    public void testBecomeUnavailableThenAvailableRead() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(RIGHTS1, NO_RIGHTS, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyAction(action, false);
        access1.release();
        verifyAction(action, false, true);
    }

    @Test
    public void testBecomeUnavailableThenAvailableReadWriteNull() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(RIGHTS1, null, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyAction(action, false);
        access1.release();
        verifyAction(action, false, true);
    }

    @Test
    public void testNoNotificationsAfterDetach() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(NO_RIGHTS, RIGHTS1, action);
        assertNotNull(listenerRef);
        verifyZeroInteractions(action);

        notifier.detach();

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyZeroInteractions(action);
        access1.release();
        verifyZeroInteractions(action);
    }

    @Test
    public void testAddGroupAfterDetach() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        notifier.detach();

        ListenerRef listenerRef = notifier.addGroupListener(NO_RIGHTS, RIGHTS1, action);
        assertNotNull(listenerRef);
        assertFalse(listenerRef.isRegistered());
        verifyZeroInteractions(action);

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyZeroInteractions(action);
        access1.release();
        verifyZeroInteractions(action);
    }

    @Test
    public void testNoNotificationsAfterUnregister() {
        AccessManager<Object, HierarchicalRight> manager = createManager();
        AccessAvailabilityNotifier<HierarchicalRight> notifier = create(manager);
        AccessChangeAction action = mock(AccessChangeAction.class);

        ListenerRef listenerRef = notifier.addGroupListener(NO_RIGHTS, RIGHTS1, action);
        assertNotNull(listenerRef);
        assertTrue(listenerRef.isRegistered());
        verifyZeroInteractions(action);

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());

        AccessResult<Object> access1 = manager.getScheduledAccess(new AccessRequest<>("REQUEST1", NO_RIGHTS, RIGHTS1));
        assertTrue(access1.isAvailable());
        verifyZeroInteractions(action);
        access1.release();
        verifyZeroInteractions(action);
    }

    /**
     * Test of multiAction method, of class AccessAvailabilityNotifier.
     */
    @Test
    public void testMultiAction() {
        for (boolean arg: Arrays.asList(false, true)) {
            AccessChangeAction action1 = mock(AccessChangeAction.class);
            AccessChangeAction action2 = mock(AccessChangeAction.class);
            AccessChangeAction action3 = mock(AccessChangeAction.class);
            AccessChangeAction action4 = mock(AccessChangeAction.class);

            doThrow(new TestException()).when(action2).onChangeAccess(anyBoolean());
            doThrow(new TestException()).when(action3).onChangeAccess(anyBoolean());

            AccessChangeAction action = AccessAvailabilityNotifier.multiAction(action1, action2, action3, action4);

            try {
                action.onChangeAccess(arg);
                fail("Expected TestException.");
            } catch (TestException ex) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals("Expected 1 suppressed", 1, suppressed.length);
                assertTrue("Expected suppressed TestException", suppressed[0] instanceof TestException);
            }

            verify(action1).onChangeAccess(arg);
            verify(action2).onChangeAccess(arg);
            verify(action3).onChangeAccess(arg);
            verify(action4).onChangeAccess(arg);
        }
    }

    @Test
    public void testMultiActionWithNoActions() {
        AccessChangeAction action = AccessAvailabilityNotifier.multiAction();
        action.onChangeAccess(true);
    }

    @Test
    public void testMultiActionWithSuccess() {
        for (boolean arg: Arrays.asList(false, true)) {
            AccessChangeAction action1 = mock(AccessChangeAction.class);
            AccessChangeAction action2 = mock(AccessChangeAction.class);

            AccessChangeAction action = AccessAvailabilityNotifier.multiAction(action1, action2);
            action.onChangeAccess(arg);

            verify(action1).onChangeAccess(arg);
            verify(action2).onChangeAccess(arg);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalMultiAction1() {
        AccessAvailabilityNotifier.multiAction((AccessChangeAction[])null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalMultiAction2() {
        AccessChangeAction action = mock(AccessChangeAction.class);
        AccessAvailabilityNotifier.multiAction(null, action, action);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalMultiAction3() {
        AccessChangeAction action = mock(AccessChangeAction.class);
        AccessAvailabilityNotifier.multiAction(action, null, action);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalMultiAction4() {
        AccessChangeAction action = mock(AccessChangeAction.class);
        AccessAvailabilityNotifier.multiAction(action, action, null);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -6799393833891021852L;
    }
}
