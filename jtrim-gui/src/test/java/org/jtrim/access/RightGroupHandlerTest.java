/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.access;

import java.util.*;
import org.jtrim.event.ListenerRef;
import org.junit.*;

import static org.junit.Assert.*;


/**
 *
 * @author Kelemen Attila
 */
public class RightGroupHandlerTest {

    public RightGroupHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of multiAction method, of class RightGroupHandler.
     */
    @Test
    public void testMultiAction() {
        int actionCount = 5;
        AccessManager<?, HierarchicalRight> managerArg
                = new HierarchicalAccessManager<>(AccessTokens.getSyncExecutor());
        boolean availableArg = true;

        AccessChangeAction[] actions = new AccessChangeAction[actionCount];
        List<AccessChange> calls = new LinkedList<>();
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new RecordAction(calls);
        }

        RightGroupHandler.multiAction(actions).onChangeAccess(managerArg, availableArg);
        assertEquals(actions.length, calls.size());
        for (AccessChange change: calls) {
            change.checkChange(managerArg, availableArg);
        }
    }

    @Test
    public void testSingleRight() {
        RightGroupHandler handler = new RightGroupHandler();
        final AccessManager<Integer, HierarchicalRight> manager = new HierarchicalAccessManager<>(
                AccessTokens.getSyncExecutor(),
                AccessTokens.getSyncExecutor(),
                handler);

        HierarchicalRight right = HierarchicalRight.create(new Object());
        Set<HierarchicalRight> rightSet = Collections.singleton(right);

        AccessRequest<Integer, HierarchicalRight> readRequest
                = AccessRequest.getReadRequest(0, right);
        AccessRequest<Integer, HierarchicalRight> writeRequest
                = AccessRequest.getWriteRequest(1, right);

        RecordAction action = new RecordAction();
        ListenerRef ref = handler.addGroupListener(rightSet, null, true, action);
        assertEquals(true, ref.isRegistered());

        AccessResult<?> accessResult = manager.tryGetAccess(readRequest);
        action.checkChange(0, manager, true);
        accessResult.getAccessToken().release();
        action.checkCallCount(1);

        accessResult = manager.tryGetAccess(readRequest);
        action.checkCallCount(1);
        accessResult.getAccessToken().release();
        action.checkCallCount(1);

        accessResult = manager.tryGetAccess(writeRequest);
        action.checkChange(1, manager, false);
        accessResult.getAccessToken().release();
        action.checkChange(2, manager, true);

        ref.unregister();
        assertEquals(false, ref.isRegistered());

        accessResult = manager.tryGetAccess(writeRequest);
        action.checkCallCount(3);
        accessResult.getAccessToken().release();
        action.checkCallCount(3);
    }

    @Test
    public void testTwoLevelRight() {
        RightGroupHandler handler = new RightGroupHandler();
        final AccessManager<Integer, HierarchicalRight> manager = new HierarchicalAccessManager<>(
                AccessTokens.getSyncExecutor(),
                AccessTokens.getSyncExecutor(),
                handler);

        HierarchicalRight parentRight = HierarchicalRight.create(new Object());
        HierarchicalRight childRight = parentRight.createSubRight(new Object());
        Set<HierarchicalRight> parentRightSet = Collections.singleton(parentRight);
        Set<HierarchicalRight> childRightSet = Collections.singleton(childRight);

        AccessRequest<Integer, HierarchicalRight> parentWriteRequest
                = AccessRequest.getWriteRequest(0, parentRight);
        AccessRequest<Integer, HierarchicalRight> childWriteRequest
                = AccessRequest.getWriteRequest(1, childRight);

        RecordAction parentAction = new RecordAction();
        RecordAction childAction = new RecordAction();
        ListenerRef parentRef = handler.addGroupListener(parentRightSet, null, true, parentAction);
        ListenerRef childRef = handler.addGroupListener(childRightSet, null, true, childAction);
        assertEquals(true, childRef.isRegistered());

        assertEquals(true, parentRef.isRegistered());

        AccessResult<?> accessResult = manager.tryGetAccess(parentWriteRequest);
        parentAction.checkChange(0, manager, false);
        childAction.checkChange(0, manager, false);
        accessResult.getAccessToken().release();
        parentAction.checkChange(1, manager, true);
        childAction.checkChange(1, manager, true);

        accessResult = manager.tryGetAccess(childWriteRequest);
        parentAction.checkCallCount(2);
        childAction.checkChange(2, manager, false);
        accessResult.getAccessToken().release();
        parentAction.checkCallCount(2);
        childAction.checkChange(3, manager, true);

        parentRef.unregister();
        childRef.unregister();
        assertEquals(false, parentRef.isRegistered());
        assertEquals(false, childRef.isRegistered());

        accessResult = manager.tryGetAccess(parentWriteRequest);
        parentAction.checkCallCount(2);
        childAction.checkCallCount(4);
        accessResult.getAccessToken().release();
        parentAction.checkCallCount(2);
        childAction.checkCallCount(4);
    }

    private static class AccessChange {
        private final AccessManager<?, ?> accessManager;
        private final boolean available;

        public AccessChange(AccessManager<?, ?> accessManager, boolean available) {
            this.accessManager = accessManager;
            this.available = available;
        }

        public AccessManager<?, ?> getAccessManager() {
            return accessManager;
        }

        public boolean isAvailable() {
            return available;
        }

        public void checkChange(AccessManager<?, ?> accessManager, boolean available) {
            assertSame(accessManager, this.accessManager);
            assertEquals(available, this.available);
        }
    }

    private static class RecordAction implements AccessChangeAction {
        private final List<AccessChange> calls;

        public RecordAction() {
            this(new LinkedList<AccessChange>());
        }

        public RecordAction(List<AccessChange> recordList) {
            this.calls = recordList;
        }

        public List<AccessChange> getCalls() {
            return new ArrayList<>(calls);
        }

        @Override
        public void onChangeAccess(
                AccessManager<?, HierarchicalRight> accessManager,
                boolean available) {
            calls.add(new AccessChange(accessManager, available));
        }

        public void checkCallCount(int expected) {
            assertEquals("Unexpected number of changes.", expected, calls.size());
        }

        public void checkChange(int index, AccessManager<?, ?> accessManager, boolean available) {
            AccessChange lastChange = calls.get(index);

            lastChange.checkChange(accessManager, available);
        }
    }
}
