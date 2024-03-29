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
 * Defines a convenient fluent style builder for producers producing a zero or more
 * sequences of elements. Instances of this class can be created through the
 * {@link SeqGroupProducer#toFluent() toFluent()} method of {@link SeqGroupProducer}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code SeqGroupProducer} instance.
 * <P>
 * For simple factory methods for initial producer implementations, see the
 * factory methods in {@link SeqProducer} or {@link SeqGroupProducer}.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqGroupProducer}.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements to be produced
 *
 * @see SeqGroupProducer#toFluent()
 * @see FluentSeqProducer
 */
public final class FluentSeqGroupProducer<T> {
    private final SeqGroupProducer<T> wrapped;

    FluentSeqGroupProducer(SeqGroupProducer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    /**
     * Transforms the wrapped {@code SeqGroupProducer} using the given function and returns a
     * fluent style builder for the transformation result. This method exists to allow
     * fluent style continuation using custom transformations. For example:
     * <pre>{@code
     * builder
     *     .apply(SeqGroupProducer::flatteningProducer)
     *     // ...
     * }</pre>
     * <P>
     * For an instance of {@code FluentSeqProducer} named {@code builder}, this method call is
     * equivalent to {@code configurer.apply(builder.unwrap()).toFluent()}.
     *
     * @param <T1> the type of the elements produced by the returned producer
     * @param configurer the transformation transforming the wrapped producer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqGroupProducer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     *
     * @see SeqGroupProducer#flatteningProducer(SeqGroupProducer) SeqGroupProducer.flatteningProducer
     */
    public <T1> FluentSeqGroupProducer<T1> apply(
            Function<? super SeqGroupProducer<T>, ? extends SeqGroupProducer<T1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    /**
     * Transforms the wrapped {@code SeqGroupProducer} using the given function and returns a
     * fluent style builder for the transformation result. This method is effectively the same as
     * {@link #apply(Function) apply}, but works on fluent builders which is more convenient in some cases.
     *
     * @param <T1> the type of the elements produced by the returned producer
     * @param configurer the transformation transforming the wrapped producer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqGroupProducer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     */
    public <T1> FluentSeqGroupProducer<T1> applyFluent(
            Function<? super FluentSeqGroupProducer<T>, ? extends FluentSeqGroupProducer<T1>> configurer
    ) {
        return apply(src -> configurer.apply(src.toFluent()).unwrap());
    }

    /**
     * Returns a producer producing the elements produced by this producer after transformed
     * by the given mapper. Note that the mapper is not necessarily a one-to-one mapper, it
     * might even filter or add more elements (and may not even produce the same number of sequences).
     *
     * @param <R> the type of the elements produced by the returned producer
     * @param mapper the mapper mapping the elements of this producer. This argument
     *   cannot be {@code null}.
     * @return a producer producing the elements produced by this producer after transformed
     *   by the given mapper. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupProducer<R> mapGroups(SeqGroupMapper<? super T, ? extends R> mapper) {
        return ElementProducers.<T, R>mapSeqGroupProducer(wrapped, mapper).toFluent();
    }

    /**
     * Returns a producer producing the elements produced by this producer after transformed
     * by the given mapper. Note that the mapper is not necessarily a one-to-one mapper, it
     * might even filter or add more elements (and may not even produce the same number of sequences).
     *
     * @param <R> the type of the elements produced by the returned producer
     * @param mapper the mapper mapping the elements of this producer. This argument
     *   cannot be {@code null}.
     * @return a producer producing the elements produced by this producer after transformed
     *   by the given mapper. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupProducer<R> mapGroups(FluentSeqGroupMapper<? super T, ? extends R> mapper) {
        return mapGroups(mapper.unwrap());
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
    public <R> FluentSeqGroupProducer<R> map(SeqMapper<? super T, ? extends R> mapper) {
        return ElementProducers.<T, R>contextFreeMapSeqGroupProducer(wrapped, mapper).toFluent();
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
    public <R> FluentSeqGroupProducer<R> map(FluentSeqMapper<? super T, ? extends R> mapper) {
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
    public <R> FluentSeqGroupProducer<R> mapContextFree(ElementMapper<? super T, ? extends R> mapper) {
        return map(ElementMappers.contextFreeSeqMapper(mapper));
    }

    /**
     * Returns a producer producing the same elements as this producer but grouped into
     * lists of size {@code batchSize} (except for the final list, which may contain less).
     * Note that as a side-effect, the processing steps will receive elements only
     * after {@code batchSize} number of elements are received or when the stream processing
     * ends. Note that each sequence is treated separately, and elements of different sequences
     * will not be mixed in any case. Also, the returned producer will produce exactly as many
     * sequences as this producer.
     * <P>
     * For example, if this producer is producing a single sequence {@code [1, 2, 3, 4, 5, 6, 7, 8]},
     * and {@code batchSize == 0}, then the returned producer will produce a single sequence.
     * {@code [[1, 2, 3], [4, 5, 6], [7, 8]]}.
     *
     * @param batchSize the number of elements to be collected into each (except the last one)
     *   produced list. This argument must be greater than or equal to 1.
     * @return a producer grouping elements into lists of size {@code batchSize}
     *   (except the last list). This method never returns {@code null}.
     *
     * @see SeqGroupProducer#flatteningProducer(SeqGroupProducer) SeqGroupProducer.flatteningProducer
     * @see FluentSeqProducer#batch(int) FluentSeqProducer.batch
     */
    public FluentSeqGroupProducer<List<T>> batch(int batchSize) {
        return ElementProducers.batchProducer(batchSize, wrapped).toFluent();
    }

    /**
     * Returns a producer producing the same elements as this producer, but doing the
     * given processing action before providing the element for the next processing step.
     *
     * @param seqGroupPeeker the consumer doing the defined action on the produced
     *   sequence. This argument cannot be {@code null}.
     * @return a producer producing the same elements as this producer, but doing the
     *   given processing action before providing the element for the next processing step.
     *   This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> peekGroups(SeqGroupConsumer<? super T> seqGroupPeeker) {
        return ElementProducers.peekedSeqGroupProducer(wrapped, seqGroupPeeker).toFluent();
    }

    /**
     * Returns a producer producing the same elements as this producer, but doing the
     * given processing action before providing the element for the next processing step.
     *
     * @param seqGroupPeeker the consumer doing the defined action on the produced
     *   sequence. This argument cannot be {@code null}.
     * @return a producer producing the same elements as this producer, but doing the
     *   given processing action before providing the element for the next processing step.
     *   This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> peekGroups(FluentSeqGroupConsumer<? super T> seqGroupPeeker) {
        return peekGroups(seqGroupPeeker.unwrap());
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
    public FluentSeqGroupProducer<T> peek(SeqConsumer<? super T> seqPeeker) {
        return ElementProducers.peekedSeqGroupProducerContextFree(wrapped, seqPeeker).toFluent();
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
    public FluentSeqGroupProducer<T> peek(FluentSeqConsumer<? super T> seqPeeker) {
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
    public FluentSeqGroupProducer<T> peekContextFree(ElementConsumer<? super T> peeker) {
        return peekGroups(ElementConsumers.contextFreeSeqGroupConsumer(peeker));
    }

    /**
     * Returns a producer producing the first {@code maxNumberOfElements} elements of each sequence of
     * this producer. If this producer produces sequences shorter than or equal to {@code maxNumberOfElements}
     * elements, then this method will do effectively nothing.
     * <P>
     * Note: This feature relies on the fact that the exceptions are rethrown to the caller
     * as is.
     *
     * @param maxNumberOfElements the maximum number of elements the returned producer
     *   supposed to produce. This argument must be greater than or equal to zero.
     * @return a producer producing the first {@code maxNumberOfElements} elements of each sequence of
     *   this producer. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> limitEachSequence(long maxNumberOfElements) {
        return ElementProducers.limitSeqGroupProducer(wrapped, maxNumberOfElements).toFluent();
    }

    /**
     * Returns a producer resplitting the produced sequences into {@code consumerThreadCount} number of sequences
     * and processes each sequence on a new separate thread.
     * <P>
     * The implementation puts the produced elements into a blocking queue, and proceeds to produce further
     * elements. Aside from the potential parallelization, the benefit is that the producer and consumer
     * runs in parallel, but if the producer is quicker, then it won't overload the memory and
     * be blocked once the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain fewer
     * elements concurrently.
     *
     * @param executorName the name given to the executor running the processing tasks. This name will
     *   appear in the name of the executing threads. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads processing elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a producer resplitting the produced sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence on a new separate thread. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> toBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducer(executorName, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer resplitting the produced sequences into {@code consumerThreadCount} number of sequences
     * and processes each sequence on a new separate thread.
     * <P>
     * The implementation puts the produced elements into a blocking queue, and proceeds to produce further
     * elements. Aside from the potential parallelization, the benefit is that the producer and consumer
     * runs in parallel, but if the producer is quicker, then it won't overload the memory and
     * be blocked once the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain fewer
     * elements concurrently.
     *
     * @param threadFactory the thread factory creating consumer threads. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads processing elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a producer resplitting the produced sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence on a new separate thread. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> toBackground(
            ThreadFactory threadFactory,
            int consumerThreadCount,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducer(threadFactory, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer resplitting the produced sequences into {@code consumerThreadCount} number of sequences
     * and processes each sequence in a new separate task of the given executor.
     * <P>
     * The implementation puts the produced elements into a blocking queue, and proceeds to get further
     * elements. Aside from the potential parallelization, the benefit is that the producer and consumer
     * runs in parallel, but if the producer is quicker, then it won't overload the memory and
     * be blocked once the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain less
     * elements concurrently.
     * <P>
     * Note that it is normally expected that the executor can run {@code consumerThreadCount} tasks
     * in parallel (which will run for the whole duration of the whole processing uninterrupted),
     * when submitted from the context where the jobs would be running otherwise. If that is not the case,
     * then the implementation might assume that the jobs submitted to the executor will eventually start
     * for the purpose of queue settings. However, dead-lock will only arise in case not even a single
     * task submitted to the given executor can start running, and in that case dead-lock is a necessity.
     *
     * @param executor the executor running the consumer tasks. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads processing elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a producer resplitting the produced sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence in a new separate task of the given executor. This method never returns
     *   {@code null}
     */
    public FluentSeqGroupProducer<T> toBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducer(executor, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer processing each sequence on a background thread while retaining the
     * sequences of this producer. A new thread is spawned for each sequences, so the parallelization
     * property of the returned producer will remain the same as this producer. The benefit of
     * the returned producer is that the producers and consumers run in parallel, but if the producer is quicker,
     * then it won't overload the memory and be blocked once the queue is full. That is, if the producer and
     * consumer uses different resources, then you can achieve better resource utilization. If there is no or
     * only insignificant variance between producing or processing element, then the {@code queueSize} argument
     * can be set to zero to retain fewer elements concurrently.
     *
     * @param executorName the name given to the executor running the processing tasks. This name will
     *   appear in the name of the executing threads. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1} for each sequence. Note that there is a separate
     *   queue for each sequence. This argument must be greater than or equal to zero. Setting this argument
     *   to zero is often appropriate, but can be set to a higher value to reduce the down time due to
     *   variance in producing and processing times.
     * @return a producer processing each sequence on a background thread while retaining the
     *   sequences of this producer. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> toBackgroundRetainSequences(
            String executorName,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducerRetainSequences(executorName, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer processing each sequence on a background thread while retaining the
     * sequences of this producer. A new thread is spawned for each sequences, so the parallelization
     * property of the returned producer will remain the same as this producer. The benefit of
     * the returned producer is that the producers and consumers run in parallel, but if the producer is quicker,
     * then it won't overload the memory and be blocked once the queue is full. That is, if the producer and
     * consumer uses different resources, then you can achieve better resource utilization. If there is no or
     * only insignificant variance between producing or processing element, then the {@code queueSize} argument
     * can be set to zero to retain fewer elements concurrently.
     *
     * @param threadFactory the thread factory creating consumer threads. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1} for each sequence. Note that there is a separate
     *   queue for each sequence. This argument must be greater than or equal to zero. Setting this argument
     *   to zero is often appropriate, but can be set to a higher value to reduce the down time due to
     *   variance in producing and processing times.
     * @return a producer processing each sequence on a background thread while retaining the
     *   sequences of this producer. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> toBackgroundRetainSequences(
            ThreadFactory threadFactory,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducerRetainSequences(threadFactory, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a producer processing each sequence on a background task of the given executor while retaining the
     * sequences of this producer. A new task is submitted to the given executor for each sequences,
     * so the parallelization property of the returned producer will remain the same as this producer as
     * long as the given executor supports that level of parallelization. The benefit of the returned producer
     * is that the producers and consumers run in parallel, but if the producer is quicker, then it won't
     * overload the memory and be blocked once the queue is full. That is, if the producer and consumer uses
     * different resources, then you can achieve better resource utilization. If there is no or only insignificant
     * variance between producing or processing element, then the {@code queueSize} argument can be set to
     * zero to retain less elements concurrently.
     * <P>
     * The given executor must be able to run at least a single task in the context of stream processing.
     * Note however that if the given executor cannot run as many tasks in parallel as the number of
     * threads used by this producer, then the processing of sequences will be serialized. That is,
     * a task submitted to the given executor will always fully process a whole sequence of elements.
     *
     * @param executor the executor running the consumer tasks. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer tasks
     *   are processing. That is, the tasks are effectively part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1} for each sequence. Note that there is a separate
     *   queue for each sequence. This argument must be greater than or equal to zero. Setting this argument
     *   to zero is often appropriate, but can be set to a higher value to reduce the down time due to
     *   variance in producing and processing times.
     * @return a producer processing each sequence on a background thread while retaining the
     *   sequences of this producer. This method never returns {@code null}.
     */
    public FluentSeqGroupProducer<T> toBackgroundRetainSequences(
            TaskExecutor executor,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducerRetainSequences(executor, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a {@code FluentSeqProducer} producing all elements of all sequences of this
     * producer as a single sequence. The order within the same sequence is preserved, but
     * there is no ordering guarantee between elements of different sequences.
     * <P>
     * The merging is done by acquiring a lock (the same lock for each element of each sequences)
     * while processing the element produced by this producer.
     * <P>
     * This method is a bridge between the {@link SeqGroupProducer} and {@link SeqProducer}.
     *
     * @return a {@code FluentSeqProducer} producing all elements of all sequences of this
     *   producer as a single sequence. This method never returns {@code null}.
     */
    public FluentSeqProducer<T> toSynchronized() {
        return ElementProducers.toSynchronizedSeqProducer(wrapped).toFluent();
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
        SeqGroupProducer<T> wrappedCapture = wrapped;
        return action -> {
            Objects.requireNonNull(action, "action");
            try {
                wrappedCapture.transferAllSimple(Cancellation.UNCANCELABLE_TOKEN, action::accept);
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
     * @param seqGroupConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withConsumer(SeqGroupConsumer<? super T> seqGroupConsumer) {
        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");

        SeqGroupProducer<T> wrappedCapture = wrapped;
        return cancelToken -> seqGroupConsumer.consumeAll(cancelToken, wrappedCapture);
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer.
     *
     * @param seqGroupConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withConsumer(FluentSeqGroupConsumer<? super T> seqGroupConsumer) {
        return withConsumer(seqGroupConsumer.unwrap());
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer. The same consumer is applied
     * to each sequences independently.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withContextFreeSeqConsumer(SeqConsumer<? super T> seqConsumer) {
        return withConsumer(ElementConsumers.contextFreeSeqGroupConsumer(seqConsumer));
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer. The same consumer is applied
     * to each sequences independently.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withContextFreeSeqConsumer(FluentSeqConsumer<? super T> seqConsumer) {
        return withContextFreeSeqConsumer(seqConsumer.unwrap());
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer assuming no more than one sequence
     * is produced by this producer. If this producer produces more than one sequences,
     * then the processing will fail.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withSingleShotSeqConsumer(SeqConsumer<? super T> seqConsumer) {
        return withConsumer(ElementConsumers.toSingleShotSeqGroupConsumer(seqConsumer));
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer assuming no more than one sequence
     * is produced by this producer. If this producer produces more than one sequences,
     * then the processing will fail.
     *
     * @param seqConsumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withSingleShotSeqConsumer(FluentSeqConsumer<? super T> seqConsumer) {
        return withSingleShotSeqConsumer(seqConsumer.unwrap());
    }

    /**
     * Returns an action which, when executed, produces the elements of this producer,
     * and then consumes them with the given consumer. The same consumer is applied
     * to each element of each sequence independently.
     *
     * @param consumer the consumer processing the elements produced by this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, produces the elements of this producer,
     *   and then consumes them with the given consumer. This method never
     *   returns {@code null}.
     */
    public CancelableTask withContextFreeConsumer(ElementConsumer<? super T> consumer) {
        return withConsumer(ElementConsumers.contextFreeSeqGroupConsumer(consumer));
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
     *     .toSingleGroupProducer()
     *     .withCollector(Collectors.toList())
     *     .execute(Cancellation.UNCANCELABLE_TOKEN);
     * }</pre>
     *
     * @param <R> the type of result of the collection or reduction operation
     * @param collector the collector collecting or reducing the elements of this producer.
     *   This argument cannot be {@code null}.
     * @return an action which, when executed, collects and returns the elements of this producer
     *   using the given {@code Collector}. This method never returns {@code null}.
     */
    public <R> CancelableFunction<R> withCollector(Collector<? super T, ?, ? extends R> collector) {
        Objects.requireNonNull(collector, "collector");

        SeqGroupProducer<T> wrappedCapture = wrapped;
        return cancelToken -> ElementProducers.collect(cancelToken, wrappedCapture, collector);
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
     *     .toSingleGroupProducer()
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
     */
    public <R> R collect(
            CancellationToken cancelToken,
            Collector<? super T, ?, ? extends R> collector) throws Exception {

        return ElementProducers.collect(cancelToken, wrapped, collector);
    }

    /**
     * Returns the underlying {@code SeqGroupProducer} instance.
     *
     * @return the underlying {@code SeqGroupProducer} instance. This method never returns {@code null}.
     */
    public SeqGroupProducer<T> unwrap() {
        return wrapped;
    }
}
