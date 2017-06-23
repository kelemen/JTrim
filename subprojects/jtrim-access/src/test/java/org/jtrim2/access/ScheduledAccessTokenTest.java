package org.jtrim2.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CancelableTasks;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ScheduledAccessTokenTest {
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        verify(task).execute(any(CancellationToken.class));
    }

    private static void completedWithErrorType(CompletionStage<Void> future, Class<? extends Throwable> expected) {
        completedWithGenericError(future, (error) -> {
            if (!expected.isInstance(error)) {
                String typeName = error != null
                        ? error.getClass().getName()
                        : null;
                throw new AssertionError("Expected error: " + expected.getName() + " but received " + typeName);
            }
        });
    }

    private static void completedWithError(CompletionStage<Void> future, Throwable expected) {
        completedWithGenericError(future, (error) -> {
            assertSame(expected, error);
        });
    }

    private static void completedWithGenericError(CompletionStage<Void> future, Consumer<Throwable> errorVerifier) {
        Runnable notified = mock(Runnable.class);
        AtomicReference<Throwable> errorRef = new AtomicReference<>(new Exception("Unexpected-Error"));
        future.whenComplete((result, error) -> {
            errorRef.set(error);
            notified.run();
        });

        verify(notified).run();
        errorVerifier.accept(errorRef.get());
    }

    @Test
    public void testNoBlockingCleanup() throws Exception {
        ScheduledAccessToken<String> token = createUnblockedToken();

        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        CompletionStage<Void> future = executor.execute(
                Cancellation.UNCANCELABLE_TOKEN,
                CancelableTasks.noOpCancelableTask());

        completedWithError(future, null);
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());

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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

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
        executor.execute(cancelSource.getToken(), task);

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
        CompletionStage<Void> future = executor.execute(cancelSource.getToken(), task);

        verifyZeroInteractions(task);
        cancelSource.getController().cancel();
        blockingToken.release();
        manualExecutor.executeCurrentlySubmitted();
        verifyZeroInteractions(task);

        completedWithErrorType(future, OperationCanceledException.class);
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

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
        CompletionStage<Void> future = executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        blockingToken.release();
        manualExecutor.executeCurrentlySubmitted();
        verifyZeroInteractions(task);
        completedWithErrorType(future, OperationCanceledException.class);
    }

    @Test
    public void testErrorInExecutorDoesNotCorruptUs() throws Exception {
        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);

        TaskExecutor buggyExecutor = new TaskExecutor() {
            @Override
            public <V> CompletionStage<V> executeFunction(
                    CancellationToken cancelToken,
                    CancelableFunction<? extends V> function) {
                throw new RuntimeException("Buggy-executor");
            }
        };

        TaskExecutor executorWithBuggy = token.createExecutor(buggyExecutor);

        executorWithBuggy.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());
        executorWithBuggy.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());

        try {
            blockingToken.release();
        } catch (RuntimeException ex) {
            // Ignore exceptions from buggyExecutor
        }

        TaskExecutor newExecutor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        CompletionStage<Void> future = newExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verify(task).execute(any(CancellationToken.class));
        completedWithError(future, null);
    }

    private void testConcurrentStartAndSubmit(int numberOfTasks) throws Exception {
        assert numberOfTasks > 0;

        AccessToken<String> subToken = AccessTokens.createToken("INDEPENDENT");
        final AccessToken<String> blockingToken = AccessTokens.createToken("BLOCKING-TOKEN");

        ScheduledAccessToken<String> token = createBlockedToken(subToken, blockingToken);

        List<Runnable> tasks = new ArrayList<>();
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        CancelableTask[] submittedTasks = new CancelableTask[numberOfTasks];
        for (int i = 0; i < numberOfTasks; i++) {
            final CancelableTask submittedTask = mock(CancelableTask.class);
            submittedTasks[i] = submittedTask;
            Runnable submitTask = () -> {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, submittedTask);
            };
            tasks.add(submitTask);
        }

        tasks.add(blockingToken::release);

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

    @Test
    public void testToString() {
        assertNotNull(createBlockedToken(AccessTokens.createToken("")).toString());
        assertNotNull(createUnblockedToken().toString());
    }
}
