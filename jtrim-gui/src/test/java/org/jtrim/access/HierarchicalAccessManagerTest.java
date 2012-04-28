package org.jtrim.access;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.concurrent.SyncTaskExecutor;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author Kelemen Attila
 */
public class HierarchicalAccessManagerTest {
    private static final Random RAND = new Random();

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
            return new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        }

        return new HierarchicalAccessManager<>(
                SyncTaskExecutor.getSimpleExecutor(),
                SyncTaskExecutor.getSimpleExecutor(),
                listener);
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

        listener.assertState(singletonRights[0], AccessState.READONLY);
        listener.assertState(singletonRights[1], AccessState.READONLY);

        AccessToken<?> token2 = manager.tryGetAccess(
                AccessRequest.getReadRequest("r2", singletonRights[0])).getAccessToken();
        assertNotNull(token2);

        listener.assertState(singletonRights[0], AccessState.READONLY);
        listener.assertState(singletonRights[1], AccessState.READONLY);

        token1.shutdown();

        listener.assertState(singletonRights[0], AccessState.READONLY);
        listener.assertState(singletonRights[1], AccessState.AVAILABLE);

        AccessToken<?> token3 = manager.tryGetAccess(
                AccessRequest.getReadRequest("r3", singletonRights[1])).getAccessToken();
        assertNotNull(token3);

        listener.assertState(singletonRights[0], AccessState.READONLY);
        listener.assertState(singletonRights[1], AccessState.READONLY);

        token2.shutdown();

        listener.assertState(singletonRights[0], AccessState.AVAILABLE);
        listener.assertState(singletonRights[1], AccessState.READONLY);

        token3.shutdown();

        listener.assertState(singletonRights[0], AccessState.AVAILABLE);
        listener.assertState(singletonRights[1], AccessState.AVAILABLE);
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

        listener.assertState(singletonRights[0], AccessState.UNAVAILABLE);
        listener.assertState(singletonRights[1], AccessState.UNAVAILABLE);

        AccessToken<?> token2 = manager.tryGetAccess(
                AccessRequest.getWriteRequest("r2", singletonRights[0])).getAccessToken();
        assertNull(token2);

        listener.assertState(singletonRights[0], AccessState.UNAVAILABLE);
        listener.assertState(singletonRights[1], AccessState.UNAVAILABLE);

        token1.shutdown();

        listener.assertState(singletonRights[0], AccessState.AVAILABLE);
        listener.assertState(singletonRights[1], AccessState.AVAILABLE);

        AccessToken<?> token3 = manager.tryGetAccess(
                AccessRequest.getWriteRequest("r3", singletonRights[1])).getAccessToken();
        assertNotNull(token3);

        listener.assertState(singletonRights[0], AccessState.AVAILABLE);
        listener.assertState(singletonRights[1], AccessState.UNAVAILABLE);

        token3.shutdown();

        listener.assertState(singletonRights[0], AccessState.AVAILABLE);
        listener.assertState(singletonRights[1], AccessState.AVAILABLE);
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

        toRemove.shutdown();
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
            return AccessState.READONLY;
        }
        if (inWrite) {
            return AccessState.UNAVAILABLE;
        }
        return AccessState.AVAILABLE;
    }

    private void testWriteStates(
            HierarchicalAccessManager<String> manager,
            Set<HierarchicalRight> currentRights,
            StateStore listener) {

        for (int i = 0; i < singletonRights.length; i++) {
            HierarchicalRight right = singletonRights[i];
            AccessState listenerState = listener.getState(right);
            AccessState setState = currentRights.contains(right)
                    ? AccessState.UNAVAILABLE
                    : AccessState.AVAILABLE;
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
            if (state == AccessState.AVAILABLE) {
                states.remove(right);
            }
            else {
                states.put(right, state);
            }
        }

        public AccessState getState(HierarchicalRight right) {
            AccessState state = states.get(right);
            return state != null ? state : AccessState.AVAILABLE;
        }

        public void assertState(HierarchicalRight right, AccessState state) {
            assertEquals(state, getState(right));
        }
    }
}
