package org.jtrim.access.task;

/**
 * Defines a REW (read, evaluate, write) task.
 * <P>
 * These tasks are normally executed by a {@link RewTaskExecutor}
 * implementation.
 * <P>
 * REW tasks are tasks that can be defined by the following three parts:
 * <ol>
 *   <li>Reading the input</li>
 *   <li>Calculating the output from the input</li>
 *   <li>Writing/Displaying the output</li>
 * </ol>
 * Reading the input executes in the context of a specified read
 * {@link org.jtrim.access.AccessToken AccessToken} while writing the output
 * executes in the context of a specified write {@code AccessToken}. These
 * access tokens are specified to the {@code RewTaskExecutor} when the REW task
 * is submitted. The task will also have a chance to display its progress and a
 * task specific state during the evaluate part.
 * <P>
 * A REW task can be canceled at any time (even before started). Once it has
 * been canceled it will remain in the canceled state. Whenever a task is
 * canceled other methods are allowed to return immediately. Also notice that
 * the {@link #evaluate(java.lang.Object, org.jtrim.access.task.RewTaskReporter) evaluate}
 * part is allowed to throw an {@link InterruptedException}. This exception can
 * be thrown even if the task was canceled but the executing thread was not
 * interrupted. Note however that a task should be canceled through the
 * {@link java.util.concurrent.Future Future} returned by the
 * {@code RewTaskExecutor} executing this task rather than calling the
 * {@link #cancel() cancel()} method of this task directly because the executor
 * will interpret spurious returns after canceling as normal return
 * (since it has no means to determine that the task was canceled).
 * <P>
 * If any part of this REW task throws an exception, the {@code RewTaskExecutor}
 * executing this task will interpret this as an abnormal termination of the
 * task. The REW task is then considered to be terminated. The {@code Future}
 * returned by the executor can be used to detect such exceptions.
 * <P>
 * Instances of this interface define a one-shot task and are not to be
 * submitted again to a {@code RewTaskExecutor}.
 *
 * <h3>Thread safety</h3>
 * Reading the input and writing the output are executed in the context's of
 * the appropriate {@code AccessToken}s so they not necessarily need to be
 * thread-safe (depending on the {@code AccessToken}s). Canceling this task can
 * however occur concurrently with all the other methods including the reads and
 * writes.
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <InputType> the type of the input of this REW task.
 *   See: {@link #readInput() readInput()}
 * @param <OutputType> the type of the output of this REW task.
 *   See: {@link #writeOutput(Object) writeOutput(OutputType)}
 *
 * @see AbstractRewTask
 * @see RewTaskExecutor
 * @author Kelemen Attila
 */
public interface RewTask<InputType, OutputType> {
    /**
     * Returns the input for the
     * {@link #evaluate(java.lang.Object, org.jtrim.access.task.RewTaskReporter) evaluate}
     * part of this REW task. In case this REW task was canceled, this method
     * may return immediately.
     * <P>
     * This method will be called in the context of the read token provided
     * to the executing {@link RewTaskExecutor}.
     * <P>
     * Note that if this method throws an exception this task is considered
     * to be completed abnormally.
     *
     * @return the input for the evaluate part of this REW query. This method
     *   can return {@code null}, if the evaluate part allows {@code null}
     *   arguments.
     */
    public InputType readInput();

    /**
     * Calculates the output of this REW task. The output returned will be
     * written/displayed by the
     * {@link #writeOutput(java.lang.Object) writeOutput(OutputType)} method.
     * <P>
     * This method will most likely execute by an
     * {@link java.util.concurrent.ExecutorService ExecutorService} and should
     * respond to interrupts. This method may wait for external events and
     * may take a while to finish.
     * <P>
     * Tasks are also recommended to report their current state of execution
     * using the provided {@link RewTaskReporter}. The provided
     * {@code RewTaskReporter} should be called by this method from time to
     * time, so an up-to-date value can be displayed. Note that the executor
     * is free to discard any reports which were submitted after this method
     * returns.
     *
     * @param input the input argument for the evaluate part returned by the
     *   {@link #readInput() readInput()} method. This argument can be
     *   {@code null} if the {@code readInput()} method returned a {@code null}
     *   value.
     * @param reporter the {@code RewTaskReporter} which can be used to report
     *   the current progress of this method. This argument is never
     *   {@code null} and can be used from other threads as well.
     * @return the output of this REW task. This method may return {@code null}
     *   if the {@link #writeOutput(java.lang.Object) writeOutput(OutputType)}
     *   method allows {@code null} arguments.
     *
     * @throws InterruptedException may be thrown if this thread was interrupted
     *   or this task was canceled. Implementations are not required to throw
     *   this exception but recommended to do so. If they choose to throw this
     *   exception, they can also clear the interrupted status of the current
     *   thread.
     */
    public OutputType evaluate(InputType input, RewTaskReporter reporter)
            throws InterruptedException;

    /**
     * Writes or displays the output of this REW task.
     * <P>
     * This method is called in the context of the write
     * {@link org.jtrim.access.AccessToken AccessToken} provided to the
     * executing {@link RewTaskExecutor}. In case this REW task
     * was canceled, this method may return immediately.
     * <P>
     * Once this method returns, this REW task is considered to be finished.
     *
     * @param output the output of the
     *   {@link #evaluate(java.lang.Object, org.jtrim.access.task.RewTaskReporter) evaluate}
     *   part of this REW task. This argument can be {@code null} if the
     *   {@code evalute} method can return {@code null}.
     */
    public void writeOutput(OutputType output);

    /**
     * Writes or displays the progress of the evaluate part of this REW task.
     * <P>
     * This method is called in the context of the write
     * {@link org.jtrim.access.AccessToken AccessToken} provided to the
     * executing {@link RewTaskExecutor}.
     * <P>
     * This method can assume that the provided progress state is the most
     * recent which was reported. That is: No more recent progress state was
     * submitted to this method.
     *
     * @param progress the current state of progress of the {@code evaluate}
     *   method. This argument can never be {@code null}.
     */
    public void writeProgress(TaskProgress<?> progress);

    /**
     * Writes or displays a user specific data which was reported during the
     * {@link #evaluate(java.lang.Object, org.jtrim.access.task.RewTaskReporter) evaluate}
     * part of this REW task.
     * <P>
     * This method is called in the context of the write
     * {@link org.jtrim.access.AccessToken AccessToken} provided to the
     * executing {@link RewTaskExecutor}.
     * <P>
     * The reported datas will be submitted to this method in the order
     * they were reported.
     *
     * @param data the data which was reported by the {@code evaluate} method.
     *   This argument can be {@code null}, if the {@code evaluate} method
     *   can report {@code null} values.
     */
    public void writeData(Object data);

    /**
     * Tries to a cancel this REW task. After this call any method of this
     * REW task may return immediately and the {@code evaluate} method may
     * also throw a {@link InterruptedException}.
     * <P>
     * This method must be idempotent, so multiple calls must have the same
     * effect as a single call to this method.
     * <P>
     * Note that this method should only be called by the
     * {@link RewTaskExecutor} executing this REW task. To cancel this REW
     * task cancel the {@link java.util.concurrent.Future Future} returned
     * by the executor.
     *
     * <h5>Thread safety</h5>
     * This method must be completely thread-safe and can be called from
     * any thread.
     * <h6>Synchronization transparency</h6>
     * This method is not required to be <I>synchronization transparent</I> but
     * must not wait for any asynchronous or external task to finish.
     */
    public void cancel();
}
