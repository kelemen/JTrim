package org.jtrim2.stream;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a sink to which asynchronous element providers can put their received elements.
 * This is generally used to supply elements to one of the synchronous producers of {@link AsyncProducers}.
 * <P>
 * Once there won't be anymore elements added to this sink, it must be marked as finished by the
 * {@link #finish(Throwable) finish} method. Failing to finish the process might prevent the consumer
 * from ever terminating.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Adding an element to the sink might block and wait until elements are consumed and there
 * is more room for additional elements in the sink. Finishing adding elements never waits, so
 * it can be safely called while holding a lock.
 *
 * @param <T> the type of the elements which can be added to this sink
 *
 * @see AsyncProducers
 */
public interface AsyncElementSink<T> {
    /**
     * Adds an element to this sink to be processed. This method may block and wait, if it is full
     * and its consumer needs to consume some of its elements to continue.
     * <P>
     * <B>Note</B>: If this method throws an exception, that exception might originate from the
     * consumer. In this case, it is futile to try adding more elements, as this method will keep
     * failing.
     *
     * @param cancelToken the cancellation token which can signal cancellation, if waiting for
     *   adding the element should be canceled. Cancellation detection is done on a best effort
     *   basis, and there is no guarantee that it will be detected. However, if it is detected, then
     *   an {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException} will
     *   be thrown, and the element will not be added. This argument cannot be {@code null}.
     * @param element the element to be added to the queue. The sink does not support {@code null}
     *   elements, so this argument cannot be {@code null}.
     * @return {@code true} if the element was added to the sink, {@code false} if
     *   {@link #finish(Throwable) finish} was called with no failure.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation was detected. Note
     *   that it is possible that this exception is thrown if the process was finished with failure
     *   and the failure exception was this kind of exception. If such distinction need to be made,
     *   then checking the passed cancellation token is recommended. The edge case when both
     *   the cancellation token signals cancellation and the processed failed with cancellation
     *   exception, it is not possible to distinguish between the two scenarios unless the consumer
     *   can be monitored.
     * @throws Exception thrown if the process finished with a failure. Either by the consumer
     *   failing or this sink (via the {@link #finish(Throwable) finish} method).
     */
    public boolean tryPut(CancellationToken cancelToken, T element) throws Exception;

    /**
     * Marks this sink completed, and prevents more elements to be added to this sink.
     * <P>
     * If calling this method <I>happens-before</I> calling the {@link #tryPut(CancellationToken, Object) tryPut}
     * method call, then the {@code tryPut} will call will just return {@code false} without doing anything.
     * <P>
     * If the {@code tryPut} <I>happens-before</I> this method call, then previously added elements remain
     * to be processed (i.e., they are not removed due to this {@code finish} call).
     * <P>
     * If this method is called concurrently with {@code tryPut}, then any one of the above two scenarios might
     * happen (but not something else).
     * <P>
     * Calling this method multiple times has an undefined behaviour.
     *
     * @param error must be {@code null} if adding the elements completed normally, or
     *   an exception if there was an error while trying to get the elements to be added
     *   to this sink. Note that setting a failure will cause the consumer process
     *   to detect that failure and rethrow it.
     */
    public void finish(Throwable error);
}
