package org.jtrim.access;

import java.util.Collection;
import java.util.Collections;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.*;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ScheduledAccessTokenTest {
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

    private static ScheduledAccessToken<String> createUnblockedToken() {
        return ScheduledAccessToken.newToken(
                AccessTokens.createToken("INDEPENDENT"),
                Collections.<AccessToken<String>>emptySet());
    }

    private static ScheduledAccessToken<String> createBlockedToken(
            AccessToken<String> blockingToken) {
        return createBlockedToken(Collections.singleton(blockingToken));
    }

    private static ScheduledAccessToken<String> createBlockedToken(
            Collection<AccessToken<String>> blockingTokens) {

        return ScheduledAccessToken.newToken(
                AccessTokens.createToken("INDEPENDENT"),
                blockingTokens);
    }

    @Test
    public void testNoBlocking() throws Exception {
        ScheduledAccessToken<String> token = createUnblockedToken();

        CancelableTask task = mock(CancelableTask.class);
        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        verify(task).execute(any(CancellationToken.class));
    }

    @Test
    public void testNoBlockingCleanup() throws Exception {
        ScheduledAccessToken<String> token = createUnblockedToken();

        CleanupTask cleanup = mock(CleanupTask.class);
        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), cleanup);
        verify(cleanup).cleanup(false, null);
    }

    @Test
    public void testReleaseEventSimplePostRelease() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        Runnable listener = mock(Runnable.class);
        token.release();
        token.addReleaseListener(listener);
        verify(listener).run();
    }

    @Test
    public void testReleaseEventSimplePreRelease() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        Runnable listener = mock(Runnable.class);
        token.addReleaseListener(listener);
        verifyZeroInteractions(listener);
        token.release();
        verify(listener).run();
    }

    @Test
    public void testReleaseEventWithTask() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        Runnable listener = mock(Runnable.class);
        token.addReleaseListener(listener);

        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);

        token.release();
        verify(listener).run();
    }

    @Test
    public void testSubTokenRelease() {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        ScheduledAccessToken<String> token = ScheduledAccessToken.newToken(subToken,
                Collections.<AccessToken<String>>emptySet());

        token.release();
        assertTrue(subToken.isReleased());
    }

    @Test
    public void testExecutesOnlyAfterBlockingToken() throws Exception {
        AccessToken<String> subToken = spy(new DelegatedAccessToken<>(AccessTokens.createToken("INDEPENDENT")));
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(blockingToken);
        ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

        verifyZeroInteractions(task);
        blockingToken.release();
        verify(task).execute(any(CancellationToken.class));

        verify(subToken).createExecutor(executor);
    }

    @Test
    public void testToString() {
        assertNotNull(createBlockedToken(AccessTokens.createToken("")).toString());
        assertNotNull(createUnblockedToken().toString());
    }
}
