package org.jtrim2.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.concurrent.SyncTaskExecutor;
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
public class HierarchicalAccessManagerTest {
    public HierarchicalAccessManagerTest() {
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

    private static HierarchicalAccessManager<String> createManager() {
        return new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
    }

    private static HierarchicalAccessManager<String> createManager(
            AccessChangeListener<String, HierarchicalRight> listener) {

        HierarchicalAccessManager<String> result = createManager();
        result.addAccessChangeListener(listener);
        return result;
    }

    private static void checkAvailable(
            HierarchicalAccessManager<?> accessManager,
            int expectedBlockingCount,
            Collection<HierarchicalRight> readRights,
            Collection<HierarchicalRight> writeRights) {
        boolean available = accessManager.isAvailable(readRights, writeRights);
        assertEquals("Wrong isAvailable result.", expectedBlockingCount == 0, available);

        int numberOfTokens = accessManager.getBlockingTokens(readRights, writeRights).size();
        assertEquals("Invalid number of blocking tokens.", expectedBlockingCount, numberOfTokens);
    }

    @Test
    public void testIsAvailable() {
        HierarchicalRight parentRight = HierarchicalRight.create("RIGHT");
        HierarchicalRight childRight = parentRight.createSubRight(new Object());

        AccessResult<?> requestResult;
        AccessResult<?> requestResult2;
        HierarchicalAccessManager<String> manager = createManager();

        // Acquire READ: parent
        requestResult = manager.tryGetAccess(AccessRequest.getReadRequest("", parentRight));
        assertEquals(true, requestResult.isAvailable());

        checkAvailable(manager, 0,
                Collections.singleton(parentRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 0,
                Collections.singleton(childRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 1,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(parentRight));

        checkAvailable(manager, 1,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(childRight));

        requestResult.getAccessToken().release();

        // Acquire READ: parent, child
        requestResult = manager.tryGetAccess(AccessRequest.getReadRequest("", parentRight));
        assertEquals(true, requestResult.isAvailable());
        requestResult2 = manager.tryGetAccess(AccessRequest.getReadRequest("", childRight));
        assertEquals(true, requestResult.isAvailable());

        checkAvailable(manager, 0,
                Collections.singleton(parentRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 0,
                Collections.singleton(childRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 2,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(parentRight));

        checkAvailable(manager, 2,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(childRight));

        requestResult.getAccessToken().release();
        requestResult2.getAccessToken().release();

        // Acquire WRITE: parent
        requestResult = manager.tryGetAccess(AccessRequest.getWriteRequest("", parentRight));
        assertEquals(true, requestResult.isAvailable());

        checkAvailable(manager, 1,
                Collections.singleton(parentRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 1,
                Collections.singleton(childRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 1,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(parentRight));

        checkAvailable(manager, 1,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(childRight));

        requestResult.getAccessToken().release();

        // Acquire WRITE: child
        requestResult = manager.tryGetAccess(AccessRequest.getWriteRequest("", childRight));
        assertEquals(true, requestResult.isAvailable());

        checkAvailable(manager, 1,
                Collections.singleton(parentRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 1,
                Collections.singleton(childRight),
                Collections.<HierarchicalRight>emptySet());

        checkAvailable(manager, 1,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(parentRight));

        checkAvailable(manager, 1,
                Collections.<HierarchicalRight>emptySet(),
                Collections.singleton(childRight));

        requestResult.getAccessToken().release();
    }

    @SuppressWarnings("unchecked")
    private static AccessChangeListener<String, HierarchicalRight> mockChangeListener() {
        return mock(AccessChangeListener.class);
    }

    private static ArgumentCaptor<HierarchicalRight> rightArgCaptor() {
        return ArgumentCaptor.forClass(HierarchicalRight.class);
    }

    private static void checkRights(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight[] expectedReadRights,
            HierarchicalRight[] expectedWriteRights) {

        Set<HierarchicalRight> expectedReadSet = new HashSet<>(Arrays.asList(expectedReadRights));
        Set<HierarchicalRight> expectedWriteSet = new HashSet<>(Arrays.asList(expectedWriteRights));

        Set<HierarchicalRight> readRights = new HashSet<>();
        Set<HierarchicalRight> writeRights = new HashSet<>();
        manager.getRights(readRights, writeRights);

        assertEquals("read rights", expectedReadSet, readRights);
        assertEquals("write rights", expectedWriteSet, writeRights);
    }

    private static void checkOnlyWriteRights(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight... expectedWriteRights) {
        checkRights(manager, new HierarchicalRight[0], expectedWriteRights);
    }

    private static void checkOnlyReadRights(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight... expectedReadRights) {
        checkRights(manager, expectedReadRights, new HierarchicalRight[0]);
    }

    private static void checkNoRights(HierarchicalAccessManager<String> manager) {
        checkRights(manager, new HierarchicalRight[0], new HierarchicalRight[0]);
        checkAvailableForWrite(manager, HierarchicalRight.create());
    }

    @Test
    public void removeReadRightButRemainsWriteRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        manager.getScheduledAccess(writeRequest);
        AccessResult<String> readResult = manager.getScheduledAccess(readRequest);
        readResult.release();

        verifyListener(manager, listener,
                acq(writeRequest), acq(readRequest), release(readRequest));
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeReadRightButRemainsWriteRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        AccessResult<String> readResult = manager.getScheduledAccess(readRequest);
        manager.getScheduledAccess(writeRequest);
        readResult.release();

        verifyListener(manager, listener,
                acq(readRequest), acq(writeRequest), release(readRequest));
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeReadRightButRemainsWriteRight3() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        AccessResult<String> writeResult = manager.tryGetAccess(writeRequest);
        AccessResult<String> readResult = manager.getScheduledAccess(readRequest);
        readResult.release();

        assertTrue(writeResult.isAvailable());
        verifyListener(manager, listener,
                acq(writeRequest), acq(readRequest), release(readRequest));
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeReadRightButRemainsWriteRight4() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        AccessResult<String> readResult = manager.tryGetAccess(readRequest);
        manager.getScheduledAccess(writeRequest);
        readResult.release();

        assertTrue(readResult.isAvailable());
        verifyListener(manager, listener,
                acq(readRequest), acq(writeRequest), release(readRequest));
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        AccessResult<String> writeResult = manager.getScheduledAccess(writeRequest);
        manager.getScheduledAccess(readRequest);
        writeResult.release();

        verifyListener(manager, listener,
                acq(writeRequest), acq(readRequest), release(writeRequest));
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        manager.getScheduledAccess(readRequest);
        AccessResult<String> writeResult = manager.getScheduledAccess(writeRequest);
        writeResult.release();

        verifyListener(manager, listener,
                acq(readRequest), acq(writeRequest), release(writeRequest));
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight3() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        AccessResult<String> writeResult = manager.tryGetAccess(writeRequest);
        manager.getScheduledAccess(readRequest);
        writeResult.release();

        assertTrue(writeResult.isAvailable());

        verifyListener(manager, listener,
                acq(writeRequest), acq(readRequest), release(writeRequest));
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight4() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(readID, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(writeID, right);

        AccessResult<String> readResult = manager.tryGetAccess(readRequest);
        AccessResult<String> writeResult = manager.getScheduledAccess(writeRequest);
        writeResult.release();

        assertTrue(readResult.isAvailable());
        verifyListener(manager, listener,
                acq(readRequest), acq(writeRequest), release(writeRequest));
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void testTwoReadsAreAllowed() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest1 = AccessRequest.getReadRequest(id1, right);
        AccessRequest<String, HierarchicalRight> readRequest2 = AccessRequest.getReadRequest(id2, right);

        AccessResult<String> result1 = manager.tryGetAccess(readRequest1);
        AccessResult<String> result2 = manager.tryGetAccess(readRequest2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        verifyListener(manager, listener,
                acq(readRequest1), acq(readRequest2));
        checkOnlyReadRights(manager, right);
    }

    private static void checkBlockingTokens(AccessResult<String> result, String... expectedIDs) {
        Set<String> ids = result.getBlockingIDs();
        assertEquals(new HashSet<>(Arrays.asList(expectedIDs)), ids);
    }

    private static void checkBlockedForRead(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight right,
            String... expectedBlockingIDs) {
        AccessResult<String> result = manager.tryGetAccess(AccessRequest.getReadRequest("", right));
        assertFalse(result.isAvailable());
        checkBlockingTokens(result, expectedBlockingIDs);
    }

    private static void checkAvailableForRead(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight right) {
        assertTrue(manager.isAvailable(Arrays.asList(right), Collections.<HierarchicalRight>emptySet()));
    }

    private static void checkBlockedForWrite(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight right,
            String... expectedBlockingIDs) {
        AccessResult<String> result = manager.tryGetAccess(AccessRequest.getWriteRequest("", right));
        assertFalse(result.isAvailable());
        checkBlockingTokens(result, expectedBlockingIDs);
    }

    private static void checkAvailableForWrite(
            HierarchicalAccessManager<String> manager,
            HierarchicalRight right) {
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Arrays.asList(right)));
    }

    @Test
    public void testWriteIsNotAllowedAfterRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> readRequest = AccessRequest.getReadRequest(id1, right);
        AccessRequest<String, HierarchicalRight> writeRequest = AccessRequest.getWriteRequest(id2, right);

        AccessResult<String> result1 = manager.tryGetAccess(readRequest);
        AccessResult<String> result2 = manager.tryGetAccess(writeRequest);

        assertTrue(result1.isAvailable());
        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener, acq(readRequest));
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void testWriteIsNotAllowedAfterWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, right);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, right);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);

        assertTrue(result1.isAvailable());
        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener, acq(request1));
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void testReadIsNotAllowedAfterWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, right);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getReadRequest(id2, right);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);

        assertTrue(result1.isAvailable());
        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener, acq(request1));
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void testAddParentReadRightAfterChildReadRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, child);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getReadRequest(id2, parent);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, child2, id2);

        result2.release();
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkAvailableForWrite(manager, child2);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request2), release(request1));
    }

    @Test
    public void testAddParentReadRightAfterChildReadRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, child);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getReadRequest(id2, parent);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, child2, id2);

        result1.release();
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, child2, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request1), release(request2));
    }

    @Test
    public void testAddParentWriteRightAfterChildWriteRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, child);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, parent);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id1, id2);
        checkBlockedForRead(manager, child, id1, id2);
        checkBlockedForRead(manager, child2, id2);

        result2.release();
        checkBlockedForRead(manager, parent, id1);
        checkBlockedForRead(manager, child, id1);
        checkAvailableForWrite(manager, child2);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request2), release(request1));
    }

    @Test
    public void testAddParentWriteRightAfterChildWriteRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, child);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, parent);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id1, id2);
        checkBlockedForRead(manager, child, id1, id2);
        checkBlockedForRead(manager, child2, id2);

        result1.release();
        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkBlockedForRead(manager, child2, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request1), release(request2));
    }

    @Test
    public void testAddChildWriteRightAfterParentWriteRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id1, id2);
        checkBlockedForRead(manager, child, id1, id2);
        checkBlockedForRead(manager, child2, id1);

        result2.release();
        checkBlockedForRead(manager, parent, id1);
        checkBlockedForRead(manager, child, id1);
        checkBlockedForRead(manager, child2, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request2), release(request1));
    }

    @Test
    public void testAddChildWriteRightAfterParentWriteRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id1, id2);
        checkBlockedForRead(manager, child, id1, id2);
        checkBlockedForRead(manager, child2, id1);

        result1.release();
        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkAvailableForWrite(manager, child2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request1), release(request2));
    }

    @Test
    public void testAddParentWriteRightAfterChildReadRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, child);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, parent);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkBlockedForRead(manager, child2, id2);
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, child2, id2);

        result1.release();
        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkBlockedForRead(manager, child2, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request1), release(request2));
    }

    @Test
    public void testAddParentWriteRightAfterChildReadRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, child);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, parent);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkBlockedForRead(manager, child2, id2);
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, child2, id2);

        result2.release();
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkAvailableForWrite(manager, child2);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request2), release(request1));
    }

    @Test
    public void testAddChildWriteRightAfterParentReadRight1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkAvailableForRead(manager, child2);
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, child2, id1);

        result1.release();
        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkAvailableForRead(manager, child2);
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkAvailableForWrite(manager, child2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request1), release(request2));
    }

    @Test
    public void testAddChildWriteRightAfterParentReadRight2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        checkBlockedForRead(manager, parent, id2);
        checkBlockedForRead(manager, child, id2);
        checkAvailableForRead(manager, child2);
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, child2, id1);

        result2.release();
        checkAvailableForRead(manager, parent);
        checkAvailableForRead(manager, child);
        checkAvailableForRead(manager, child2);
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, child2, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), release(request2), release(request1));
    }

    @Test
    public void testReadPreventsWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getReadRequest(id1, right);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener, acq(request1));
    }

    @Test
    public void testWritePreventsRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testWritePreventsWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testReadPreventsParentWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, child));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, parent));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testWritePreventsParentRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, child));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, parent));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testWritePreventsParentWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, child));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, parent));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testReadPreventsChildWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, parent));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, child));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testWritePreventsChildRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, parent));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, child));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testWritePreventsChildWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, parent));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, child));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testReadAllowsRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertTrue(result2.isAvailable());
        assertTrue(result2.getBlockingTokens().isEmpty());
    }

    @Test
    public void testReadAllowsParentRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, child));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, parent));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertTrue(result2.isAvailable());
        assertTrue(result2.getBlockingTokens().isEmpty());
    }

    @Test
    public void testReadAllowsChildRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, parent));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, child));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertTrue(result2.isAvailable());
        assertTrue(result2.getBlockingTokens().isEmpty());
    }

    private static RequestAndState acq(AccessRequest<String, HierarchicalRight> request) {
        return new RequestAndState(request, true);
    }

    private static RequestAndState release(AccessRequest<String, HierarchicalRight> request) {
        return new RequestAndState(request, false);
    }

    private static void verifyListener(
            HierarchicalAccessManager<String> manager,
            AccessChangeListener<String, HierarchicalRight> listener,
            RequestAndState... expected) {

        @SuppressWarnings("unchecked")
        ArgumentCaptor<AccessRequest<? extends String, ? extends HierarchicalRight>> requestArgs
                = (ArgumentCaptor)ArgumentCaptor.forClass(AccessRequest.class);

        ArgumentCaptor<Boolean> acquireArgs = ArgumentCaptor.forClass(Boolean.class);
        verify(listener, times(expected.length)).onChangeAccess(
                requestArgs.capture(),
                acquireArgs.capture());

        List<AccessRequest<? extends String, ? extends HierarchicalRight>> requestArgsList
                = new ArrayList<>(requestArgs.getAllValues());
        List<Boolean> acquireArgsList = new ArrayList<>(acquireArgs.getAllValues());

        RequestAndState[] received = new RequestAndState[expected.length];
        for (int i = 0; i < received.length; i++) {
            received[i] = new RequestAndState(requestArgsList.get(i), acquireArgsList.get(i));
        }

        assertArrayEquals(expected, received);
        verifyNoMoreInteractions(listener);
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        manager.getScheduledAccess(request1);
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(request2);
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, grandChild, id1, id2);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        manager.getScheduledAccess(request3);
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder132() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        manager.getScheduledAccess(request1);
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(request3);
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(request2);
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                acq(request1), acq(request3), acq(request2));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder213() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        manager.getScheduledAccess(request2);
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, grandChild, id2);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        manager.getScheduledAccess(request1);
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, grandChild, id1, id2);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        manager.getScheduledAccess(request3);
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                acq(request2), acq(request1), acq(request3));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder231() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        manager.getScheduledAccess(request2);
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, grandChild, id2);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        manager.getScheduledAccess(request3);
        checkBlockedForWrite(manager, parent, id2, id3);
        checkBlockedForWrite(manager, child, id2, id3);
        checkBlockedForWrite(manager, grandChild, id2, id3);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        manager.getScheduledAccess(request1);
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                acq(request2), acq(request3), acq(request1));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder312() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        manager.getScheduledAccess(request3);
        checkBlockedForWrite(manager, parent, id3);
        checkBlockedForWrite(manager, child, id3);
        checkBlockedForWrite(manager, grandChild, id3);
        checkAvailableForWrite(manager, child2);
        checkAvailableForWrite(manager, grandChild2);

        manager.getScheduledAccess(request1);
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(request2);
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                acq(request3), acq(request1), acq(request2));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder321() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));
        checkBlockedForWrite(manager, parent, id3);
        checkBlockedForWrite(manager, child, id3);
        checkBlockedForWrite(manager, grandChild, id3);
        checkAvailableForWrite(manager, child2);
        checkAvailableForWrite(manager, grandChild2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        checkBlockedForWrite(manager, parent, id2, id3);
        checkBlockedForWrite(manager, child, id2, id3);
        checkBlockedForWrite(manager, grandChild, id2, id3);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id1, parent));
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                acq(request3), acq(request2), acq(request1));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder123() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);

        result1.release();
        checkBlockedForWrite(manager, parent, id2, id3);
        checkBlockedForWrite(manager, child, id2, id3);
        checkBlockedForWrite(manager, grandChild, id2, id3);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        result2.release();
        checkBlockedForWrite(manager, parent, id3);
        checkBlockedForWrite(manager, child, id3);
        checkBlockedForWrite(manager, grandChild, id3);
        checkAvailableForWrite(manager, child2);
        checkAvailableForWrite(manager, grandChild2);

        result3.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request1), release(request2), release(request3));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder132() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);

        result1.release();
        checkBlockedForWrite(manager, parent, id2, id3);
        checkBlockedForWrite(manager, child, id2, id3);
        checkBlockedForWrite(manager, grandChild, id2, id3);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        result3.release();
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, grandChild, id2);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request1), release(request3), release(request2));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder213() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);

        result2.release();
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        result1.release();
        checkBlockedForWrite(manager, parent, id3);
        checkBlockedForWrite(manager, child, id3);
        checkBlockedForWrite(manager, grandChild, id3);
        checkAvailableForWrite(manager, child2);
        checkAvailableForWrite(manager, grandChild2);

        result3.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request2), release(request1), release(request3));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder231() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);

        result2.release();
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        result3.release();
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request2), release(request3), release(request1));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder312() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        result3.release();
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, grandChild, id1, id2);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        result1.release();
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, grandChild, id2);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request3), release(request1), release(request2));
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder321() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, child);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild);

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.getScheduledAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        result3.release();
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, grandChild, id1, id2);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        result2.release();
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request3), release(request2), release(request1));
    }

    private void testMultipleRights(int rightCount) {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight[] children = new HierarchicalRight[rightCount];

        String[] ids = new String[rightCount];
        for (int i = 0; i < children.length; i++) {
            ids[i] = "CHILD" + i;
            children[i] = parent.createSubRight(ids[i]);
        }

        AccessRequest<String, HierarchicalRight> request
                = new AccessRequest<>("MULTI-ID", null, children);
        AccessResult<String> result = manager.tryGetAccess(request);

        assertTrue(result.isAvailable());

        result.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request), release(request));
    }

    @Test
    public void testAcquireMultipleRights() {
        for (int rightCount = 2; rightCount < 6; rightCount++) {
            testMultipleRights(rightCount);
        }
    }

    @Test
    public void testUniversalRightPreventsEverythingReadWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String universalID = "UNIVERSAL-ID";

        HierarchicalRight universal = HierarchicalRight.create();
        HierarchicalRight right = HierarchicalRight.create("RIGHT");

        HierarchicalRight[] universalArray = new HierarchicalRight[]{universal};
        AccessRequest<String, HierarchicalRight> request
                = new AccessRequest<>(universalID, universalArray, universalArray);

        AccessResult<String> result = manager.tryGetAccess(request);
        checkBlockedForRead(manager, right, universalID);

        result.release();

        verifyListener(manager, listener, acq(request), release(request));
    }

    @Test
    public void testUniversalRightPreventsEverythingWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String universalID = "UNIVERSAL-ID";

        HierarchicalRight universal = HierarchicalRight.create();
        HierarchicalRight right = HierarchicalRight.create("RIGHT");

        AccessRequest<String, HierarchicalRight> request = AccessRequest.getWriteRequest(universalID, universal);

        AccessResult<String> result = manager.tryGetAccess(request);
        checkBlockedForRead(manager, right, universalID);

        result.release();

        verifyListener(manager, listener, acq(request), release(request));
    }

    @Test
    public void testUniversalRightPreventsEverythingRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String universalID = "UNIVERSAL-ID";

        HierarchicalRight universal = HierarchicalRight.create();
        HierarchicalRight right = HierarchicalRight.create("RIGHT");

        AccessRequest<String, HierarchicalRight> request = AccessRequest.getReadRequest(universalID, universal);

        AccessResult<String> result = manager.tryGetAccess(request);
        checkAvailableForRead(manager, universal);
        checkBlockedForWrite(manager, right, universalID);

        result.release();

        verifyListener(manager, listener, acq(request), release(request));
    }


    @Test
    public void testRightPreventsUniversalWrite() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String rightID = "RIGHT-ID";

        HierarchicalRight universal = HierarchicalRight.create();
        HierarchicalRight right = HierarchicalRight.create("RIGHT");

        AccessRequest<String, HierarchicalRight> request = AccessRequest.getWriteRequest(rightID, right);

        AccessResult<String> result = manager.tryGetAccess(request);
        checkBlockedForRead(manager, universal, rightID);

        result.release();

        verifyListener(manager, listener, acq(request), release(request));
    }

    @Test
    public void testRightPreventsUniversalRead() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String rightID = "RIGHT-ID";

        HierarchicalRight universal = HierarchicalRight.create();
        HierarchicalRight right = HierarchicalRight.create("RIGHT");

        AccessRequest<String, HierarchicalRight> request = AccessRequest.getReadRequest(rightID, right);

        AccessResult<String> result = manager.tryGetAccess(request);
        checkAvailableForRead(manager, universal);
        checkBlockedForWrite(manager, universal, rightID);

        result.release();

        verifyListener(manager, listener, acq(request), release(request));
    }

    // This test is repeated attaching the grand child to a different child to
    // ensure that both path of the internal ordering of the access manager is
    // tested. Therefore it is important to have the same string for the rights
    // in both tests.
    @Test
    public void testDifferentLevelRights1Release12() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight child1 = parent.createSubRight("CHILD1");
        HierarchicalRight child2 = parent.createSubRight("CHILD2");
        HierarchicalRight grandChild = child1.createSubRight("GRAND-CHILD");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, child2);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, grandChild);
        AccessRequest<String, HierarchicalRight> request3
                = new AccessRequest<>(id3, null, new HierarchicalRight[]{child2, grandChild});

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        result3.release();

        result1.release();
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request3), release(request1), release(request2));
    }

    @Test
    public void testDifferentLevelRights2Release12() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight child1 = parent.createSubRight("CHILD1");
        HierarchicalRight child2 = parent.createSubRight("CHILD2");
        HierarchicalRight grandChild = child2.createSubRight("GRAND-CHILD");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, child1);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, grandChild);
        AccessRequest<String, HierarchicalRight> request3
                = new AccessRequest<>(id3, null, new HierarchicalRight[]{child1, grandChild});

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        result3.release();

        result1.release();
        checkAvailableForWrite(manager, child1);
        checkBlockedForWrite(manager, grandChild, id2);

        result2.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request3), release(request1), release(request2));
    }

    @Test
    public void testDifferentLevelRights1Release21() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight child1 = parent.createSubRight("CHILD1");
        HierarchicalRight child2 = parent.createSubRight("CHILD2");
        HierarchicalRight grandChild = child1.createSubRight("GRAND-CHILD");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, child2);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, grandChild);
        AccessRequest<String, HierarchicalRight> request3
                = new AccessRequest<>(id3, null, new HierarchicalRight[]{child2, grandChild});

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        result3.release();

        result2.release();
        checkAvailableForWrite(manager, grandChild);
        checkBlockedForWrite(manager, child2, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request3), release(request2), release(request1));
    }

    @Test
    public void testDifferentLevelRights2Release21() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight child1 = parent.createSubRight("CHILD1");
        HierarchicalRight child2 = parent.createSubRight("CHILD2");
        HierarchicalRight grandChild = child2.createSubRight("GRAND-CHILD");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, child1);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, grandChild);
        AccessRequest<String, HierarchicalRight> request3
                = new AccessRequest<>(id3, null, new HierarchicalRight[]{child1, grandChild});

        AccessResult<String> result1 = manager.tryGetAccess(request1);
        AccessResult<String> result2 = manager.tryGetAccess(request2);
        AccessResult<String> result3 = manager.getScheduledAccess(request3);

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        result3.release();

        result2.release();
        checkAvailableForWrite(manager, grandChild);
        checkBlockedForWrite(manager, child1, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3),
                release(request3), release(request2), release(request1));
    }

    @Test
    public void testAddReadAfterWrite1() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create("RIGHT");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, right);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getReadRequest(id2, right);

        manager.getScheduledAccess(request1);
        manager.getScheduledAccess(request2);

        checkBlockedForWrite(manager, right, id1, id2);

        verifyListener(manager, listener, acq(request1), acq(request2));
    }

    @Test
    public void testAddReadAfterWrite2() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight child = parent.createSubRight("CHILD");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getReadRequest(id2, child);

        manager.getScheduledAccess(request1);
        manager.getScheduledAccess(request2);

        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);

        verifyListener(manager, listener, acq(request1), acq(request2));
    }

    @Test
    public void test2CousinsWithGrandParent() {
        AccessChangeListener<String, HierarchicalRight> listener = mockChangeListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create("PARENT");
        HierarchicalRight child1 = parent.createSubRight("CHILD1");
        HierarchicalRight child2 = parent.createSubRight("CHILD2");
        HierarchicalRight grandChild1 = child1.createSubRight("GRAND-CHILD1");
        HierarchicalRight grandChild2 = child2.createSubRight("GRAND-CHILD2");

        AccessRequest<String, HierarchicalRight> request1 = AccessRequest.getWriteRequest(id1, parent);
        AccessRequest<String, HierarchicalRight> request2 = AccessRequest.getWriteRequest(id2, grandChild1);
        AccessRequest<String, HierarchicalRight> request3 = AccessRequest.getWriteRequest(id3, grandChild2);

        AccessResult<String> result1 = manager.getScheduledAccess(request1);
        manager.getScheduledAccess(request2);
        manager.getScheduledAccess(request3);

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child1, id1, id2);
        checkBlockedForWrite(manager, child2, id1, id3);
        checkBlockedForWrite(manager, grandChild1, id1, id2);
        checkBlockedForWrite(manager, grandChild2, id1, id3);

        result1.release();
        checkBlockedForWrite(manager, parent, id2, id3);
        checkBlockedForWrite(manager, child1, id2);
        checkBlockedForWrite(manager, child2, id3);
        checkBlockedForWrite(manager, grandChild1, id2);
        checkBlockedForWrite(manager, grandChild2, id3);

        verifyListener(manager, listener,
                acq(request1), acq(request2), acq(request3), release(request1));
    }

    @Test
    public void testToString() {
        HierarchicalAccessManager<String> manager = createManager();

        HierarchicalRight riqht1 = HierarchicalRight.create("RIGHT1");
        HierarchicalRight riqht2 = HierarchicalRight.create("RIGHT2");
        HierarchicalRight riqht3 = HierarchicalRight.create("RIGHT3");

        manager.tryGetAccess(AccessRequest.getWriteRequest("r1", riqht1));
        manager.tryGetAccess(AccessRequest.getWriteRequest("r2", riqht2));
        manager.tryGetAccess(AccessRequest.getWriteRequest("r3", riqht2));

        manager.tryGetAccess(AccessRequest.getReadRequest("r4", riqht1));
        manager.tryGetAccess(AccessRequest.getReadRequest("r5", riqht3));
        manager.tryGetAccess(AccessRequest.getReadRequest("r6", riqht3));
        manager.tryGetAccess(AccessRequest.getReadRequest("r7", riqht3));

        assertNotNull(manager.toString());
    }

    private static class RequestAndState {
        private final String id;
        private final Set<HierarchicalRight> readRights;
        private final Set<HierarchicalRight> writeRights;
        private final boolean acquired;

        public RequestAndState(AccessRequest<? extends String, ? extends HierarchicalRight> request, boolean acquired) {
            this.id = request.getRequestID();
            this.readRights = new HashSet<>(request.getReadRights());
            this.writeRights = new HashSet<>(request.getWriteRights());
            this.acquired = acquired;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 67 * hash + Objects.hashCode(this.id);
            hash = 67 * hash + Objects.hashCode(this.readRights);
            hash = 67 * hash + Objects.hashCode(this.writeRights);
            hash = 67 * hash + (this.acquired ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final RequestAndState other = (RequestAndState)obj;

            if (!Objects.equals(this.id, other.id)) return false;
            if (!Objects.equals(this.readRights, other.readRights)) return false;
            if (!Objects.equals(this.writeRights, other.writeRights)) return false;
            if (this.acquired != other.acquired) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Request{" + (acquired ? "acquired" : "released") + ", id=" + id + '}';
        }
    }
}
