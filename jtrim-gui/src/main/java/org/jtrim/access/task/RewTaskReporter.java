package org.jtrim.access.task;

/**
 * REW tasks must report the progress of their evaluate part through this
 * interface. This object will be provided by the REW task executor executing
 * the REW task.
 * <P>
 * Through this interface one can report updatable progress state and
 * non-updatable state. The difference between the the state information is
 * that a new updatable state information invalidates previously submitted
 * states so the old ones can be discarded. The non-updatable states must
 * however all be delivered a none must be discarded.
 * <P>
 * The reported progress states will be executed in the context of the write
 * access token used to execute the REW task and they will be submitted
 * to this write token in order.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be completely thread-safe
 * and the methods can be called from any thread. Although methods of this
 * interface can be called from any thread, REW task executors are not required
 * (but allowed) to forward calls of this method after the evaluate part of a
 * REW task completes.
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> but they must not wait for asynchronous
 * or external tasks to complete.
 *
 * @author Kelemen Attila
 */
public interface RewTaskReporter {
    /**
     * Reports the current progress of the evaluate part of the executing
     * REW task. This method is equivalent to the following invocation:
     * {@code reportProgress(new TaskProgress<Object>(progress, null))}.
     *
     * @param progress the current progress of the REW task. This argument
     *   should be within the range [0; 1] where 0 means the beginning of
     *   the execution and 1 means that the execution has completed. In case
     *   this argument is lower than 0.0, 0.0 is assumed. If this argument if
     *   higher than 1.0, 1.0 is assumed.
     *
     * @see #reportProgress(org.jtrim.access.task.TaskProgress) reportProgress(TaskProgress<?>)
     */
    void reportProgress(double progress);

    /**
     * Reports the current progress and a user specific progress state of the
     * evaluate part of the executing REW task.
     * <P>
     * The progress can be specified with a double value between 0.0 and 1.0
     * where 0.0 means the beginning of the execution and 1.0 means that
     * the execution has been completed. This method allows to pass a user
     * specific object. The meaning of this user specific object is undefined,
     * however since it is shared across threads this object is recommended to
     * immutable.
     * <P>
     * Note that any invocation of this method may cause older reported
     * progress states to be discarded if they were not yet submitted.
     * <P>
     * The submitted progress state will be delivered to the
     * {@link RewTask#writeProgress(org.jtrim.access.task.TaskProgress) writeProgress(TaskProgress<?>)}
     * method of the REW task.
     *
     * @param progress the current progress of the REW task. This argument
     *   cannot be {@code null}
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     */
    void reportProgress(TaskProgress<?> progress);

    /**
     * Reports a state of the current progress of the execution of the REW task.
     * This method will always deliver the passed object to the
     * {@link RewTask#writeData(java.lang.Object) writeData(Object)} method of
     * the REW task. Unlike the {@code reportProgress} methods invoking this
     * method will not overwrite previously submitted datas.
     * <P>
     * Use this method if the data must always be delivered, like in the case
     * of a progress caption.
     *
     * @param data the progress data to be reported. This argument can be
     *   {@code null} if the executing REW task can handle {@code null}
     *   values.
     */
    void reportData(Object data);
}
