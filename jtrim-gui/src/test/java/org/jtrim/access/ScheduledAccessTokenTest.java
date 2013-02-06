package org.jtrim.access;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.*;
import org.junit.*;
import org.mockito.ArgumentMatcher;

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
            AccessToken<String> subToken,
            AccessToken<String> blockingToken) {
        return createBlockedToken(subToken, Collections.singleton(blockingToken));
    }

    private static ScheduledAccessToken<String> createBlockedToken(
            Collection<AccessToken<String>> blockingTokens) {

        return createBlockedToken(AccessTokens.createToken("INDEPENDENT"), blockingTokens);
    }

    private static ScheduledAccessToken<String> createBlockedToken(
            AccessToken<String> subToken,
            Collection<AccessToken<String>> blockingTokens) {

        return ScheduledAccessToken.newToken(subToken, blockingTokens);
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

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);
        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

        verifyZeroInteractions(task);
        blockingToken.release();
        verify(task).execute(any(CancellationToken.class));

        verify(subToken).createExecutor(any(TaskExecutor.class));
    }

    @Test
    public void testCancelBeforeAllowedToExecute() {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        TaskExecutor executor = token.createExecutor(manualExecutor);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        CancelableTask task = mock(CancelableTask.class);
        executor.execute(cancelSource.getToken(), task, null);

        verifyZeroInteractions(task);
        cancelSource.getController().cancel();
        blockingToken.release();
        manualExecutor.executeCurrentlySubmitted();
        verifyZeroInteractions(task);
    }

    @Test
    public void testCancelBeforeAllowedToExecuteWithCleanup() throws Exception {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        TaskExecutor executor = token.createExecutor(manualExecutor);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(cancelSource.getToken(), task, cleanup);

        verifyZeroInteractions(task, cleanup);
        cancelSource.getController().cancel();
        blockingToken.release();
        manualExecutor.executeCurrentlySubmitted();
        verifyZeroInteractions(task);
        verify(cleanup).cleanup(eq(true), argThat(canceledOrNull()));
    }

    @Test
    public void testReleaseBeforeAllowedToExecute() throws Exception {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        TaskExecutor executor = token.createExecutor(manualExecutor);

        token.release();

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

        blockingToken.release();
        manualExecutor.executeCurrentlySubmitted();
        verifyZeroInteractions(task);
    }

    @Test
    public void testReleaseBeforeAllowedToExecuteWithCleanup() throws Exception {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        TaskExecutor executor = token.createExecutor(manualExecutor);

        token.release();

        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

        blockingToken.release();
        manualExecutor.executeCurrentlySubmitted();
        verifyZeroInteractions(task);
        verify(cleanup).cleanup(eq(true), argThat(canceledOrNull()));
    }

    @Test
    public void testErrorInExecutorDoesNotCorruptUs() throws Exception {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);

        TaskExecutor buggyExecutor = mock(TaskExecutor.class);
        doThrow(RuntimeException.class)
                .when(buggyExecutor)
                .execute(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));

        TaskExecutor executorWithBuggy = token.createExecutor(buggyExecutor);

        executorWithBuggy.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);
        executorWithBuggy.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);

        try {
            blockingToken.release();
        } catch (RuntimeException ex) {
            // Ignore exceptions from buggyExecutor
        }

        TaskExecutor newExecutor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        newExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

        verify(task).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(false, null);
    }

    private void testConcurrentStartAndSubmit(int numberOfTasks) throws Exception {
        assert numberOfTasks > 0;

        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        final AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);

        List<Runnable> tasks = new LinkedList<>();
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        CancelableTask[] submittedTasks = new CancelableTask[numberOfTasks];
        for (int i = 0; i < numberOfTasks; i++) {
            final CancelableTask submittedTask = mock(CancelableTask.class);
            submittedTasks[i] = submittedTask;
            Runnable submitTask = new Runnable() {
                @Override
                public void run() {
                    executor.execute(Cancellation.UNCANCELABLE_TOKEN, submittedTask, null);
                }
            };
            tasks.add(submitTask);
        }

        tasks.add(new Runnable() {
            @Override
            public void run() {
                blockingToken.release();
            }
        });

        Tasks.runConcurrently(tasks.toArray(new Runnable[tasks.size()]));

        for (int i = 0; i < numberOfTasks; i++) {
            verify(submittedTasks[i]).execute(any(CancellationToken.class));
        }
    }

    @Test(timeout = 20000)
    public void testConcurrentStartAndSubmit() throws Exception {
        int numberOfTasks = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            testConcurrentStartAndSubmit(numberOfTasks);
        }
    }

    private static ArgumentMatcher<Throwable> canceledOrNull() {
        return new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Object argument) {
                if (argument instanceof OperationCanceledException) {
                    return true;
                }
                return argument == null;
            }
        };
    }

    @Test
    public void testToString() {
        assertNotNull(createBlockedToken(AccessTokens.createToken("")).toString());
        assertNotNull(createUnblockedToken().toString());
    }
}
