package org.jtrim.access;

import java.util.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * A utility class containing static convenience methods for
 * {@link AccessManager AccessManagers}. Methods generally deal with cases
 * where only read or only write rights are required.
 *
 * @see AccessTokens
 * @author Kelemen Attila
 */
public final class AccessManagers {
    /**
     * Cannot be instantiated.
     */
    private AccessManagers() {
        throw new AssertionError();
    }

    /**
     * Tries to aquire the requested <B>read rights</B> and returns immediately
     * if it cannot be acquired without waiting.
     * <P>
     * This method only saves the caller from the need to manually instantiate
     * an {@link AccessRequest} with no write rights requested.
     *
     * @param <IDType> the type of the request ID
     * @param <RightType> the type of the rights that can be managed by the
     *   {@link AccessManager}
     * @param manager the {@link AccessManager} from which the
     *   {@code AccessToken} is requested. This argument cannot be {@code null}.
     * @param requestID the ID used to identify this request
     *   (see {@link AccessRequest#getRequestID()}). This argument cannot
     *   be {@code null}.
     * @param rights the read rights to be acquired. This argument cannot
     *   be {@code null} but can be an empty collection.
     * @return the {@code AccessToken} if the request could be granted or
     *   the list of {@code AccessToken}s that needed to be shutted down before
     *   the request can be granted. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments are
     *   {@code null}
     *
     * @see AccessManager#tryGetAccess(AccessRequest)
     */
    public static <IDType, RightType> AccessResult<IDType> tryGetReadAccess(
            AccessManager<IDType, ? super RightType> manager,
            IDType requestID, Collection<? extends RightType> rights) {

        ExceptionHelper.checkNotNullArgument(rights, "rights");
        return manager.tryGetAccess(
                new AccessRequest<>(requestID, rights, null));
    }

    /**
     * Tries to aquire the requested <B>write rights</B> and returns immediately
     * if it cannot be acquired without waiting.
     * <P>
     * This method only saves the caller from the need to manually instantiate
     * an {@link AccessRequest} with no read rights requested.
     *
     * @param <IDType> the type of the request ID
     * @param <RightType> the type of the rights that can be managed by the
     *   {@link AccessManager}
     * @param manager the {@link AccessManager} from which the
     *   {@code AccessToken} is requested. This argument cannot be {@code null}.
     * @param requestID the ID used to identify this request
     *   (see {@link AccessRequest#getRequestID()}). This argument cannot
     *   be {@code null}.
     * @param rights the write rights to be acquired. This argument cannot
     *   be {@code null} but can be an empty collection.
     * @return the {@code AccessToken} if the request could be granted or
     *   the list of {@code AccessToken}s that needed to be shutted down before
     *   the request can be granted. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments are
     *   {@code null}
     *
     * @see AccessManager#tryGetAccess(AccessRequest)
     */
    public static <IDType, RightType> AccessResult<IDType> tryGetWriteAccess(
            AccessManager<IDType, ? super RightType> manager,
            IDType requestID, Collection<? extends RightType> rights) {

        ExceptionHelper.checkNotNullArgument(rights, "rights");
        return manager.tryGetAccess(
                new AccessRequest<>(requestID, null, rights));
    }

    /**
     * Returns an {@link AccessToken} which has the requested <B>read rights</B>
     * and will execute tokens after all conflicting {@code AccessToken}s have
     * been shutted down. This method returns immediately without waiting with
     * an {@code AccessToken} to which tasks can be scheduled to but these tasks
     * will not be executed until there are active conflicting
     * {@code AccessToken}s ({@code AccessToken}s not yet shutted down).
     * <P>
     * This method only saves the caller from the need to manually instantiate
     * an {@link AccessRequest} with no write rights requested.
     *
     * @param <IDType> the type of the request ID
     * @param <RightType> the type of the rights that can be managed by the
     *   {@link AccessManager}
     * @param manager the {@link AccessManager} from which the
     *   {@code AccessToken} is requested. This argument cannot be {@code null}.
     * @param requestID the ID used to identify this request
     *   (see {@link AccessRequest#getRequestID()}). This argument cannot
     *   be {@code null}.
     * @param rights the read rights to be acquired. This argument cannot
     *   be {@code null} but can be an empty collection.
     * @return the {@code AccessToken} if the request could be granted or
     *   the list of {@code AccessToken}s that needed to be shutted down before
     *   the request can be granted. This method never returns {@code null} and
     *   will always return a {@code non-null} {@code AccessToken}.
     *
     * @throws NullPointerException thrown if any of the arguments are
     *   {@code null}
     *
     * @see AccessManager#getScheduledAccess(AccessRequest)
     */
    public static <IDType, RightType> AccessResult<IDType> getScheduledReadAccess(
            AccessManager<IDType, ? super RightType> manager,
            IDType requestID, Collection<? extends RightType> rights) {

        ExceptionHelper.checkNotNullArgument(rights, "rights");
        return manager.getScheduledAccess(
                new AccessRequest<>(requestID, rights, null));
    }

    /**
     * Returns an {@link AccessToken} which has the requested
     * <B>write rights</B> and will execute tokens after all conflicting
     * {@code AccessToken}s have been shutted down. This method returns
     * immediately without waiting with an {@code AccessToken} to which tasks
     * can be scheduled to but these tasks will not be executed until there are
     * active conflicting {@code AccessToken}s ({@code AccessToken}s not yet
     * shutted down).
     * <P>
     * This method only saves the caller from the need to manually instantiate
     * an {@link AccessRequest} with no write rights requested.
     *
     * @param <IDType> the type of the request ID
     * @param <RightType> the type of the rights that can be managed by the
     *   {@link AccessManager}
     * @param manager the {@link AccessManager} from which the
     *   {@code AccessToken} is requested. This argument cannot be {@code null}.
     * @param requestID the ID used to identify this request
     *   (see {@link AccessRequest#getRequestID()}). This argument cannot
     *   be {@code null}.
     * @param rights the write rights to be acquired. This argument cannot
     *   be {@code null} but can be an empty collection.
     * @return the {@code AccessToken} if the request could be granted or
     *   the list of {@code AccessToken}s that needed to be shutted down before
     *   the request can be granted. This method never returns {@code null} and
     *   will always return a {@code non-null} {@code AccessToken}.
     *
     * @throws NullPointerException thrown if any of the arguments are
     *   {@code null}
     *
     * @see AccessManager#getScheduledAccess(AccessRequest)
     */
    public static <IDType, RightType> AccessResult<IDType> getScheduledWriteAccess(
            AccessManager<IDType, ? super RightType> manager,
            IDType requestID, Collection<? extends RightType> rights) {

        ExceptionHelper.checkNotNullArgument(rights, "rights");
        return manager.getScheduledAccess(
                new AccessRequest<>(requestID, null, rights));
    }
}
