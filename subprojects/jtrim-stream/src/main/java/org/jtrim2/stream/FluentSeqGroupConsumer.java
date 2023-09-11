package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a convenient fluent style builder for consumers processing a group of
 * sequences of elements. Instances of this class can be created through the
 * {@link SeqGroupConsumer#toFluent() toFluent()} method of {@link SeqGroupConsumer}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code FluentSeqGroupConsumer} instance.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqGroupConsumer}.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements to be processed
 *
 * @see SeqConsumer#toFluent()
 * @see FluentSeqConsumer
 */
public final class FluentSeqGroupConsumer<T> {
    private final SeqGroupConsumer<T> wrapped;

    FluentSeqGroupConsumer(SeqGroupConsumer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    /**
     * Transforms the wrapped {@code SeqGroupConsumer} using the given function and returns a
     * fluent style builder for the transformation result. This method exists to allow
     * fluent style continuation using custom transformations. For example:
     * <pre>{@code
     * builder
     *     .apply(SeqGroupConsumer::flatteningConsumer)
     *     // ...
     * }</pre>
     * <P>
     * For an instance of {@code FluentSeqGroupConsumer} named {@code builder}, this method call is
     * equivalent to {@code configurer.apply(builder.unwrap()).toFluent()}.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param configurer the transformation transforming the wrapped consumer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqGroupConsumer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     *
     * @see SeqGroupConsumer#flatteningConsumer(SeqGroupConsumer) SeqGroupConsumer.flatteningConsumer
     */
    public <T1> FluentSeqGroupConsumer<T1> apply(
            Function<? super SeqGroupConsumer<T>, ? extends SeqGroupConsumer<T1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    /**
     * Transforms the wrapped {@code SeqGroupConsumer} using the given function and returns a
     * fluent style builder for the transformation result. This method is effectively the same as
     * {@link #apply(Function) apply}, but works on fluent builders which is more convenient in some cases.
     */
    public <T1> FluentSeqGroupConsumer<T1> applyFluent(
            Function<? super FluentSeqGroupConsumer<T>, ? extends FluentSeqGroupConsumer<T1>> configurer
    ) {
        return apply(src -> configurer.apply(src.toFluent()).unwrap());
    }

    /**
     * Returns a consumer first applying this consumer for each processed element, then applying
     * the consumer given in the argument.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param seqGroupConsumer the consumer applied to the processed elements after this consumer.
     *   This argument cannot be {@code null}.
     * @return a consumer first apply this consumer for each processed element, then applying
     *   the consumer given in the argument.
     *
     * @see #thenContextFree(ElementConsumer) thenContextFree
     * @see #then(SeqConsumer) then
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> thenForGroups(SeqGroupConsumer<? super T1> seqGroupConsumer) {
        return ElementConsumers.<T1>concatSeqGroupConsumers(wrapped, seqGroupConsumer).toFluent();
    }

    /**
     * Returns a consumer first applying this consumer for each processed element, then applying
     * the consumer given in the argument.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param seqGroupConsumer the consumer applied to the processed elements after this consumer.
     *   This argument cannot be {@code null}.
     * @return a consumer first apply this consumer for each processed element, then applying
     *   the consumer given in the argument.
     *
     * @see #thenContextFree(ElementConsumer) thenContextFree
     * @see #then(SeqConsumer) then
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> thenForGroups(
            FluentSeqGroupConsumer<? super T1> seqGroupConsumer) {

        return thenForGroups(seqGroupConsumer.unwrap());
    }

    /**
     * Returns a consumer first applying this consumer for each processed element, then applying
     * the consumer given in the argument.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param seqConsumer the consumer applied to the processed elements after this consumer.
     *   This argument cannot be {@code null}.
     * @return a consumer first apply this consumer for each processed element, then applying
     *   the consumer given in the argument.
     *
     * @see #thenContextFree(ElementConsumer) thenContextFree
     * @see #thenForGroups(SeqGroupConsumer) thenForGroups
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> then(SeqConsumer<? super T1> seqConsumer) {
        return thenForGroups(ElementConsumers.contextFreeSeqGroupConsumer(seqConsumer));
    }

    /**
     * Returns a consumer first applying this consumer for each processed element, then applying
     * the consumer given in the argument.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param seqConsumer the consumer applied to the processed elements after this consumer.
     *   This argument cannot be {@code null}.
     * @return a consumer first apply this consumer for each processed element, then applying
     *   the consumer given in the argument.
     *
     * @see #thenContextFree(ElementConsumer) thenContextFree
     * @see #thenForGroups(SeqGroupConsumer) thenForGroups
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> then(FluentSeqConsumer<? super T1> seqConsumer) {
        return then(seqConsumer.unwrap());
    }

    /**
     * Returns a consumer first applying this consumer for each processed element, then applying
     * the consumer given in the argument.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param consumer the consumer applied to the processed elements after this consumer.
     *   This argument cannot be {@code null}.
     * @return a consumer first apply this consumer for each processed element, then applying
     *   the consumer given in the argument.
     *
     * @see #then(SeqConsumer) then
     * @see #thenForGroups(SeqGroupConsumer) thenForGroups
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> thenContextFree(ElementConsumer<? super T1> consumer) {
        return thenForGroups(ElementConsumers.contextFreeSeqGroupConsumer(consumer));
    }

    /**
     * Returns a consumer resplitting the input sequences into {@code consumerThreadCount} number of sequences
     * and processes each sequence on a new separate thread.
     * <P>
     * The implementation puts received elements into a blocking queue, and proceeds to get further
     * elements. Aside from the potential parallelization, the benefit is that the producer and consumer
     * runs in parallel, but if the producer is quicker, then it won't overload the memory and
     * be blocked once the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain less
     * elements concurrently.
     *
     * @param <T1> the type of the elements of the new consumer. Although the returned consumer
     *   consumes the same elements as this producer this method allows you to return a less specific
     *   consumer for convenience.
     * @param executorName the name given to the executor running the consumer tasks. This name will
     *   appear in the name of the executing threads. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads processing elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the downtime due to variance in producing and processing times.
     * @return a consumer resplitting the input sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence on a new separate thread. This method never returns {@code null}.
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> inBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.owned(executorName);
        return new ParallelSeqGroupConsumer<T1>(executorRefProvider, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a consumer resplitting the input sequences into {@code consumerThreadCount} number of sequences
     * and processes each sequence in a new separate task of the given executor.
     * <P>
     * The implementation puts received elements into a blocking queue, and proceeds to get further
     * elements. Aside from the potential parallelization, the benefit is that the producer and consumer
     * runs in parallel, but if the producer is quicker, then it won't overload the memory and
     * be blocked once the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain fewe
     * elements concurrently.
     * <P>
     * Note that it is normally expected that the executor can run {@code consumerThreadCount} tasks
     * in parallel (which will run for the whole duration of the whole processing uninterrupted),
     * when submitted from the context where the jobs would be running otherwise. If that is not the case,
     * then the implementation might assume that the jobs submitted to the executor will eventually start
     * for the purpose of queue settings. However, dead-lock will only arise in case not even a single
     * task submitted to the given executor can start running, and in that case dead-lock is a necessity.
     *
     * @param <T1> the type of the elements of the new consumer. Although the returned consumer
     *   consumes the same elements as this producer this method allows you to return a less specific
     *   consumer for convenience.
     * @param executor the executor running the consumer tasks. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads processing elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the downtime due to variance in producing and processing times.
     * @return a consumer resplitting the input sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence in a new separate task of the given executor. This method never returns
     *   {@code null}
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> inBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.external(executor);
        return new ParallelSeqGroupConsumer<T1>(executorRefProvider, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    /**
     * Returns a consumer calling this consumer on a background thread. Each sequence will be processed on
     * a separate thread the same way as done by the
     * {@link FluentSeqGroupProducer#toBackgroundRetainSequences(String, int)} method.
     *
     * @param <T1> the type of the elements of the new consumer. Although the returned consumer
     *   consumes the same elements as this producer this method allows you to return a less specific
     *   consumer for convenience.
     * @param executorName the name given to the executor in which this consumer must run on. This name will
     *   appear in the name of the executing threads. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the downtime due to variance in producing and processing times.
     * @return a consumer calling this consumer on a background thread. This method never returns {@code null}.
     *
     * @see FluentSeqGroupProducer#toBackgroundRetainSequences(String, int)
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> inBackgroundRetainSequences(
            String executorName,
            int queueSize
    ) {
        return ElementConsumers
                .<T1>backgroundRetainedSequencesSeqGroupConsumer(wrapped, executorName, queueSize)
                .toFluent();
    }

    /**
     * Returns a consumer calling this consumer on a background thread. Each sequence will be processed on
     * a separate thread the same way as done by the
     * {@link FluentSeqGroupProducer#toBackgroundRetainSequences(TaskExecutor, int)} method.
     *
     * @param <T1> the type of the elements of the new consumer. Although the returned consumer
     *   consumes the same elements as this producer this method allows you to return a less specific
     *   consumer for convenience.
     * @param executor the executor in which this consumer must run on. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the downtime due to variance in producing and processing times.
     * @return a consumer calling this consumer on a background thread. This method never returns {@code null}.
     *
     * @see FluentSeqGroupProducer#toBackgroundRetainSequences(TaskExecutor, int)
     */
    public <T1 extends T> FluentSeqGroupConsumer<T1> inBackgroundRetainSequences(
            TaskExecutor executor,
            int queueSize
    ) {
        return ElementConsumers
                .<T1>backgroundRetainedSequencesSeqGroupConsumer(wrapped, executor, queueSize)
                .toFluent();
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output before calling this consumer. This argument
     *   cannot be {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupConsumer<R> mappedGroups(SeqGroupMapper<? super R, ? extends T> mapper) {
        return ElementConsumers.<R, T>mapToSeqGroupConsumer(mapper, wrapped).toFluent();
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output before calling this consumer. This argument
     *   cannot be {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupConsumer<R> mappedGroups(FluentSeqGroupMapper<? super R, ? extends T> mapper) {
        return mappedGroups(mapper.unwrap());
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer. The same mapper is called for each sequence.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output before calling this consumer. This argument
     *   cannot be {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupConsumer<R> mapped(SeqMapper<? super R, ? extends T> mapper) {
        return mappedGroups(SeqGroupMapper.fromMapper(mapper));
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer. The same mapper is called for each sequence.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output before calling this consumer. This argument
     *   cannot be {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupConsumer<R> mapped(FluentSeqMapper<? super R, ? extends T> mapper) {
        return mapped(mapper.unwrap());
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer. The same mapper is applied to all elements.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output before calling this consumer. This argument
     *   cannot be {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqGroupConsumer<R> mappedContextFree(ElementMapper<? super R, ? extends T> mapper) {
        return mappedGroups(SeqGroupMapper.fromElementMapper(mapper));
    }

    /**
     * Returns an identity mapper doing whatever this consumer does when requested to map the elements.
     *
     * @return an identity mapper doing whatever this consumer does when requested to map the elements.
     *   This method never returns {@code null}.
     */
    public FluentSeqGroupMapper<T, T> toInspectorMapper() {
        return ElementMappers.toInspectorMapper(wrapped).toFluent();
    }

    /**
     * Returns the underlying {@code SeqGroupConsumer} instance.
     *
     * @return the underlying {@code SeqGroupConsumer} instance. This method never returns {@code null}.
     */
    public SeqGroupConsumer<T> unwrap() {
        return wrapped;
    }
}
