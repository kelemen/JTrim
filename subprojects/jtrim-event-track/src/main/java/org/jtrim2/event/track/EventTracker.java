package org.jtrim2.event.track;

import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;

/**
 * Defines an interface for tracking the causality between events.
 * <P>
 * An instance of {@code EventTracker} must be able to keep track of the
 * causality between all the events triggered through the
 * {@link TrackedListenerManager#onEvent(Object) onEvent} method of the
 * {@link TrackedListenerManager} instances created by the same
 * {@code EventTracker}. In every such {@code onEvent} call, the
 * {@code EventTracker} will collect the causes leading to this {@code onEvent}
 * call and pass these causes as well as the specified event argument to
 * the event handlers registered with {@code TrackedListenerManager}.
 * <P>
 * Every implementation of {@code EventTracker} must support at least the
 * following ways to recognize the causality between events:
 * <ul>
 *  <li>
 *   If the {@code onEvent} method is called from within another {@code onEvent}
 *   method of a {@code TrackedListenerManager} of the same
 *   {@code EventTracker} (directly or indirectly). That is, a registered
 *   listener triggers an event of the {@code EventTracker} within itself.
 *  </li>
 *  <li>
 *   If a task is submitted to a tracked executor (created by
 *   {@link #createTrackedExecutor(TaskExecutor) createTrackedExecutor} or
 *   {@link #createTrackedExecutorService(TaskExecutorService) createTrackedExecutorService}),
 *   the task will have the cause which was the cause of submitting the same
 *   task.
 *  </li>
 * </ul>
 * Apart from the above mentioned ways, implementations may define additional
 * ways to recognize causality between events.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface must be safe to be accessed by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are required to be
 * <I>synchronization transparent</I>. Note that only the methods provided
 * by the {@code EventTracker} must be <I>synchronization transparent</I>,
 * the {@code TrackedListenerManager} is not.
 *
 * @see LinkedEventTracker
 *
 * @author Kelemen Attila
 */
public interface EventTracker {
    /**
     * Returns a {@link TrackedListenerManager} to which dispatched events can
     * be keep tracked of by this {@code EventTracker}.
     * <P>
     * The {@code TrackedListenerManager} is returned based on an event kind,
     * which usually specifies a method of a listener interface and the type
     * of arguments accepted by the listeners. In case
     * {@code TrackedListenerManager} instances are requested multiple times
     * using the same event kind and argument type, the returned
     * {@code TrackedListenerManager} instances are functionally equivalent
     * (although they may not necessarily equal based on their {@code equals}
     * method).
     * <P>
     * The {@link TrackedListenerManager#onEvent(Object) onEvent} method of the
     * returned {@code TrackedListenerManager} will forward events to listeners
     * registered to it. If the {@code TrackedListenerManager} is requested
     * multiple times from this {@code EventTracker} registering a listener
     * with or dispatching event to either of them has the same effect.
     *
     * @param <ArgType> the type of the argument accepted listeners of the
     *   returned {@code TrackedListenerManager}
     * @param eventKind the kind of events can be possibly triggered by
     *   the returned {@code TrackedListenerManager}. This argument cannot be
     *   {@code null}.
     * @param argType the {@code Class} object defining the type of the
     *   arguments accepted by the listeners of the returned
     *   {@code TrackedListenerManager}. Note that for different {@code argType}
     *   objects, independent {@code TrackedListenerManager} instances are
     *   returned (i.e.: registering listeners to one of them does not affect
     *   the other and the same is with dispatching events) even if the class
     *   types are related (one is the subclass of the other). This argument
     *   cannot be {@code null}.
     * @return the {@link TrackedListenerManager} to which dispatched events can
     *   be keep tracked of by this {@code EventTracker}. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public <ArgType> TrackedListenerManager<ArgType> getManagerOfType(
            Object eventKind,
            Class<ArgType> argType);

    /**
     * Returns an executor which submits tasks submitted to the to the executor
     * specified but remembers the causing events from the time the task was
     * submitted. That is, if the submitted (by the {@code execute} method of
     * the returned executor) tasks calls the {@code onEvent} method of a
     * {@code TrackedListenerManager} created by this {@code EventTracker}, it
     * will have the same causes as if it had been called directly where the
     * task was submitted to the executor. This is also true for the cleanup
     * tasks.
     *
     * @param executor the executor to which tasks are forwarded to by the
     *   returned executor. This argument cannot be {@code null}.
     * @return the executor which submits tasks submitted to the to the executor
     *   specified but remembers the causing events from the time the task was
     *   submitted. This method never returns {@code null}.
     *
     * @see #createTrackedExecutorService(TaskExecutorService)
     */
    public TaskExecutor createTrackedExecutor(TaskExecutor executor);

    /**
     * Returns an executor which submits tasks submitted to the to the executor
     * specified but remembers the causing events from the time the task was
     * submitted. That is, if the submitted (by the {@code execute} or
     * {@code submit}, etc. methods of the returned executor) tasks calls the
     * {@code onEvent} method of a {@code TrackedListenerManager} created by
     * this {@code EventTracker}, it will have the same causes as if it had been
     * called directly where the task was submitted to the executor. This is
     * also true for the cleanup tasks.
     *
     * @param executor the executor to which tasks are forwarded to by the
     *   returned executor. This argument cannot be {@code null}.
     * @return the executor which submits tasks submitted to the to the executor
     *   specified but remembers the causing events from the time the task was
     *   submitted. This method never returns {@code null}.
     *
     * @see #createTrackedExecutor(TaskExecutor)
     */
    public TaskExecutorService createTrackedExecutorService(
            TaskExecutorService executor);
}
