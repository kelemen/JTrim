package org.jtrim2.access;

import java.util.Collection;
import org.jtrim2.event.ListenerRef;

/**
 * Manages read and write access to resources. A certain access right
 * is represented by an {@link AccessToken} which allows tasks to be
 * executed on a certain {@code TaskExecutor}. The {@code TaskExecutor} will
 * only execute tasks until the right associated with the given
 * {@code AccessToken}.
 * <P>
 * No {@code AccessToken}s provided by an {@code AccessManager} can be
 * conflicting and simultaneously being active (i.e.: not released).
 * <ol>
 * <li>Two {@code AccessToken}s are said to be conflicting if their right
 * requests conflict.</li>
 * <li>Two access requests are said to be conflicting if they contain conflicting
 * rights.</li>
 * <li>Two rights are said to be conflicting if their domains intersect and
 * at least one of them is a write request.
 * rights.</li>
 * <li>If the domain of rights intersect or not is defined by the implementation
 * of the {@code AccessManager}.</li>
 * </ol>
 * Notice that two read request can never conflict.
 * <P>
 * <B>Note that {@code AccessManager} was not designed to protect from
 * malicious access to a resource.</B> The intended use of this class
 * is to allow cooperating codes to access resources that cannot be accessed
 * concurrently. Anyone may shuts down {@code AccessToken}s to gain access
 * to a resource and the specification of this interface does not allow
 * an implementation to refuse such a request. However it may fail to
 * shut down an {@code AccessToken} if the {@code AccessToken} is currently
 * executing a task.
 * <h2>Thread safety</h2>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 * <h3>Synchronization transparency</h3>
 * The methods of this class are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <IDType> the type of the request ID (see
 *   {@link AccessRequest#getRequestID()})
 * @param <RightType> the type of the rights that can be managed by the
 *   {@code AccessManager}
 *
 * @see AccessManagers
 * @see AccessToken
 * @see HierarchicalAccessManager
 */
public interface AccessManager<IDType, RightType> {
    /**
     * Adds a new listener which is to be notified whenever a new access token
     * is acquired or released. The listeners are notified in the order these
     * events occur. That is, if an event (A) happens before another event (B),
     * then the notification of the registered listeners reporting the event A,
     * happens before reporting the event B.
     * <P>
     * The listener can be removed by calling the
     * {@link ListenerRef#unregister() unregister} method of the returned
     * reference.
     *
     * @param listener the listener to be notified whenever a new access token
     *   is acquired or released. This argument cannot be {@code null}.
     * @return the {@code ListenerRef} which can be used to remove the currently
     *   added listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public ListenerRef addAccessChangeListener(AccessChangeListener<IDType, RightType> listener);

    /**
     * Returns the {@link AccessToken AccessTokens} which conflicts the
     * given read and write right request.
     * <P>
     * If no other right requests were made during and after this call
     * shutting down these {@code AccessToken}s will allow the caller
     * to immediately acquire the requested rights.
     * <P>
     * The following method prints the {@code AccessToken}s blocking a given
     * right request:
     * <pre>{@code
     * <R> void printConflicts(
     *     AccessManager<R> manager,
     *     AccessRequest<R> request) {
     *   Collection<?> conflicts = manager.getBlockingTokens(
     *     request.getReadRights(),
     *     request.getWriteRights());
     *
     *   if (conflicts.isEmpty()) {
     *     System.out.println("There are no conflicts.");
     *   }
     *   else {
     *     System.out.println("Conflicts: " + conflicts);
     *   }
     * }
     * }</pre>
     * <P>
     * Note that the returned tokens may conflict because they may contain
     * tokens that were not yet returned by a
     * {@link #getScheduledAccess(AccessRequest)} call.
     *
     * @param requestedReadRights the required read rights. This argument
     *   cannot be {@code null} but can be an empty {@code Collection}.
     * @param requestedWriteRights the required write rights. This argument
     *   cannot be {@code null} but can be an empty {@code Collection}.
     * @return the {@code AccessToken}s needed to be shutted down before
     *    being able to acquire the required rights. This method never returns
     *    {@code null}.
     *
     * @throws NullPointerException thrown if one of the arguments is
     *   {@code null}
     *
     * @see #isAvailable(Collection, Collection)
     */
    public Collection<AccessToken<IDType>> getBlockingTokens(
            Collection<? extends RightType> requestedReadRights,
            Collection<? extends RightType> requestedWriteRights);

    /**
     * Checks if the given rights are currently available for an access request.
     * That is, if this method returns {@code true}, a subsequent
     * {@code tryGetAccess} will succeed unless new access is requested before
     * the {@code tryGetAccess}.
     * <P>
     * This method returns {@code true}, if and only, if the
     * {@link #getBlockingTokens(Collection, Collection) getBlockingTokens}
     * method call would have returned an empty {@code Collection}.
     *
     * @param requestedReadRights the required read rights. This argument
     *   cannot be {@code null} but can be an empty {@code Collection}.
     * @param requestedWriteRights the required write rights. This argument
     *   cannot be {@code null} but can be an empty {@code Collection}.
     * @return {@code true} if the specified rights are available at the moment,
     *   {@code false} if not
     *
     * @throws NullPointerException thrown if one of the arguments is
     *   {@code null}
     *
     * @see #getBlockingTokens(Collection, Collection)
     */
    public boolean isAvailable(
            Collection<? extends RightType> requestedReadRights,
            Collection<? extends RightType> requestedWriteRights);

    /**
     * Tries to acquire the requested rights and returns immediately if it
     * cannot be acquired without waiting and/or releasing
     * {@link AccessToken AccessTokens}. The returned access token will use
     * the default executor to execute tasks submitted to it. This method never
     * waits for tasks to complete and always returns immediately.
     * <P>
     * This method either returns an {@code AccessToken} representing the
     * requested rights or the list of {@code AccessToken}s required to be
     * shutted down before such request can be granted. Note that even if the
     * request could have been granted the returned {@code AccessToken} can
     * already be released when the caller wants to use it.
     * <P>
     * The following method executes a task using a given right request
     * or prints the conflicting {@code AccessToken}s if it cannot be
     * done immediately:
     * <pre>{@code
     * <R> void tryExecuteTask(
     *     AccessManager<R> manager,
     *     TaskExecutor executor,
     *     AccessRequest<R> request
     *     CancelableTask task) {
     *   AccessResult<IDType> result = manager.tryGetAccess(request);
     *
     *   if (result.isAvailable()) {
     *     TaskExecutor tokenExecutor = result.getAccessToken().createExecutor(executor);
     *     tokenExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
     *   }
     *   else {
     *     System.out.println("Conflicts: " + result.getBlockingTokens());
     *   }
     * }
     * }</pre>
     * Note that the returned blocking tokens (those conflicting with the
     * requested rights) may conflict because they may contain tokens that were
     * returned by a {@link #getScheduledAccess(AccessRequest)} call.
     *
     * @param request the rights to be requested. This argument cannot be
     *   {@code null}.
     * @return the {@code AccessToken} if the request could be granted or
     *   the list of {@code AccessToken}s that needed to be shut down before
     *   the request can be granted. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the request is {@code null}
     */
    public AccessResult<IDType> tryGetAccess(
            AccessRequest<? extends IDType, ? extends RightType> request);

    /**
     * Returns an {@link AccessToken} which has the requested rights and will
     * execute tasks after all conflicting {@code AccessToken}s have been
     * released.
     * <P>
     * This method returns immediately without waiting with an
     * {@code AccessToken} to which tasks can be scheduled to but these tasks
     * will not be executed until there are active conflicting
     * {@code AccessToken}s ({@code AccessToken}s not yet shutted down).
     * The returned result will also contain the conflicting
     * {@code AccessToken}s needed to be shutted down before tasks will be
     * executed by the returned {@code AccessToken}.
     * <P>
     * The following method will print the conflicting tokens and
     * schedule a task to be executed after the conflicting tokens were
     * released:
     * <pre>{@code
     * <R> void executeTask(
     *     AccessManager<R> manager,
     *     TaskExecutor executor,
     *     AccessRequest<R> request
     *     CancelableTask task) {
     *   AccessResult<?> result = manager.getScheduledAccess(request);
     *   try {
     *     System.out.println("Conflicts: " + result.getBlockingTokens());
     *     TaskExecutor tokenExecutor = result.getAccessToken().createExecutor(executor);
     *     tokenExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
     *   } finally {
     *     result.release();
     *   }
     * }
     * }</pre>
     *
     * @param request the rights to be requested. This argument cannot be
     *   {@code null}.
     * @return the {@code AccessToken} associated with the requested rights
     *   and the list of tokens that must be shut down before a task
     *   scheduled to the returned {@code AccessToken} can execute. This method
     *   always returns a {@code non-null} {@code AccessToken}.
     *
     * @throws NullPointerException thrown if the request is {@code null}
     *
     * @see ScheduledAccessToken
     */
    public AccessResult<IDType> getScheduledAccess(
            AccessRequest<? extends IDType, ? extends RightType> request);
}
