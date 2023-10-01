package org.jtrim2.stream;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collector;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ForEachable;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a convenient fluent style builder for producers producing a single
 * sequence of elements. Instances of this class can be created through the
 * {@link SeqProducer#toFluent() toFluent()} method of {@link SeqProducer}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code SeqProducer} instance.
 * <P>
 * For simple factory methods for initial producer implementations, see the
 * factory methods in {@link SeqProducer}.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqProducer}.
 *
 * <h3>Synchronization transparency</h3>
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
     * Transforms the wrapped {@code SeqProducer} using the given function and returns a
     * fluent style builder for the transformation result. This method is effectively the same as
     * {@link #apply(Function) apply}, but works on fluent builders which is more convenient in some cases.
     *
     * @param <T1> the type of the elements produced by the returned producer
     * @param configurer the transformation transforming the wrapped producer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqProducer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     */
    public <T1> FluentSeqProducer<T1> applyFluent(
            Function<? super FluentSeqProducer<T>, ? extends FluentSeqProducer<T1>> configurer
    ) {
        return apply(src -> configurer.apply(src.toFluent()).unwrap());
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
    public FluentSeqProducer<T> concat(FluentSeqProducer<? extends T> nextProducer) {
        return concat(nextProducer.unwrap());
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
        return ElementProducers.<T, R>mapSeqProducer(wrapped, mapper).toFluent();
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
    public <R> FluentSeqProducer<R> map(FluentSeqMapper<? super T, ? extends R> mapper) {
        return map(mapper.unwrap());
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
        return ElementProducers.<T, R>mapSeqProducerContextFree(wrapped, mapper).toFluent();
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
     * @param seqPeeker the consumer doing the defined action on the produced
     *   sequence. This argument cannot be {@code null}.
     * @return a producer producing the same elements as this producer, but doing the
     *   given processing action before providing the element for the next processing step.
     *   This method never returns {@code null}.
     */
    public FluentSeqProducer<T> peek(FluentSeqConsumer<? super T> seqPeeker) {
        return peek(seqPeeker.unwrap());
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
     * Returns a producer moving the consumer to a background thread. The implementation puts the produced elements
     * into a blocking queue, and proceeds to get further elements. This allows the producer and consumer to
     * run in parallel, but if the producer is quicker, then it won't overload the memory and be blocked once
     * the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain fewer
     * elements concurrently.
     *
     * @param executorName the name given to the executor running the consumer task. This name will
     *   appear in the name of the executing thread. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread is effectively act as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   down time due to variance in producing and processing times.
     * @return a producer moving the consumer to a background thread. This method never returns {@code null}.
     */
    public FluentSeqProducer<T> toBackground(
            String executorName,
            int queueSize) {

        return ElementProducers
                .backgroundSeqProducer(executorName, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer moving the consumer to a background thread. The implementation puts the produced elements
     * into a blocking queue, and proceeds to get further elements. This allows the producer and consumer to
     * run in parallel, but if the producer is quicker, then it won't overload the memory and be blocked once
     * the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain fewer
     * elements concurrently.
     *
     * @param threadFactory the thread factory creating consumer threads. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread is effectively act as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   down time due to variance in producing and processing times.
     * @return a producer moving the consumer to a background thread. This method never returns {@code null}.
     */
    public FluentSeqProducer<T> toBackground(
            ThreadFactory threadFactory,
            int queueSize
    ) {
        return ElementProducers
                .backgroundSeqProducer(threadFactory, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer moving the consumer to a background thread. The implementation puts the produced elements
     * into a blocking queue, and proceeds to get further elements. This allows the producer and consumer to
     * run in parallel, but if the producer is quicker, then it won't overload the memory and be blocked once
     * the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain less
     * elements concurrently.
     * <P>
     * Note that it is normally expected that the executor can run at least a single task (which will run for the
     * whole duration of the whole processing uninterrupted), when submitted from the context where the jobs would
     * be running otherwise. If that is not the case, then the implementation might assume that the job submitted
     * to the executor will eventually start for the purpose of queue settings. However, dead-lock will only arise
     * in case not even a single task submitted to the given executor can start running, and in that case dead-lock
     * is a necessity.
     *
     * @param executor the executor running the consumer task. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   down time due to variance in producing and processing times.
     * @return a producer moving the consumer to a background thread. This method never returns {@code null}
     */
    public FluentSeqProducer<T> toBackground(
            TaskExecutor executor,
            int queueSize) {

        return ElementProducers
                .backgroundSeqProducer(executor, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a {@code ForEachable} providing the elements of this producer. Notice that
     * {@code ForEachable} accepts a {@link java.util.function.Consumer Consumer} which does
     * not declare any checked exceptions, while this API usually allows producers and consumers
     * to throw any instance of {@code Exception}. Therefore, the returned {@code ForEachable}
     * have to (and will) rethrow checked exceptions wrapped into a {@code RuntimeException}.
     * <P>
     * <B>Note</B>: Unlike most part of this stream API, the returned {@code ForEachable}
     * considers {@code InterruptedException} thrown by this producer. That is, when this
     * producer throws an {@code InterruptedException}, the current thread is reinterrupted,
     * and the exception is rethrown wrapped into a {@code RuntimeException}.
     *
     * @return a {@code ForEachable} providing the elements of this producer. This method
     *   never returns {@code null}.
     */
    public ForEachable<T> toForEachable() {
        SeqProducer<T> wrappedCapture = wrapped;
        return action -> {
            Objects.requireNonNull(action, "action");
            try {
                wrappedCapture.transferAll(Cancellation.UNCANCELABLE_TOKEN, action::accept);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(ex);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withConsumer(SeqConsumer<? super T> seqConsumer) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        SeqProducer<T> wrappedCapture = wrapped;
        return cancelToken -> seqConsumer.consumeAll(cancelToken, wrappedCapture);
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withConsumer(FluentSeqConsumer<? super T> seqConsumer) {
        return withConsumer(seqConsumer.unwrap());
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer. The same consumer is applied
     * to each elements independently.
     *
     * @param consumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withContextFreeConsumer(ElementConsumer<? super T> consumer) {
        return withConsumer(SeqConsumer.fromElementConsumer(consumer));
    }

    /**
     * Returns an action which, when executed, processes the elements of the sequence produced by this producer
     * the same way as the {@link java.util.stream.Stream#collect(java.util.stream.Collector) Stream.collect} does.
     * <P>
     * For example, the following code produces the list {@code [2, 4, 6, 8]}:
     * <pre>{@code
     * List<Integer> result = SeqProducer.copiedArrayProducer(1, 2, 3, 4)
     *     .toFluent()
     *     .mapContextFree(ElementMapper.oneToOneMapper(e -> 2 * e))
     *     .withCollector(Collectors.toList())
     *     .execute(Cancellation.UNCANCELABLE_TOKEN);
     * }</pre>
     *
     * @param <R> the type of result of the collection or reduction operation
     * @param collector the collector collecting or reducing the elements of this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, collects and returns the elements of this producer
     *   using the given {@code Collector}. This method never returns {@code null}.
     *
     * @see #collect(CancellationToken, Collector)
     */
    public <R> CancelableFunction<R> withCollector(Collector<? super T, ?, ? extends R> collector) {
        Objects.requireNonNull(collector, "collector");

        SeqProducer<T> wrappedCapture = wrapped;
        return cancelToken -> ElementProducers.collectSeq(cancelToken, wrappedCapture, collector);
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
     * This method is effectively the same as calling: {@code withCollector(collector).execute(cancelToken)}.
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
     *
     * @see #withCollector(Collector)
     */
    public <R> R collect(
            CancellationToken cancelToken,
            Collector<? super T, ?, ? extends R> collector) throws Exception {

        return ElementProducers.collectSeq(cancelToken, wrapped, collector);
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
