package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a convenient fluent style builder for mappers mapping zero or more
 * sequences of elements. Instances of this class can be created through the
 * {@link SeqGroupMapper#toFluent() toFluent()} method of {@link SeqGroupMapper}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code SeqGroupMapper} instance.
 * <P>
 * For simple factory methods for initial mapper implementations, see the
 * factory methods in {@link SeqGroupMapper}.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqGroupMapper}.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements of the input stream
 * @param <R> the type of the elements this mapper is producing
 *
 * @see SeqGroupMapper#toFluent()
 * @see FluentSeqMapper
 */
public final class FluentSeqGroupMapper<T, R> {
    private final SeqGroupMapper<T, R> wrapped;

    FluentSeqGroupMapper(SeqGroupMapper<T, R> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    /**
     * Transforms the wrapped {@code SeqGroupMapper} using the given function and returns a
     * fluent style builder for the transformation result. This method exists to allow
     * fluent style continuation using custom transformations.
     * <P>
     * For an instance of {@code FluentSeqGroupMapper} named {@code builder}, this method call is
     * equivalent to {@code configurer.apply(builder.unwrap()).toFluent()}.
     *
     * @param <T1> the type of the elements mapped by the returned mapper
     * @param <R1> the type of the elements the returned mapper maps element to
     * @param configurer the transformation transforming the wrapped mapper. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqGroupMapper} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     */
    public <T1, R1> FluentSeqGroupMapper<T1, R1> apply(
            Function<? super SeqGroupMapper<T, R>, ? extends SeqGroupMapper<T1, R1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    /**
     * Returns a mapper further mapping the output sequences of this mapper.
     *
     * @param <R2> the type of the elements the returned mapper maps element to
     * @param mapper the mapper further mapping the sequences of this mapper. This argument
     *   cannot be {@code null}.
     * @return a mapper further mapping the output sequences of this mapper. This method never returns {@code null}.
     */
    public <R2> FluentSeqGroupMapper<T, R2> mapGroups(SeqGroupMapper<? super R, ? extends R2> mapper) {
        return ElementMappers.<T, R, R2>concatSeqGroupMapper(wrapped, mapper).toFluent();
    }

    /**
     * Returns a mapper further mapping the output elements of this mapper. Each sequence will be further
     * mapped by the same given mapper.
     *
     * @param <R2> the type of the elements the returned mapper maps element to
     * @param mapper the mapper further mapping the output elements of this mapper. This argument
     *   cannot be {@code null}.
     * @return a mapper further mapping the output elements of this mapper. This method never returns {@code null}.
     */
    public <R2> FluentSeqGroupMapper<T, R2> map(SeqMapper<? super R, ? extends R2> mapper) {
        return mapGroups(ElementMappers.contextFreeSeqGroupMapper(mapper));
    }

    /**
     * Returns a mapper further mapping the output elements of this mapper. Each element will be further
     * mapped by the same given mapper.
     *
     * @param <R2> the type of the elements the returned mapper maps element to
     * @param mapper the mapper further mapping the output elements of this mapper. This argument
     *   cannot be {@code null}.
     * @return a mapper further mapping the output elements of this mapper. This method never returns {@code null}.
     */
    public <R2> FluentSeqGroupMapper<T, R2> mapContextFree(ElementMapper<? super R, ? extends R2> mapper) {
        return mapGroups(ElementMappers.contextFreeSeqGroupMapper(mapper));
    }

    /**
     * Returns a mapper resplitting the mapped sequences into {@code consumerThreadCount} number of sequences
     * and maps each sequence on a new separate thread.
     * <P>
     * The implementation puts the received elements into a blocking queue, and proceeds to receive further
     * elements. Aside from the potential parallelization, the benefit is that the producer and consumer
     * runs in parallel, but if the producer is quicker, then it won't overload the memory and
     * be blocked once the queue is full. That is, if the producer and consumer uses different resources,
     * then you can achieve better resource utilization. If there is no or only insignificant variance between
     * producing or processing element, then the {@code queueSize} argument can be set to zero to retain less
     * elements concurrently.
     *
     * @param executorName the name given to the executor running the mapper tasks. This name will
     *   appear in the name of the executing threads. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads mapping elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the mapper threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a mapper resplitting the mapped sequences into {@code consumerThreadCount} number of sequences
     *   and map each sequence on a new separate thread. This method never returns {@code null}.
     */
    public FluentSeqGroupMapper<T, R> inBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.owned(executorName);
        return new ParallelSeqGroupMapper<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
    }

    /**
     * Returns a mapper resplitting the mapped sequences into {@code consumerThreadCount} number of sequences
     * and maps each sequence in a new separate task of the given executor.
     * <P>
     * The implementation puts the received elements into a blocking queue, and proceeds to receive further
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
     * @param executor the executor running the mapper tasks. This argument cannot be {@code null}.
     * @param consumerThreadCount the number of threads mapping elements concurrently. This
     *   argument must be greater than or equal to zero.
     * @param queueSize the number of extra elements to store aside from what the mapper threads
     *   are processing. That is, the threads are effectively act as part of the queue. So, the total
     *   outstanding elements are {@code consumerThreadCount + queueSize}. This argument must be
     *   greater than or equal to zero. Setting this argument to zero is often appropriate, but can be
     *   set to a higher value to reduce the down time due to variance in producing and processing times.
     * @return a mapper resplitting the mapped sequences into {@code consumerThreadCount} number of sequences
     *   and maps each sequence in a new separate task of the given executor. This method never returns
     *   {@code null}
     */
    public FluentSeqGroupMapper<T, R> inBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.external(executor);
        return new ParallelSeqGroupMapper<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
    }

    /**
     * Returns a {@code FluentSeqGroupConsumer} doing nothing with the output of this mapper, but the mapping
     * action taking place in the returned consumer.
     *
     * @return a {@code FluentSeqGroupConsumer} doing nothing with the output of this mapper, but the mapping
     *   action taking place in the returned consumer. This method never returns {@code null}.
     */
    public FluentSeqGroupConsumer<T> toDrainingConsumer() {
        return ElementConsumers.toDrainingSeqGroupConsumer(wrapped).toFluent();
    }

    /**
     * Returns a {@code FluentSeqGroupConsumer} consuming the output of this mapper using the given consumer,
     * where the mapping and element consumption taking place in the returned consumer.
     *
     * @param seqGroupConsumer the consumer consuming the output of this mapper (immediately after the mapping
     *   action on the same thread). This argument cannot be {@code null}.
     * @return a {@code FluentSeqGroupConsumer} consuming the output of this mapper using the given consumer,
     *   where the mapping and element consumption taking place in the returned consumer.
     *   This method never returns {@code null}.
     */
    public FluentSeqGroupConsumer<T> toConsumer(SeqGroupConsumer<? super R> seqGroupConsumer) {
        return ElementConsumers.mapToSeqGroupConsumer(wrapped, seqGroupConsumer).toFluent();
    }

    /**
     * Returns the underlying {@code SeqGroupMapper} instance.
     *
     * @return the underlying {@code SeqGroupMapper} instance. This method never returns {@code null}.
     */
    public SeqGroupMapper<T, R> unwrap() {
        return wrapped;
    }
}
