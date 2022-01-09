package org.jtrim2.stream;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableTask;

/**
 * Defines a convenient fluent style builder for producers producing a single
 * sequence of elements. Instances of this class can be created through the
 * {SeqProducer#toFluent() toFluent()} method of {@link SeqProducer}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code SeqProducer} instance.
 * <P>
 * For simple factory methods for initial producer implementations, see the
 * factory methods in {@link SeqProducer}.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqProducer}.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements to be produced
 *
 * @see SeqProducer#toFluent()
 * @see FluentSeqGroupProducer
 */
public final class FluentSeqProducer<T> {
    private final SeqProducer<T> wrapped;

    FluentSeqProducer(SeqProducer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    /**
     * Transforms the wrapped {@code SeqProducer} using the given function and returns a
     * fluent style builder for the transformation result. This method exists to allow
     * fluent style continuation using custom transformations. For example:
     * <pre>{@code
     * builder
     *     .apply(SeqProducer::flatteningProducer)
     *     // ...
     * }</pre>
     * <P>
     * For an instance of {@code FluentSeqProducer} named {@code builder}, this method call is
     * equivalent to {@code configurer.apply(builder.unwrap()).toFluent()}.
     *
     * @param <T1> the type of the elements produced by the returned producer
     * @param configurer the transformation transforming the wrapped producer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqProducer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     *
     * @see SeqProducer#flatteningProducer(SeqProducer) SeqProducer.flatteningProducer
     */
    public <T1> FluentSeqProducer<T1> apply(Function<? super SeqProducer<T>, ? extends SeqProducer<T1>> configurer) {
        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    /**
     * Returns a producer producing the elements this producer, then producing the elements
     * of the producer given in the arguments in this order. For example, if this producer
     * produces {@code [1, 2, 3, 4]}, and the producer in the arguments producers {@code [5, 6]},
     * then the returned producer will produce {@code [1, 2, 3, 4, 5, 6]}.
     *
     * @param nextProducer the producer producing the second part of the sequence. This argument
     *   cannot be {@code null}.
     * @return a producer producing the elements of this producer and the given producer in this order.
     *   This method never returns {@code null}.
     */
    public FluentSeqProducer<T> concat(SeqProducer<? extends T> nextProducer) {
        return ElementProducers.concat(wrapped, nextProducer).toFluent();
    }

    /**
     * Returns a producer producing the elements produced by this producer after transformed
     * by the given mapper. Note that the mapper is not necessarily a one-to-one mapper, it
     * might even filter or add more elements.
     *
     * @param <R> the type of the elements produced by the returned producer
     * @param mapper the mapper mapping the elements of this producer. This argument
     *   cannot be {@code null}.
     * @return a producer producing the elements produced by this producer after transformed
     *   by the given mapper. This method never returns {@code null}.
     */
    public <R> FluentSeqProducer<R> map(SeqMapper<? super T, ? extends R> mapper) {
        return ElementProducers.mapSeqProducer(wrapped, mapper).toFluent();
    }

    /**
     * Returns a producer producing the elements produced by this producer after transformed
     * by the given mapper. Note that the mapper is not necessarily a one-to-one mapper, it
     * might even filter or add more elements.
     *
     * @param <R> the type of the elements produced by the returned producer
     * @param mapper the mapper mapping the elements of this producer. This argument
     *   cannot be {@code null}.
     * @return a producer producing the elements produced by this producer after transformed
     *   by the given mapper. This method never returns {@code null}.
     */
    public <R> FluentSeqProducer<R> mapContextFree(ElementMapper<? super T, ? extends R> mapper) {
        return ElementProducers.mapSeqProducerContextFree(wrapped, mapper).toFluent();
    }

    /**
     * Returns a producer producing the same elements as this producer but grouped into
     * lists of size {@code batchSize} (except for the final list, which may contain less).
     * Note that as a side-effect, the processing steps will receive elements only
     * after {@code batchSize} number of elements are received or when the stream processing
     * ends.
     * <P>
     * For example, if this producer is producing {@code [1, 2, 3, 4, 5, 6, 7, 8]}, and
     * {@code batchSize == 0}, then the returned producer will produce
     * {@code [[1, 2, 3], [4, 5, 6], [7, 8]]}.
     *
     * @param batchSize the number of elements to be collected into each (except the last one)
     *   produced list. This argument must be greater than or equal to 1.
     * @return a producer grouping elements into lists of size {@code batchSize}
     *   (except the last list). This method never returns {@code null}.
     *
     * @see SeqProducer#flatteningProducer(SeqProducer) SeqProducer.flatteningProducer
     * @see FluentSeqGroupProducer#batch(int) FluentSeqGroupProducer.batch
     */
    public FluentSeqProducer<List<T>> batch(int batchSize) {
        return ElementProducers.batchSeqProducer(batchSize, wrapped).toFluent();
    }

    /**
     * Returns a producer producing the same elements as this producer, but doing the
     * given processing action before providing the element for the next processing step.
     *
     * @param seqPeeker the consumer doing the defined action on the produced
     *   sequence. This argument cannot be {@code null}.
     * @return a producer producing the same elements as this producer, but doing the
     *   given processing action before providing the element for the next processing step.
     *   This method never returns {@code null}.
     */
    public FluentSeqProducer<T> peek(SeqConsumer<? super T> seqPeeker) {
        return ElementProducers.peekedSeqProducer(wrapped, seqPeeker).toFluent();
    }

    /**
     * Returns a producer producing the same elements as this producer, but doing the
     * given processing action before providing the element for the next processing step.
     *
     * @param peeker the consumer doing the defined action on the produced
     *   sequence. This argument cannot be {@code null}.
     * @return a producer producing the same elements as this producer, but doing the
     *   given processing action before providing the element for the next processing step.
     *   This method never returns {@code null}.
     */
    public FluentSeqProducer<T> peekContextFree(ElementConsumer<? super T> peeker) {
        return ElementProducers.peekedSeqProducerContextFree(wrapped, peeker).toFluent();
    }

    /**
     * Returns a producer producing the first {@code maxNumberOfElements} elements of this producer.
     * If this producer produces less than or equal to {@code maxNumberOfElements} elements, then
     * this method will do effectively nothing.
     * <P>
     * Note: This feature relies on the fact that the exceptions are rethrown to the caller
     * as is.
     *
     * @param maxNumberOfElements the maximum number of elements the returned producer
     *   supposed to produce. This argument must be greater than or equal to zero.
     * @return a producer producing the first {@code maxNumberOfElements} elements of this producer.
     *   This method never returns {@code null}.
     */
    public FluentSeqProducer<T> limit(long maxNumberOfElements) {
        return ElementProducers.limitSeqProducer(wrapped, maxNumberOfElements).toFluent();
    }

    /**
     * Returns an action which is when executed produces the elements of this producer,
     * and then consumes them with the given consumer.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which is when executed produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withConsumer(SeqConsumer<? super T> seqConsumer) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        SeqProducer<T> wrappedCapture = wrapped;
        return cancelToken -> seqConsumer.consumeAll(cancelToken, wrappedCapture);
    }

    /**
     * Returns an action which is when executed produces the elements of this producer,
     * and then consumes them with the given consumer. The same consumer is applied
     * to each elements independently.
     *
     * @param consumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which is when executed produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withContextFreeConsumer(ElementConsumer<? super T> consumer) {
        return withConsumer(ElementConsumers.contextFreeSeqConsumer(consumer));
    }

    /**
     * Processes the elements of the sequence produced by this producer the same way as the
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector) Stream.collect} does.
     * <P>
     * For example, the following code produces the list {@code [2, 4, 6, 8]}:
     * <pre>{@code
     * List<Integer> result = SeqProducer.copiedArrayProducer(1, 2, 3, 4)
     *     .toFluent()
     *     .mapContextFree(ElementMapper.oneToOneMapper(e -> 2 * e))
     *     .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());
     * }</pre>
     *
     * @param <R> the type of result of the collection or reduction operation
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a best effort check will be made.
     *   If cancellation was detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param collector the collector collecting or reducing the elements of this producer.
     *   This argument cannot be {@code null}.
     * @return the result of the collection or reduction. Normally a collector should not return
     *   {@code null}, so in that case this method will not return {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure
     */
    public <R> R collect(
            CancellationToken cancelToken,
            Collector<? super T, ?, ? extends R> collector) throws Exception {

        return ElementProducers.collectSerial(cancelToken, wrapped, collector);
    }


    /**
     * Returns a {@code FluentSeqGroupProducer} producing the same sequence of elements
     * as this producer as a single sequence. This method is a bridge between the
     * {@link SeqGroupProducer} and {@link SeqProducer}.
     *
     * @return a {@code FluentSeqGroupProducer} producing the same sequence of elements
     *   as this producer as a single sequence. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> toSingleGroupProducer() {
        return ElementProducers.toSingleGroupProducer(wrapped).toFluent();
    }

    /**
     * Returns the underlying {@code SeqProducer} instance.
     *
     * @return the underlying {@code SeqProducer} instance. This method never returns {@code null}.
     */
    public SeqProducer<T> unwrap() {
        return wrapped;
    }
}
