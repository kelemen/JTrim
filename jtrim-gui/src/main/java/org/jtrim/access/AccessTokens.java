package org.jtrim.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;
import org.jtrim.cancel.CancellationToken;

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
     * Creates a new independent {@link AccessToken}.
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
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <IDType> AccessToken<IDType> createToken(IDType id) {
        return new GenericAccessToken<>(id);
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
     * Note that the resulting token will submit tasks to executors of both
     * specified tokens to detect
     * {@link AccessManager#getScheduledAccess(AccessRequest) scheduled tokens}.
     * These tasks are not the same as the ones passed to the resulting token.
     *
     * @param <IDType1> the type of {@link AccessToken#getAccessID() ID}
     *   of the first {@link AccessToken}
     * @param <IDType2> the type of {@link AccessToken#getAccessID() ID}
     *   of the second {@link AccessToken}
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
            AccessToken<IDType1> token1,
            AccessToken<IDType2> token2) {
        return new CombinedToken<>(token1, token2);
    }

    /**
     * Calls {@link AccessToken#releaseAndCancel() releaseAndCancel()} on all of
     * the passed {@link AccessToken AccessTokens}.

     * @param tokens the {@link AccessToken AccessTokens} to be released.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   tokens are {@code null}. Note that even if this exception is thrown
     *   some tokens might have already been released by this method.
     */
    public static void releaseAndCancelTokens(AccessToken<?>... tokens) {
        releaseAndCancelTokens(Arrays.asList(tokens));
    }

    /**
     * Calls {@link AccessToken#releaseAndCancel() releaseAndCancel()} on all of
     * the passed {@link AccessToken AccessTokens}.
     *
     * @param tokens the {@link AccessToken AccessTokens} to be released.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   tokens are {@code null}. Note that even if this exception is thrown
     *   some tokens might have already been released by this method.
     */
    public static void releaseAndCancelTokens(Collection<? extends AccessToken<?>> tokens) {
        for (AccessToken<?> token: tokens) {
            token.release();
        }
    }

    /**
     * Releases the conflicting tokens of the specified
     * {@link AccessResult AccessResults} and waits for them to be released.
     * This method first calls
     * {@link AccessToken#releaseAndCancel() releaseAndCancel()} on all the
     * conflicting tokens then waits for them to be released.
     * <P>
     * This is more efficient than releasing and waiting for the tokens
     * one-by-one because this way tokens may be released concurrently so it
     * takes roughly as much time as the slowest token needs.
     *
     * @param cancelToken the {@code CancellationToken} to be checked if this
     *   call should return (by throwing an {@code OperationCanceledException}
     *   immediately without waiting for the tokens to be released.
     * @param results the {@link AccessResult AccessResults} of which the
     *   conflicting tokens need to be released. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   {@link AccessResult AccessResults} are {@code null}. Note that even if
     *   this exception is thrown some tokens might have already been released
     *   by this method.
     */
    public static void unblockResults(
            CancellationToken cancelToken,
            AccessResult<?>... results) {
        unblockResults(cancelToken, Arrays.asList(results));
    }

    /**
     * Releases the conflicting tokens of the specified
     * {@link AccessResult AccessResults} and waits for them to be released.
     * This method first calls
     * {@link AccessToken#releaseAndCancel() releaseAndCancel()} on all the
     * conflicting tokens then waits for them to be released.
     * <P>
     * This is more efficient than releasing and waiting for the tokens
     * one-by-one because this way tokens may be released concurrently so it
     * takes roughly as much time as the slowest token needs.
     *
     * @param cancelToken the {@code CancellationToken} to be checked if this
     *   call should return (by throwing an {@code OperationCanceledException}
     *   immediately without waiting for the tokens to be released.
     * @param results the {@link AccessResult AccessResults} of which the
     *   conflicting tokens need to be released. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of the
     *   {@link AccessResult AccessResults} are {@code null}. Note that even if
     *   this exception is thrown some tokens might have already been released
     *   by this method.
     * @throws org.jtrim.cancel.OperationCanceledException thrown if the
     *   specified {@code CancellationToken} signals a cancellation request
     *   before the specified tokens were released. The tokens were requested to
     *   be released even if this exception is thrown.
     */
    public static void unblockResults(
            CancellationToken cancelToken,
            Collection<? extends AccessResult<?>> results) {
        for (AccessResult<?> result: results) {
            result.releaseAndCancelBlockingTokens();
        }

        for (AccessResult<?> result: results) {
            for (AccessToken<?> token: result.getBlockingTokens()) {
                token.awaitRelease(cancelToken);
            }
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
