package org.jtrim2.stream;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an action processing a sequence of elements. If you need to process multiple
 * sequences of elements, then please see {@link SeqGroupConsumer}, or if the processing
 * is context free (i.e., you don't need to do anything before and after processing the sequence),
 * then you might want to implement {@link ElementConsumer} instead for simplicity.
 * <P>
 * The consumer must always start consuming the input stream (unless it fails with an exception
 * before that), and must only consume it exactly once.
 * <P>
 * Note that this interface can be conveniently implemented by a lambda. For example:
 * <pre>{@code
 * Path destFile = ...;
 * SeqConsumer<String> consumer = (cancelToken, seqProducer) -> {
 *   try (Writer writer = Files.newBufferedWriter(destFile, StandardCharsets.UTF_8)) {
 *     seqProducer.transferAll(cancelToken, element -> {
 *       cancelToken.checkCanceled();
 *       writer.write(element + "\n");
 *     });
 *   }
 * };
 * }</pre>
 * The above example writes each elements of the input stream into separate lines of {@code destFile}
 * while properly opening the file only once before the stream processing, and the closing the file
 * as appropriate.
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of this interface are completely implementation dependent.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be synchronization transparent.
 *
 * @param <T> the type of the elements to be processed
 *
 * @see ElementConsumer
 * @see SeqGroupConsumer
 */
public interface SeqConsumer<T> {
    /**
     * Returns a consumer consuming the whole input stream and doing nothing with the elements.
     * <P>
     * The returns consumer is reusable any number of times.
     *
     * @param <T> the type of the elements to be processed
     * @return a consumer consuming the whole input stream and doing nothing with the elements.
     *   This method never returns {@code null}.
     */
    public static <T> SeqConsumer<T> draining() {
        return ElementConsumers.drainingSeqConsumer();
    }

    /**
     * Returns a consumer processing a stream of {@code Iterable} instances, and delegating the elements of
     * the collection to the given destination consumer in the iteration order of the collection. For example,
     * if the input stream is {@code [[0, 1, 2], [3, 4]]}, then the elements sent to the given
     * destination consumer are {@code [0, 1, 2, 3, 4]} (in this order).
     * <P>
     * The returned consumer is reusability is the same as the consumer given in the argument.
     *
     * @param <T> the type of the elements to be processed by the destination consumer
     * @param <C> the type of the {@code Iterable} (e.g., {@code List})
     * @param dest the destination consumer processing the elements of each collection of the
     *   input stream. This argument cannot be {@code null}.
     * @return a consumer processing a stream of collections, and delegating the elements of the collection
     *   to the given destination consumer. This method never returns {@code null}.
     *
     * @see FluentSeqConsumer#apply(java.util.function.Function) FluentSeqConsumer.apply
     * @see FluentSeqProducer#batch(int) FluentSeqProducer.batch
     */
    public static <T, C extends Iterable<? extends T>> SeqConsumer<C> flatteningConsumer(
            SeqConsumer<? super T> dest) {

        return ElementConsumers.flatteningSeqConsumer(dest);
    }

    /**
     * Processes all the elements of the given producer. This method must consume the elements
     * of the given producer exactly once, unless it fails before even attempting it once. This method
     * must assume that the provided producer is no longer usable once this method returns.
     * <P>
     * Exceptions thrown by the provided {@code seqProducer} must be propagated to the caller, and must
     * not be wrapped (with the exception of checked exceptions not implementing {@code Exception}).
     *
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a well-behaved implementation
     *   should make a best effort check, if it does some lengthy operation. If cancellation was
     *   detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param seqProducer the producer whose elements are to be consumed. This argument cannot be {@code null}
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void consumeAll(
            CancellationToken cancelToken,
            SeqProducer<? extends T> seqProducer) throws Exception;

    /**
     * Returns a convenient fluent builder to create a complex processing action starting out from
     * this consumer.
     * <P>
     * Implementation note: This method has an appropriate default implementation, and there is normally
     * no reason to override it.
     *
     * @return a convenient fluent builder to create a complex processing action. This method
     *   never returns {@code null}.
     */
    public default FluentSeqConsumer<T> toFluent() {
        return new FluentSeqConsumer<>(this);
    }
}
