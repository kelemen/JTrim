package org.jtrim2.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRegistries;
import org.jtrim2.utils.ExceptionHelper;

/**
 * A utility class containing static convenience methods for
 * {@link AccessToken AccessTokens}.
 */
public final class AccessTokens {
    private AccessTokens() {
        throw new AssertionError();
    }

    /**
     * Registers a listener to be notified when all of the provided tokens
     * were released.
     * <P>
     * The listener will be notified even if the specified tokens were already
     * released and will not be notified more than once. The listener must
     * expect to be called from various threads, including the current thread
     * (i.e.: from within this {@code addReleaseAllListener} method call).
     *
     * <h3>Unregistering the listener</h3>
     * Unlike the general {@code removeXXX} idiom in Swing listeners, this
     * listener can be removed using the returned reference.
     * <P>
     * The unregistering of the listener is not necessary, the listener will
     * automatically be unregistered once it has been notified.
     *
     * @param tokens the collection of {@link AccessToken access tokens} to be
     *   checked when they will be released. When all of these tokens
     *   are released, the listener will be notified. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements. This collection
     *   is allowed to be empty, in which case the listener will be notified
     *   immediately in this method call.
     * @param listener the {@code Runnable} whose {@code run} method will be
     *   invoked when all the specified access tokens are released. This
     *   argument cannot be {@code null}.
     * @return the reference to the newly registered listener which can be
     *   used to remove this newly registered listener, so it will no longer
     *   be notified of the release event. Note that this method may return
     *   an unregistered listener if all the access tokens were already released
     *   prior to this method call. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} or any of the specified tokens in the collection is
     *   {@code null}
     */
    public static ListenerRef addReleaseAllListener(
            Collection<? extends AccessToken<?>> tokens,
            final Runnable listener) {
        ExceptionHelper.checkNotNullElements(tokens, "tokens");
        Objects.requireNonNull(listener, "listener");

        final Collection<ListenerRef> listenerRefs = new LinkedList<>();
        final AtomicInteger activeCount = new AtomicInteger(1);
        for (AccessToken<?> token: tokens) {
            activeCount.incrementAndGet();
            ListenerRef listenerRef = token.addReleaseListener(() -> {
                if (activeCount.decrementAndGet() == 0) {
                    listener.run();
                }
            });
            listenerRefs.add(listenerRef);
        }

        if (activeCount.decrementAndGet() == 0) {
            listener.run();
        }

        ListenerRef[] refArray = listenerRefs.toArray(new ListenerRef[listenerRefs.size()]);
        return ListenerRegistries.combineListenerRefs(refArray);
    }

    /**
     * Registers a listener to be notified when at least one of the provided
     * tokens has been released. The listener will not be notified multiple
     * times. The listener must expect to be called from various threads,
     * including the current thread (i.e.: from within this
     * {@code addReleaseAllListener} method call).
     *
     * <h3>Unregistering the listener</h3>
     * Unlike the general {@code removeXXX} idiom in Swing listeners, this
     * listener can be removed using the returned reference.
     * <P>
     * The unregistering of the listener is not necessary, the listener will
     * automatically be unregistered once it has been notified.
     *
     * @param tokens the collection of {@link AccessToken access tokens} to be
     *   checked when they will be released. When at least one of these
     *   tokens is released, the listener will be notified. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements. This collection
     *   is allowed to be empty, in which case the listener will never be
     *   notified.
     * @param listener the {@code Runnable} whose {@code run} method will be
     *   invoked when all the specified access tokens are released. This
     *   argument cannot be {@code null}.
     * @return the reference to the newly registered listener which can be
     *   used to remove this newly registered listener, so it will no longer
     *   be notified of the release event. Note that this method may return
     *   an unregistered listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} or any of the specified tokens in the collection is
     *   {@code null}
     */
    public static ListenerRef addReleaseAnyListener(
            Collection<? extends AccessToken<?>> tokens,
            final Runnable listener) {
        ExceptionHelper.checkNotNullElements(tokens, "tokens");
        Objects.requireNonNull(listener, "listener");

        final AtomicReference<ListenerRef[]> listenerRefsRef = new AtomicReference<>(null);
        final ListenerRef unregisterAll = () -> {
            ListenerRef[] refs = listenerRefsRef.getAndSet(null);
            if (refs != null) {
                for (ListenerRef ref: refs) {
                    ref.unregister();
                }
            }
        };

        final AtomicBoolean released = new AtomicBoolean(false);
        Collection<ListenerRef> listenerRefs = new ArrayList<>(tokens.size());

        Runnable idempotentListener = Tasks.runOnceTask(() -> {
            released.set(true);
            listener.run();
            unregisterAll.unregister();
        });
        for (AccessToken<?> token: tokens) {
            ListenerRef listenerRef = token.addReleaseListener(idempotentListener);
            listenerRefs.add(listenerRef);
        }
        listenerRefsRef.set(listenerRefs.toArray(new ListenerRef[listenerRefs.size()]));
        if (released.get())  {
            idempotentListener.run();
        }

        return unregisterAll;
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
     * both passed {@code AccessToken}s. Releasing the returned token will
     * release both tokens passed in the arguments but the returned token is
     * considered released even if only one of the underlying tokens is
     * released.
     * <P>
     * Note that the order of the passed {@code AccessToken}s does not
     * matter.
     * <P>
     * Note that the resulting token will submit tasks to executors of both
     * specified tokens to detect
     * {@link AccessManager#getScheduledAccess(AccessRequest) scheduled tokens}.
     * These tasks are not the same as the ones passed to the resulting token.
     *
     * @param <IDType> the type of {@link AccessToken#getAccessID() ID}
     *   of the returned {@link AccessToken}
     * @param id the access ID of the returned access token. This argument
     *   cannot be {@code null}.
     * @param tokens the tokens to be combined. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements. Also this
     *   array must contain at least a single element.
     * @return the new combined {@link AccessToken}. This method never returns
     *   {@code null}.
     *
     * @throws IllegalArgumentException thrown if there are zero tokens
     *   specified in the arguments
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <IDType> AccessToken<IDType> combineTokens(IDType id, AccessToken<?>... tokens) {
        return new CombinedToken<>(id, tokens);
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
            token.releaseAndCancel();
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
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the
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
}
