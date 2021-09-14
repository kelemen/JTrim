package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a convenient fluent style builder for consumers processing a group of
 * sequences of elements. Instances of this class can be created through the
 * {SeqGroupConsumer#toFluent() toFluent()} method of {@link SeqGroupConsumer}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code FluentSeqGroupConsumer} instance.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqGroupConsumer}.
 *
 * <h4>Synchronization transparency</h4>
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
        return ElementConsumers.concatSeqGroupConsumers(wrapped, seqGroupConsumer).toFluent();
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
     * @param executorName the name given to the executor running the consumer tasks. This name will
     *   appear in the name of the executing threads. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads processing elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the consumer threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a consumer resplitting the input sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence on a new separate thread. This method never returns {@code null}.
     */
    public FluentSeqGroupConsumer<T> inBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.owned(executorName, consumerThreadCount);
        return new ParallelSeqGroupConsumer<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
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
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a consumer resplitting the input sequences into {@code consumerThreadCount} number of sequences
     *   and processes each sequence in a new separate task of the given executor. This method never returns
     *   {@code null}
     */
    public FluentSeqGroupConsumer<T> inBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.external(executor);
        return new ParallelSeqGroupConsumer<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
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