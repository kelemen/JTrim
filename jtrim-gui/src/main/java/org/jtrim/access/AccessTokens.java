package org.jtrim.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.jtrim.utils.ExceptionHelper;

/**
 * A utility class containing static convenience methods for
 * {@link AccessToken AccessTokens}. Most methods allow actions to be
 * executed on multiple {@code AccessToken}s. The exceptions are creating
 * miscellaneous {@code AccessToken}s and making an
 * {@link AccessListener AccessListener} idempotent.
 *
 * @author Kelemen Attila
 */
public final class AccessTokens {
    private AccessTokens() {
        throw new AssertionError();
    }

    /**
     * Makes an {@link AccessListener} idempotent.
     * If the returned {@code AccessListener} is notified, it will notify the
     * specified listener exactly once.
     * <P>
     * The following method can be used to notify an {@code AccessListener}
     * exactly once after the specified {@link AccessToken}
     * terminates even if terminated before or during calling this method:
     * <pre>
     * void registerListener(AccessToken<?> token, AccessListener listener) {
     *   token.addAccessListener(AccessTokens.idempotentAccessListener(listener));
     *   if (token.isTerminated()) {
     *     listener.onLostAccess();
     *   }
     * }
     * </pre>
     *
     * @param listener the {@link AccessListener AccessListener} which is to be
     *   made idempotent. This argument cannot be {@code null}.
     * @return the new idempotent {@link AccessListener AccessListener} which
     *   will only forward the terminate event exactly once to the passed
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     */
    public static AccessListener idempotentAccessListener(AccessListener listener) {
        return new IdempotentAccessListener(listener);
    }

    /**
     * Creates a new independent {@link AccessToken}. The newly created token
     * will execute task on the specified
     * {@link java.util.concurrent.Executor Executor} (unless the
     * {@code executeNow} methods are used).
     * <P>
     * The returned token does not conflict with any other {@code AccessToken}s
     * and will function as a general purpose {@code AccessToken}.
     *
     * @param <IDType> the type of {@link AccessToken#getAccessID() ID}
     *   associated with the returned {@link AccessToken}
     * @param id the {@link AccessToken#getAccessID() ID}
     *   associated with the returned {@link AccessToken}. This argument
     *   cannot be {@code null}.
     * @param executor the {@link java.util.concurrent.Executor Executor} used
     *   to execute tasks submitted to the returned {@link AccessToken}.
     *   This argument cannot be {@code null}.
     * @return the newly created {@link AccessToken}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <IDType> AccessToken<IDType> createToken(
            IDType id, Executor executor) {

        ExceptionHelper.checkNotNullArgument(executor, "executor");
        return new GenericAccessToken<>(id, executor);
    }

    /**
     * Creates a new independent {@link AccessToken} which will execute
     * submitted tasks on the current call stack of the caller.
     * <P>
     * The returned token does not conflict with any other {@code AccessToken}s
     * and will function as a general purpose {@code AccessToken}.
     *
     * @param <IDType> the type of {@link AccessToken#getAccessID() ID}
     *   associated with the returned {@link AccessToken}
     * @param id the {@link AccessToken#getAccessID() ID}
     *   associated with the returned {@link AccessToken}. This argument
     *   cannot be {@code null}.
     * @return the newly created {@link AccessToken}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code id} is
     *   {@code null}
     */
    public static <IDType> AccessToken<IDType> createSyncToken(IDType id) {
        return createToken(id, getSyncExecutor());
    }

    /**
     * Creates a new {@link AccessToken} from two underlying
     * {@code AccessToken}s which will only execute tasks if both the passed
     * {@code AccessToken}s allow tasks to be executed. The returned token
     * effectively represents a token that is associated with the rights of
     * both passed {@code AccessToken}s.
     * <P>
     * Note that the order of the passed {@code AccessToken}s does not
     * matter.
     * <P>
     * Note that the resulting token will submit tasks to both specified
     * tokens to detect
     * {@link AccessManager#getScheduledAccess(org.jtrim.access.AccessRequest) scheduled tokens}.
     * These tasks are not the same as the ones passed to the resulting token.
     * The latter tasks will execute in the context of the specified executor
     * but may not execute before tasks scheduled to the specified tokens
     * execute.
     *
     * @param <IDType1> the type of {@link AccessToken#getAccessID() ID}
     *   of the first {@link AccessToken}
     * @param <IDType2> the type of {@link AccessToken#getAccessID() ID}
     *   of the second {@link AccessToken}
     * @param executor the {@link java.util.concurrent.Executor Executor}
     *   used to execute tasks submitted to the returned {@link AccessToken}.
     *   This argument cannot be {@code null}.
     * @param token1 the first {@link AccessToken} to be combined.
     *   This argument cannot be {@code null}.
     * @param token2 the second {@link AccessToken} to be combined.
     *   This argument cannot be {@code null}.
     * @return the new combined {@link AccessToken}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <IDType1, IDType2>
            AccessToken<MultiAccessID<IDType1, IDType2>> combineTokens(
            Executor executor, AccessToken<IDType1> token1,
            AccessToken<IDType2> token2) {
        return new CombinedToken<>(executor, token1, token2);
    }

    /**
     * Calls {@link AccessToken#release() release()} on all of the passed
     * {@link AccessToken AccessTokens}.

     * @param tokens the {@link AccessToken AccessTokens} to be terminated.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   tokens are {@code null}. Note that even if this exception is thrown
     *   some tokens might have already been terminated by this method.
     */
    public static void releaseTokens(AccessToken<?>... tokens) {
        releaseTokens(Arrays.asList(tokens));
    }

    /**
     * Calls {@link AccessToken#release() release()} on all of the passed
     * {@link AccessToken AccessTokens}.
     *
     * @param tokens the {@link AccessToken AccessTokens} to be terminated.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   tokens are {@code null}. Note that even if this exception is thrown
     *   some tokens might have already been terminated by this method.
     */
    public static void releaseTokens(Collection<? extends AccessToken<?>> tokens) {
        for (AccessToken<?> token: tokens) {
            token.release();
        }
    }

    /**
     * Shutdowns the conflicting tokens of some {@link AccessResult AccessResults}
     * and waits for them to terminate. This method first calls
     * {@link java.util.concurrent.ExecutorService#shutdownNow() shutdownNow()}
     * on all the conflicting tokens then waits for them to terminate.
     * <P>
     * This is more efficient than terminating the tokens one-by-one because
     * this way to tokens may terminate concurrently so it takes roughly as
     * much time to terminate the tokens as much as the slowest needs.
     *
     * @param results the {@link AccessResult AccessResults} of which the
     *   conflicting tokens need to be terminated. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   {@link AccessResult AccessResults} are {@code null}. Note that even if
     *   this exception is thrown some tokens might have already been terminated
     *   by this method.
     */
    public static void unblockResults(AccessResult<?>... results) {
        unblockResults(Arrays.asList(results));
    }

    /**
     * Shutdowns the conflicting tokens of some {@link AccessResult AccessResults}
     * and waits for them to terminate. This method first calls
     * {@link java.util.concurrent.ExecutorService#shutdownNow() shutdownNow()}
     * on all the conflicting tokens then waits for them to terminate.
     * <P>
     * This is more efficient than terminating the tokens one-by-one because
     * this way to tokens may terminate concurrently so it takes roughly as
     * much time to terminate the tokens as much as the slowest needs.
     *
     * @param results the {@link AccessResult AccessResults} of which the
     *   conflicting tokens need to be terminated. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   {@link AccessResult AccessResults} are {@code null}. Note that even if
     *   this exception is thrown some tokens might have already been terminated
     *   by this method.
     */
    public static void unblockResults(Collection<? extends AccessResult<?>> results) {
        for (AccessResult<?> result: results) {
            result.shutdownBlockingTokensNow();
        }

        for (AccessResult<?> result: results) {
            AccessTokens.releaseTokens(result.getBlockingTokens());
        }
    }

    /**
     * @deprecated Use {@code TaskExecutor} instead of {@code Executor}.
     */
    @Deprecated
    public static Executor getSyncExecutor() {
        return SyncExecutor.INSTANCE;
    }

    @Deprecated
    private enum SyncExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            command.run();
        }

    }
}
