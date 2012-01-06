package org.jtrim.access.query;

import org.jtrim.concurrent.async.*;

/**
 * Defines a REW (read, evaluate, write) query whose evaluate part is
 * an {@link org.jtrim.concurrent.async.AsyncDataQuery AsyncDataQuery}.
 * <P>
 * These tasks are normally executed by a {@link RewQueryExecutor}
 * implementation.
 * <P>
 * This REW query defines task which first reads an input in the context
 * of a read {@link org.jtrim.access.AccessToken AccessToken},
 * using the result of this read queries an {@code AsyncDataQuery} whose
 * result will be written in the context of a write {@code AccessToken}.
 * Implementations of {@code RewQuery} can write the state of the query as well.
 * This write also occurs in the context of the write token. How often and when
 * the write of the state of the query occurs depends on the actual
 * implementation of the {@code RewQueryExecutor} executing this task.
 * <P>
 * Instances of this interface define a one-shot task and are not to be
 * submitted again to a {@code RewQueryExecutor}.
 * <P>
 * Tasks can be canceled by calling the {@link #cancel() cancel()} method.
 * Once a task was canceled, it will remain in the canceled state forever.
 * Whenever a task is canceled other methods are allowed to return immediately.
 * Note however that a task should be canceled through the
 * {@link java.util.concurrent.Future Future} returned by the
 * {@code RewQueryExecutor} executing this task rather than calling the
 * {@code cancel()} method of this task directly because the executor will
 * interpret spurious returns after canceling as normal return (since it has
 * no means to determine that the task was canceled).
 * <P>
 * If any part of this REW query throws an exception, the
 * {@code RewQueryExecutor} executing this task will interpret this as an
 * abnormal termination of the task. The REW query is then considered to be
 * terminated. The {@code Future} returned by the executor can be used to
 * detect such exceptions.
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
 * @param <InputType> the type of the input of the
 *   {@link #getOutputQuery() query}
 * @param <OutputType> the type of the result of the
 *   {@link #getOutputQuery() query}
 *
 * @see RewQueryExecutor
 * @author Kelemen Attila
 */
public interface RewQuery<InputType, OutputType> {
    /**
     * Returns the input for query of the
     * {@link #getOutputQuery() getOutputQuery()} method.
     * <P>
     * This method will be called in the context of the read token provided
     * to the executing {@link RewQueryExecutor}.
     * <P>
     * Note that if this method throws an exception this task is considered
     * to be completed abnormally.
     *
     * @return the input for the query part of this REW query. This method can
     *   return {@code null}, if the query allows {@code null} arguments.
     */
    public InputType readInput();

    /**
     * Returns the {@code AsyncDataQuery} for the query part of this REW query.
     * The object returned by the {@link #readInput() readInput()} method
     * will be used to query the result of this {@code AsyncDataQuery}.
     * The result (and the partial results) will be written by calling the
     * {@link #writeOutput(Object) writeOutput(OutputType)}
     * method in the context of the write token provided to the
     * {@link RewQueryExecutor} executing this REW query.
     * <P>
     * The state of the returned query will be passed to the
     * {@link #writeState(org.jtrim.concurrent.async.AsyncDataState) writeState(AsyncDataState)}
     * method at the discretion of the executor executing this REQ query.
     *
     * @return the {@code AsyncDataQuery} for the query part of this REW query.
     *   This method must never return {@code null}.
     */
    public AsyncDataQuery<InputType, OutputType> getOutputQuery();

    /**
     * Writes the result of the {@link #getOutputQuery() query}.
     * <P>
     * This method will be called in the context of the write token provided
     * to the executing {@link RewQueryExecutor}.
     * <P>
     * Note that if this method throws an exception this task is considered
     * to be completed abnormally.
     *
     * @param output the data returned by the query. This argument can
     *   be {@code null} if the query returned a {@code null} value.
     */
    public void writeOutput(OutputType output);

    /**
     * Writes the state of the {@link #getOutputQuery() query}.
     * <P>
     * This method will be called in the context of the write token provided
     * to the executing {@link RewQueryExecutor}. How many times and when
     * this method is called is left at the discretion of the executing
     * {@code RewQueryExecutor}. However when this method is called, no
     * other calls to this method have been made with a newer (more up-to-date)
     * state. This effectively means that this method cannot be called
     * concurrently with another {@code writeState} method of the same REW
     * query.
     * <P>
     * Note that if this method throws an exception this task is considered
     * to be completed abnormally.
     *
     * @param state the current state of the {@link #getOutputQuery() query}.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException implementations may throw this exception
     *   if the {@code state} argument is {@code null}, though they are not
     *   required to do so
     */
    public void writeState(AsyncDataState state);

    /**
     * Tries to a cancel this REW query. After this call any method of this
     * REW query may return immediately.
     * <P>
     * This method must be idempotent, so multiple calls must have the same
     * effect as a single call to this method.
     * <P>
     * Note that this method should only be called by the
     * {@link RewQueryExecutor} executing this REW query. To cancel this REW
     * query cancel the {@link java.util.concurrent.Future Future} returned
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
