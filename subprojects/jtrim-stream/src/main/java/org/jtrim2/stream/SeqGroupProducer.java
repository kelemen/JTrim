package org.jtrim2.stream;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a source of zero or more sequences of elements. If you only need to provide a single
 * sequence of elements, then consider using {@link SeqProducer} instead. The producer may or may not
 * be reusable depending on the implementation. If the producer is defined to be reusable, then it should
 * produce the same elements every time it is requested for the sequence (of course, if the source is
 * an external source that is not always enforceable).
 * <P>
 * Note that this interface can be conveniently implemented by a lambda. For example:
 * <pre>{@code
 * Path fileListFile = ...;
 * SeqGroupProducer<String> producer = (cancelToken, seqConsumer) -> {
 *   try (BufferedReader source = Files.newBufferedReader(fileListFile, StandardCharsets.UTF_8)) {
 *     String fileName;
 *     while ((fileName = source.readLine()) != null) {
 *       cancelToken.checkCanceled();
 *       seqConsumer.consumeAll(cancelToken, newLineReaderProducer(Paths.get(fileName)));
 *     }
 *   }
 * };
 *
 * SeqProducer<String> newLineReaderProducer(Path srcFile) {
 *   return (cancelToken, consumer) -> {
 *     try (BufferedReader source = Files.newBufferedReader(srcFile, StandardCharsets.UTF_8)) {
 *       String line;
 *       while ((line = source.readLine()) != null) {
 *         cancelToken.checkCanceled();
 *         consumer.processElement(transformJob(line, "reader"));
 *       }
 *     }
 *   };
 * }
 * }</pre>
 * The {@code producer} in the above example code will read each line from a file, interpret
 * each line as a file name, and then will provide the lines of those files as a separate sequence.
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of this interface are completely implementation dependent.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be synchronization transparent.
 *
 * @param <T> the type of the produced elements
 *
 * @see SeqProducer
 */
public interface SeqGroupProducer<T> {
    /**
     * Returns a producer producing zero sequences (i.e., it will never make a call to the consumer).
     * <P>
     * The returned producer is reusable any number of times.
     *
     * @param <T> the type of the produced elements
     * @return a producer producing zero sequences (i.e., it will never make a call to the consumer).
     *   This method never returns {@code null}.
     */
    public static <T> SeqGroupProducer<T> empty() {
        return ElementProducers.emptySeqGroupProducer();
    }

    /**
     * Returns a producer producing the elements of the {@code Iterable} instances of each sequence produced by
     * the given producer. For example, if the given producer produces two sequences
     * {@code [[[0, 1, 2], [3, 4]], [[5, 6], [7, 8, 9]]]}, then the returned producer will produce
     * {@code [[0, 1, 2, 3, 4], [5, 6, 7, 8, 9]]}. Notice that different sequences are not merged together.
     * <P>
     * The returned producer is reusability is the same as the producer given in the argument.
     *
     * @param <T> the type of the element produced by the returned producer
     * @param src the source producer producing {@code Iterable} instances to be flattened.
     *   This argument cannot be {@code null}.
     * @return a producer producing the elements of the {@code Iterable} instances of each sequence produced by
     *   the given producer. This method never returns {@code null}.
     *
     * @see FluentSeqGroupProducer#apply(java.util.function.Function) FluentSeqGroupProducer.apply
     * @see FluentSeqGroupProducer#batch(int) FluentSeqGroupProducer.batch
     */
    public static <T> SeqGroupProducer<T> flatteningProducer(
            SeqGroupProducer<? extends Iterable<? extends T>> src) {

        return ElementProducers.flatteningSeqGroupProducer(src);
    }

    /**
     * Passes all the underlying sequences to the provided consumer. Note that an empty producer
     * will never invoke the given consumer.  This method must assume that the provided consumer is no
     * longer usable once this method returns.
     * <P>
     * Exceptions thrown by the provided {@code consumer} must be propagated to the caller, and must
     * not be wrapped (with the exception of checked exceptions not implementing {@code Exception}).
     *
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a well-behaved implementation
     *   should make a best effort check, if it does some lengthy operation. If cancellation was
     *   detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param seqConsumer the consumer to which the sequences are to be passed to.
     *   This argument cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void transferAll(CancellationToken cancelToken, SeqConsumer<? super T> seqConsumer) throws Exception;

    /**
     * A convenient method to consume all elements independent of sequences. This method call is logically
     * equivalent to calling:
     * <pre>{@code
     * transferAll(cancelToken, (consumerCancelToken, seqProducer) -> {
     *   seqProducer.transferAll(consumerCancelToken, consumer);
     * })
     * }</pre>
     *
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a well-behaved implementation
     *   should make a best effort check, if it does some lengthy operation. If cancellation was
     *   detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param consumer the consumer to which the elements are to be passed to in the order of underlying
     *   sequence (when the two elements are from the same sequence, there is no ordering guarantee between sequences).
     *   This argument cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public default void transferAllSimple(
            CancellationToken cancelToken,
            ElementConsumer<? super T> consumer) throws Exception {

        transferAll(cancelToken, ElementConsumers.contextFreeSeqConsumer(consumer));
    }

    /**
     * Returns a convenient fluent builder to create a complex producer starting out from
     * this producer.
     * <P>
     * Implementation note: This method has an appropriate default implementation, and there is normally
     * no reason to override it.
     *
     * @return a convenient fluent builder to create a complex producer. This method
     *   never returns {@code null}.
     */
    public default FluentSeqGroupProducer<T> toFluent() {
        return new FluentSeqGroupProducer<>(this);
    }
}
