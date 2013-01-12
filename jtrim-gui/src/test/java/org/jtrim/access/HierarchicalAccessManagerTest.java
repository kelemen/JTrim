package org.jtrim.access;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.junit.*;
import org.mockito.ArgumentCaptor;

import static org.jtrim.access.AccessState.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class HierarchicalAccessManagerTest {
    private static final Random RAND = new Random();
    private static final Object JOKER = new Object();

    private final HierarchicalRight[] singletonRights;

    public HierarchicalAccessManagerTest() {
        singletonRights = new HierarchicalRight[10];
        for (int i = 0; i < singletonRights.length; i++) {
            singletonRights[i] = HierarchicalRight.create(Integer.toString(i));
        }
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

    private static HierarchicalAccessManager<String> createManager(
            AccessStateListener<HierarchicalRight> listener) {

        if (listener == null) {
            return new HierarchicalAccessManager<>();
        }

        return new HierarchicalAccessManager<>(
                SyncTaskExecutor.getSimpleExecutor(),
                listener);
    }

    private static void assertArrayEqualsWithJoker(Object[] expected, Object[] actual) {
        if (expected.length != actual.length) {
            fail("Array length differs. Expected: " + expected.length + ". Actual: " + actual.length);
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != JOKER) {
                if (!Objects.equals(expected[i], actual[i])) {
                    fail("Elements at index " + i + " differ. Expected: " + expected[i] + ". Actual: " + actual[i]);
                }
            }
        }
    }

    /**
     * Tests the event notifications when only read rights are requested.
     */
    @Test
    public void testNotificationsRead() {
        StateStore listener = new StateStore();
        HierarchicalAccessManager<String> manager = createManager(listener);

        AccessToken<?> token1 = manager.tryGetAccess(new AccessRequest<>("r1",
                new HierarchicalRight[]{ singletonRights[0], singletonRights[1] },
                null)).getAccessToken();
        assertNotNull(token1);

        listener.assertState(singletonRights[0], READONLY);
        listener.assertState(singletonRights[1], READONLY);

        AccessToken<?> token2 = manager.tryGetAccess(
                AccessRequest.getReadRequest("r2", singletonRights[0])).getAccessToken();
        assertNotNull(token2);

        listener.assertState(singletonRights[0], READONLY);
        listener.assertState(singletonRights[1], READONLY);

        token1.release();

        listener.assertState(singletonRights[0], READONLY);
        listener.assertState(singletonRights[1], AVAILABLE);

        AccessToken<?> token3 = manager.tryGetAccess(
                AccessRequest.getReadRequest("r3", singletonRights[1])).getAccessToken();
        assertNotNull(token3);

        listener.assertState(singletonRights[0], READONLY);
        listener.assertState(singletonRights[1], READONLY);

        token2.release();

        listener.assertState(singletonRights[0], AVAILABLE);
        listener.assertState(singletonRights[1], READONLY);

        token3.release();

        listener.assertState(singletonRights[0], AVAILABLE);
        listener.assertState(singletonRights[1], AVAILABLE);
    }

    /**
     * Tests the event notifications when only write rights are requested.
     */
    @Test
    public void testNotificationsWrite() {
        StateStore listener = new StateStore();
        HierarchicalAccessManager<String> manager = createManager(listener);

        AccessToken<?> token1 = manager.tryGetAccess(new AccessRequest<>("r1",
                null,
                new HierarchicalRight[]{ singletonRights[0], singletonRights[1] })).getAccessToken();
        assertNotNull(token1);

        listener.assertState(singletonRights[0], UNAVAILABLE);
        listener.assertState(singletonRights[1], UNAVAILABLE);

        AccessToken<?> token2 = manager.tryGetAccess(
                AccessRequest.getWriteRequest("r2", singletonRights[0])).getAccessToken();
        assertNull(token2);

        listener.assertState(singletonRights[0], UNAVAILABLE);
        listener.assertState(singletonRights[1], UNAVAILABLE);

        token1.release();

        listener.assertState(singletonRights[0], AVAILABLE);
        listener.assertState(singletonRights[1], AVAILABLE);

        AccessToken<?> token3 = manager.tryGetAccess(
                AccessRequest.getWriteRequest("r3", singletonRights[1])).getAccessToken();
        assertNotNull(token3);

        listener.assertState(singletonRights[0], AVAILABLE);
        listener.assertState(singletonRights[1], UNAVAILABLE);

        token3.release();

        listener.assertState(singletonRights[0], AVAILABLE);
        listener.assertState(singletonRights[1], AVAILABLE);
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
        HierarchicalRight parentRight = singletonRights[0];
        HierarchicalRight childRight = parentRight.createSubRight(new Object());

        AccessResult<?> requestResult;
        AccessResult<?> requestResult2;
        HierarchicalAccessManager<String> manager = createManager(null);

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

    private HierarchicalRight getRandomRight() {
        return singletonRights[RAND.nextInt(singletonRights.length)];
    }

    private HierarchicalRight[] getRandomRights(int rightCount) {
        HierarchicalRight[] result = new HierarchicalRight[rightCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = getRandomRight();
        }
        return result;
    }

    private AccessRequest<String, HierarchicalRight> getRandomWriteRequest(int rightCount) {
        return new AccessRequest<>("random", null, getRandomRights(rightCount));
    }

    private static <V> boolean containsAny(Set<V> set, Collection<? extends V> elements) {
        for (V element: elements) {
            if (set.contains(element)) {
                return true;
            }
        }

        return false;
    }

    private void tryGetRandomWrite(
            HierarchicalAccessManager<String> manager, int rightCount,
            Set<HierarchicalRight> usedRights,
            Map<AccessToken<?>, AccessRequest<String, HierarchicalRight>> tokens) {

        AccessRequest<String, HierarchicalRight> request = getRandomWriteRequest(rightCount);
        AccessResult<?> result = manager.tryGetAccess(request);
        if (result.isAvailable()) {
            assertFalse(containsAny(usedRights, request.getWriteRights()));
            usedRights.addAll(request.getWriteRights());
            tokens.put(result.getAccessToken(), request);
        }
        else {
            assertTrue(containsAny(usedRights, request.getWriteRights()));
        }
    }

    private void removeToken(Set<HierarchicalRight> usedRights,
            Map<AccessToken<?>, AccessRequest<String, HierarchicalRight>> tokens,
            AccessToken<?> toRemove) {

        toRemove.release();
        AccessRequest<String, HierarchicalRight> request = tokens.remove(toRemove);
        usedRights.removeAll(request.getWriteRights());
    }

    private void removeRandomToken(
            Set<HierarchicalRight> usedRights,
            Map<AccessToken<?>, AccessRequest<String, HierarchicalRight>> tokens) {
        int tokenCount = tokens.size();
        if (tokenCount == 0) {
            return;
        }
        AccessToken<?> toRemove = (AccessToken<?>)tokens.keySet().toArray()[RAND.nextInt(tokenCount)];
        removeToken(usedRights, tokens, toRemove);
    }

    private AccessState getRightState(HierarchicalAccessManager<String> manager, HierarchicalRight right) {
        Set<HierarchicalRight> reads = new HashSet<>();
        Set<HierarchicalRight> writes = new HashSet<>();

        manager.getRights(reads, writes);
        boolean inRead = reads.contains(right);
        boolean inWrite = writes.contains(right);
        assertFalse(inRead && inWrite);

        if (inRead) {
            return READONLY;
        }
        if (inWrite) {
            return UNAVAILABLE;
        }
        return AVAILABLE;
    }

    private void testWriteStates(
            HierarchicalAccessManager<String> manager,
            Set<HierarchicalRight> currentRights,
            StateStore listener) {

        for (int i = 0; i < singletonRights.length; i++) {
            HierarchicalRight right = singletonRights[i];
            AccessState listenerState = listener.getState(right);
            AccessState setState = currentRights.contains(right)
                    ? UNAVAILABLE
                    : AVAILABLE;
            AccessState managerState = getRightState(manager, right);

            assertEquals(setState, listenerState);
            assertEquals(setState, managerState);
        }
    }

    /**
     * Requests and removes random tokens (requests write rights only).
     */
    @Test
    public void testRandomWrites() {
        Set<HierarchicalRight> currentRights = new HashSet<>();
        Map<AccessToken<?>, AccessRequest<String, HierarchicalRight>> tokens
                = new IdentityHashMap<>(128);

        StateStore listener = new StateStore();
        HierarchicalAccessManager<String> manager = createManager(listener);

        for (int i = 0; i < 100; i++) {
            if (i > 5 && RAND.nextBoolean()) {
                removeRandomToken(currentRights, tokens);
            }
            else {
                int rightCount = RAND.nextInt(3) + 1;
                tryGetRandomWrite(manager, rightCount, currentRights, tokens);
            }

            testWriteStates(manager, currentRights, listener);
        }

        for (AccessToken<?> token: new ArrayList<>(tokens.keySet())) {
            removeToken(currentRights, tokens, token);
            testWriteStates(manager, currentRights, listener);
        }
    }

    @SuppressWarnings("unchecked")
    private static AccessStateListener<HierarchicalRight> mockStateListener() {
        return mock(AccessStateListener.class);
    }

    private static ArgumentCaptor<HierarchicalRight> rightArgCaptor() {
        return ArgumentCaptor.forClass(HierarchicalRight.class);
    }

    private static ArgumentCaptor<AccessState> stateArgCaptor() {
        return ArgumentCaptor.forClass(AccessState.class);
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        manager.getScheduledAccess(AccessRequest.getWriteRequest(writeID, right));
        AccessResult<String> readResult = manager.getScheduledAccess(AccessRequest.getReadRequest(readID, right));
        readResult.release();

        verifyListener(manager, listener,
                new Object[]{right},
                new Object[]{UNAVAILABLE});
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeReadRightButRemainsWriteRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> readResult = manager.getScheduledAccess(AccessRequest.getReadRequest(readID, right));
        manager.getScheduledAccess(AccessRequest.getWriteRequest(writeID, right));
        readResult.release();

        verifyListener(manager, listener,
                new Object[]{right, right},
                new Object[]{READONLY, UNAVAILABLE});
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeReadRightButRemainsWriteRight3() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> writeResult = manager.tryGetAccess(AccessRequest.getWriteRequest(writeID, right));
        AccessResult<String> readResult = manager.getScheduledAccess(AccessRequest.getReadRequest(readID, right));
        readResult.release();

        assertTrue(writeResult.isAvailable());
        verifyListener(manager, listener,
                new Object[]{right},
                new Object[]{UNAVAILABLE});
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeReadRightButRemainsWriteRight4() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> readResult = manager.tryGetAccess(AccessRequest.getReadRequest(readID, right));
        manager.getScheduledAccess(AccessRequest.getWriteRequest(writeID, right));
        readResult.release();

        assertTrue(readResult.isAvailable());
        verifyListener(manager, listener,
                new Object[]{right, right},
                new Object[]{READONLY, UNAVAILABLE});
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight1() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> writeResult = manager.getScheduledAccess(AccessRequest.getWriteRequest(writeID, right));
        manager.getScheduledAccess(AccessRequest.getReadRequest(readID, right));
        writeResult.release();

        verifyListener(manager, listener,
                new Object[]{right, right},
                new Object[]{UNAVAILABLE, READONLY});
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        manager.getScheduledAccess(AccessRequest.getReadRequest(readID, right));
        AccessResult<String> writeResult = manager.getScheduledAccess(AccessRequest.getWriteRequest(writeID, right));
        writeResult.release();

        verifyListener(manager, listener,
                new Object[]{right, right, right},
                new Object[]{READONLY, UNAVAILABLE, READONLY});
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight3() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> writeResult = manager.tryGetAccess(AccessRequest.getWriteRequest(writeID, right));
        manager.getScheduledAccess(AccessRequest.getReadRequest(readID, right));
        writeResult.release();

        assertTrue(writeResult.isAvailable());

        verifyListener(manager, listener,
                new Object[]{right, right},
                new Object[]{UNAVAILABLE, READONLY});
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void removeWriteRightButRemainsReadRight4() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String readID = "ID-READ";
        String writeID = "ID-WRITE";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> readResult = manager.tryGetAccess(AccessRequest.getReadRequest(readID, right));
        AccessResult<String> writeResult = manager.getScheduledAccess(AccessRequest.getWriteRequest(writeID, right));
        writeResult.release();

        assertTrue(readResult.isAvailable());
        verifyListener(manager, listener,
                new Object[]{right, right, right},
                new Object[]{READONLY, UNAVAILABLE, READONLY});
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void testTwoReadsAreAllowed() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertTrue(result2.isAvailable());

        verifyListener(manager, listener,
                new Object[]{right},
                new Object[]{READONLY});
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener,
                new Object[]{right},
                new Object[]{READONLY});
        checkOnlyReadRights(manager, right);
    }

    @Test
    public void testWriteIsNotAllowedAfterWrite() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener,
                new Object[]{right},
                new Object[]{UNAVAILABLE});
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void testReadIsNotAllowedAfterWrite() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);

        verifyListener(manager, listener,
                new Object[]{right},
                new Object[]{UNAVAILABLE});
        checkOnlyWriteRights(manager, right);
    }

    @Test
    public void testAddParentReadRightAfterChildReadRight1() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, child));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, parent));

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
                new Object[]{child, parent, parent},
                new Object[]{READONLY, READONLY, AVAILABLE});
    }

    @Test
    public void testAddParentReadRightAfterChildReadRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, child));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getReadRequest(id2, parent));

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
                new Object[]{child, parent, parent},
                new Object[]{READONLY, READONLY, AVAILABLE});
    }

    @Test
    public void testAddParentWriteRightAfterChildWriteRight1() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, child));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, parent));

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
                new Object[]{child, parent, parent},
                new Object[]{UNAVAILABLE, UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testAddParentWriteRightAfterChildWriteRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, child));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, parent));

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
                new Object[]{child, parent, parent},
                new Object[]{UNAVAILABLE, UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testAddChildWriteRightAfterParentWriteRight1() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, parent));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));

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
                new Object[]{parent, parent},
                new Object[]{UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testAddChildWriteRightAfterParentWriteRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, parent));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));

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
                new Object[]{parent, parent},
                new Object[]{UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testAddParentWriteRightAfterChildReadRight1() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, child));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, parent));

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
                new Object[]{child, parent, parent},
                new Object[]{READONLY, UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testAddParentWriteRightAfterChildReadRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, child));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, parent));

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
                new Object[]{child, parent, parent, parent},
                new Object[]{READONLY, UNAVAILABLE, READONLY, AVAILABLE});
    }

    @Test
    public void testAddChildWriteRightAfterParentReadRight1() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, parent));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));

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
                new Object[]{parent, child, parent},
                new Object[]{READONLY, UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testAddChildWriteRightAfterParentReadRight2() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, parent));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));

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
                new Object[]{parent, child, JOKER, parent},
                new Object[]{READONLY, UNAVAILABLE, READONLY, AVAILABLE});
    }

    @Test
    public void testReadPreventsWrite() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";

        HierarchicalRight right = HierarchicalRight.create(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getReadRequest(id1, right));
        AccessResult<String> result2 = manager.tryGetAccess(AccessRequest.getWriteRequest(id2, right));

        assertTrue(result1.isAvailable());
        assertTrue(result1.getBlockingTokens().isEmpty());

        assertFalse(result2.isAvailable());
        checkBlockingTokens(result2, id1);
    }

    @Test
    public void testWritePreventsRead() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
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

    private static void verifyListener(
            HierarchicalAccessManager<String> manager,
            AccessStateListener<HierarchicalRight> listener,
            Object[] expectedRights,
            Object[] expectedStates) {
        assert expectedStates.length == expectedRights.length : "Bug in the test code: Illegal argument for verifyListener";

        ArgumentCaptor<AccessState> stateArgs = stateArgCaptor();
        ArgumentCaptor<HierarchicalRight> rightArgs = rightArgCaptor();
        verify(listener, times(expectedRights.length)).onEnterState(
                same(manager),
                rightArgs.capture(),
                stateArgs.capture());

        assertArrayEqualsWithJoker(
                expectedRights,
                rightArgs.getAllValues().toArray());

        assertArrayEquals(
                expectedStates,
                stateArgs.getAllValues().toArray());

        verifyNoMoreInteractions(listener);
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id1, parent));
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, grandChild, id1, id2);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                new Object[]{parent},
                new Object[]{UNAVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder132() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id1, parent));
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                new Object[]{parent},
                new Object[]{UNAVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder213() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, grandChild, id2);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id1, parent));
        checkBlockedForWrite(manager, parent, id1, id2);
        checkBlockedForWrite(manager, child, id1, id2);
        checkBlockedForWrite(manager, grandChild, id1, id2);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                new Object[]{child, parent},
                new Object[]{UNAVAILABLE, UNAVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder231() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        checkBlockedForWrite(manager, parent, id2);
        checkBlockedForWrite(manager, child, id2);
        checkBlockedForWrite(manager, grandChild, id2);
        checkAvailableForWrite(manager, child2);
        checkBlockedForWrite(manager, grandChild2, id2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));
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
                new Object[]{child, parent},
                new Object[]{UNAVAILABLE, UNAVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder312() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));
        checkBlockedForWrite(manager, parent, id3);
        checkBlockedForWrite(manager, child, id3);
        checkBlockedForWrite(manager, grandChild, id3);
        checkAvailableForWrite(manager, child2);
        checkAvailableForWrite(manager, grandChild2);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id1, parent));
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1);

        manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);
        checkBlockedForWrite(manager, child2, id1);
        checkBlockedForWrite(manager, grandChild2, id1, id2);

        verifyListener(manager, listener,
                new Object[]{grandChild, parent},
                new Object[]{UNAVAILABLE, UNAVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder321() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

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
                new Object[]{grandChild, child, parent},
                new Object[]{UNAVAILABLE, UNAVAILABLE, UNAVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder123() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight child2 = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());
        HierarchicalRight grandChild2 = child.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, parent));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        AccessResult<String> result3 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));

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
                new Object[]{parent, parent},
                new Object[]{UNAVAILABLE, AVAILABLE});
    }

    // Order meanings: 1 = parent, 2 = child, 3 = grandchild
    @Test
    public void test3LevelWriteRightAcquireOrder123ReleaseOrder231() {
        AccessStateListener<HierarchicalRight> listener = mockStateListener();
        HierarchicalAccessManager<String> manager = createManager(listener);

        String id1 = "ID1";
        String id2 = "ID2";
        String id3 = "ID3";

        HierarchicalRight parent = HierarchicalRight.create(new Object());
        HierarchicalRight child = parent.createSubRight(new Object());
        HierarchicalRight grandChild = child.createSubRight(new Object());

        AccessResult<String> result1 = manager.tryGetAccess(AccessRequest.getWriteRequest(id1, parent));
        AccessResult<String> result2 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id2, child));
        AccessResult<String> result3 = manager.getScheduledAccess(AccessRequest.getWriteRequest(id3, grandChild));

        checkBlockedForWrite(manager, parent, id1, id2, id3);
        checkBlockedForWrite(manager, child, id1, id2, id3);
        checkBlockedForWrite(manager, grandChild, id1, id2, id3);

        result2.release();
        checkBlockedForWrite(manager, parent, id1, id3);
        checkBlockedForWrite(manager, child, id1, id3);
        checkBlockedForWrite(manager, grandChild, id1, id3);

        result3.release();
        checkBlockedForWrite(manager, parent, id1);
        checkBlockedForWrite(manager, child, id1);
        checkBlockedForWrite(manager, grandChild, id1);

        result1.release();
        checkNoRights(manager);

        verifyListener(manager, listener,
                new Object[]{parent, parent},
                new Object[]{UNAVAILABLE, AVAILABLE});
    }

    @Test
    public void testToString() {
        HierarchicalAccessManager<String> manager = createManager(null);

        manager.tryGetAccess(AccessRequest.getWriteRequest("r1", singletonRights[0]));
        manager.tryGetAccess(AccessRequest.getWriteRequest("r2", singletonRights[1]));
        manager.tryGetAccess(AccessRequest.getWriteRequest("r3", singletonRights[1]));

        manager.tryGetAccess(AccessRequest.getReadRequest("r4", singletonRights[0]));
        manager.tryGetAccess(AccessRequest.getReadRequest("r5", singletonRights[2]));
        manager.tryGetAccess(AccessRequest.getReadRequest("r6", singletonRights[2]));
        manager.tryGetAccess(AccessRequest.getReadRequest("r7", singletonRights[2]));

        assertNotNull(manager.toString());
    }

    private class StateStore implements AccessStateListener<HierarchicalRight> {
        private final ConcurrentMap<HierarchicalRight, AccessState> states;

        public StateStore() {
            this.states = new ConcurrentHashMap<>();
        }

        @Override
        public void onEnterState(
                AccessManager<?, HierarchicalRight> accessManager,
                HierarchicalRight right, AccessState state) {
            if (state == AVAILABLE) {
                states.remove(right);
            }
            else {
                states.put(right, state);
            }
        }

        public AccessState getState(HierarchicalRight right) {
            AccessState state = states.get(right);
            return state != null ? state : AVAILABLE;
        }

        public void assertState(HierarchicalRight right, AccessState state) {
            assertEquals(state, getState(right));
        }
    }
}
