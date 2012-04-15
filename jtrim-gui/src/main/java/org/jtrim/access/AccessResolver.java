package org.jtrim.access;

/**
 * The interface used to determine if {@link AccessToken AccessTokens} can be
 * canceled to allow a new {@code AccessToken} to be created.
 * <P>
 * Implementation can base their decision on many things like the importance
 * of the conflicting tasks or may ask for the permission of the user.
 * <P>
 * This listener was designed to use with REW (Read, Evaluate, Write) tasks
 * and queries found in the {@code org.jtrim.access.task} and
 * {@code org.jtrim.access.task.query} packages.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface does not need to be thread-safe.
 * <h4>Synchronization transparency</h4>
 * Instances of this interface are not required to be
 * <I>synchronization transparent</I> in general.
 *
 * @param <IDType> the type of the access ID of the used
 *   {@link AccessToken access tokens} (see {@link AccessToken#getAccessID()})
 *
 * @see org.jtrim.access.task.RewTaskExecutors
 * @author Kelemen Attila
 */
public interface AccessResolver<IDType> {
    /**
     * Decides if the passed conflicting {@link AccessToken AccessTokens} can be
     * stopped in order to allow the new task to execute.
     * <P>
     * This method need not return immediately in particular it may ask to
     * permission of the user if canceling ongoing tasks is allowed.
     * <P>
     * This method is defined in the context of REW (Read, Evaluate, Write)
     * tasks and queries and hardly makes sense in other scenarios.
     *
     * @param readRequestResult the result of the request for the read part
     *   of the REW task or query. The result may contain some conflicting
     *   tokens needed to be shutted down before proceeding with the new task.
     *   This argument cannot be {@code null}.
     * @param writeRequestResult the result of the request for the write part
     *   of the REW task or query. The result may contain some conflicting
     *   tokens needed to be shutted down before proceeding with the new task.
     *   This argument cannot be {@code null}.
     * @return {@code true} if the conflicting tokens can be shutted down,
     *   {@code false} otherwise
     *
     * @throws NullPointerException may thrown if one of its arguments is
     *   {@code null}. Implementation does not need to throw this exception
     *   but are recommended to do so in order to catch errors early.
     *
     * @see org.jtrim.access.task.RewTaskExecutors
     */
    public boolean canContinue(
            AccessResult<IDType> readRequestResult,
            AccessResult<IDType> writeRequestResult);
}
