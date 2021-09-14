package org.jtrim2.stream;

/**
 * Defines an action processing element(s) of a stream of elements. The action
 * is either context free or has an otherwise implied context. If you need to
 * do something before or after processing the stream, consider using {@link SeqConsumer}
 * instead.
 * <P>
 * Note that implementations of this interface
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of this interface are completely implementation dependent.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be synchronization transparent.
 *
 * @param <T> the type of the element being processed
 *
 * @see SeqConsumer
 * @see SeqGroupConsumer
 */
public interface ElementConsumer<T> {
    /**
     * Returns an {@code ElementConsumer} doing nothing when invoked.
     *
     * @param <T> the type of the element processed by the returned action
     * @return an {@code ElementConsumer} doing nothing. This method never returns
     *   {@code null}.
     */
    public static <T> ElementConsumer<T> noOp() {
        return ElementConsumers.noOpConsumer();
    }

    /**
     * Processes a single element of the underlying stream. This method is called
     * for each elements of the stream exactly once (in the order defined by the stream, if any).
     *
     * @param element the element being processed. This argument may not be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call. Note that although this method
     *   does not accept a {@link org.jtrim2.cancel.CancellationToken CancellationToken} for the
     *   sake of simplicity, the context usually has one.
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void processElement(T element) throws Exception;
}
