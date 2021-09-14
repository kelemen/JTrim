package org.jtrim2.stream;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an action processing zero or more sequence of elements. If you only need to process
 * a single sequence or has to do the same action for each sequence without the need to do anything before
 * or after the processing of all sequences, you might consider using {@link SeqConsumer}.
 * <P>
 * The consumer must always start consuming the sequences (unless it fails with an exception
 * before that), and must only consume it exactly once (though of course, it might need to
 * process multiple sequences).
 * <P>
 * See the following example for a simple implementation:
 * <pre>{@code
 * Path destDir = ...;
 * SeqGroupConsumer<String> consumer = (cancelToken, seqGroupProducer) -> {
 *   Files.createDirectories(destDir);
 *   AtomicInteger groupCounterRef = new AtomicInteger(0);
 *   seqGroupProducer.transferAll(cancelToken, lineWriterConsumer(() -> {
 *     return destFile.resolve(groupCounterRef.getAndIncrement() + ".txt");
 *   }));
 *   Files.createFile(destDir.resolve("signal"));
 * };
 *
 * SeqConsumer<String> lineWriterConsumer(Supplier<Path> destFileRef) {
 *   return (cancelToken, seqProducer) -> {
 *     try (Writer writer = Files.newBufferedWriter(destFileRef.get(), StandardCharsets.UTF_8)) {
 *       producer.transferAll(cancelToken, element -> {
 *         cancelToken.checkCanceled();
 *         writer.write(element + "\n");
 *       });
 *     }
 *   };
 * }
 * }</pre>
 * The above example {@code consumer} creates a separate file for each sequence of elements in
 * {@code destDir}, while also creating the destination directory only once before the processing, and
 * creating a signal file only after all the sequences were processed.
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
 * @see SeqConsumer
 */
public interface SeqGroupConsumer<T> {
    /**
     * Returns a consumer consuming each sequence and doing nothing with their elements (but consuming them).
     * <P>
     * The returns consumer is reusable any number of times.
     *
     * @param <T> the type of the elements to be processed
     * @return a consumer consuming each sequence and doing nothing with their elements.
     *   This method never returns {@code null}.
     */
    public static <T> SeqGroupConsumer<T> draining() {
        return ElementConsumers.drainingSeqGroupConsumer();
    }

    /**
     * Returns a consumer processing streams of {@code Iterable} instances, and delegating the elements of the
     * collection to the given destination consumer in the iteration order of the collection. For example, two
     * groups are expanded this way: If the input is {@code [[[0, 1, 2], [3, 4]], [[5, 6], [7, 8, 9]]]},
     * then the elements sent to the given destination consumer are
     * {@code [[0, 1, 2, 3, 4], [5, 6, 7, 8, 9]]} (in this order). Notice that the returned consumer does not
     * merge (nor split) sequences.
     * <P>
     * The returned consumer allows processing the sequences in parallel.
     * <P>
     * The returned consumer is reusability is the same as the consumer given in the argument.
     *
     * @param <T> the type of the elements to be processed by the destination consumer
     * @param <C> the type of the {@code Iterable} (e.g., {@code List})
     * @param dest the destination consumer processing the elements of each collection of the
     *   input streams. This argument cannot be {@code null}.
     * @return a consumer processing streams of collections, and delegating the elements of the collection
     *   to the given destination consumer. This method never returns {@code null}.
     *
     * @see FluentSeqGroupConsumer#apply(java.util.function.Function) FluentSeqGroupConsumer.apply
     * @see FluentSeqGroupProducer#batch(int) FluentSeqGroupProducer.batch
     */
    public static <T, C extends Iterable<? extends T>> SeqGroupConsumer<C> flatteningConsumer(
            SeqGroupConsumer<? super T> dest) {

        return ElementConsumers.flatteningSeqGroupConsumer(dest);
    }

    /**
     * Processes all the provided input sequences and of their elements. This method must process the
     * provided sequences exactly once (however, each sequence must be processed, and there can be more).
     * <P>
     * Exceptions thrown by the provided {@code seqGroupProducer} must be propagated to the caller, and must
     * not be wrapped (with the exception of checked exceptions not implementing {@code Exception}).
     *
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a well-behaved implementation
     *   should make a best effort check, if it does some lengthy operation. If cancellation was
     *   detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param seqGroupProducer the producer whose elements are to be consumed. This argument cannot be {@code null}
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void consumeAll(
            CancellationToken cancelToken,
            SeqGroupProducer<? extends T> seqGroupProducer) throws Exception;

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
    public default FluentSeqGroupConsumer<T> toFluent() {
        return new FluentSeqGroupConsumer<>(this);
    }
}
