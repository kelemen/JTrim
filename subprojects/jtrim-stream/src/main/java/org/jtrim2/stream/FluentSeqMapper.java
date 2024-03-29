package org.jtrim2.stream;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a convenient fluent style builder for mappers mapping a single
 * sequence of elements. Instances of this class can be created through the
 * {@link SeqMapper#toFluent() toFluent()} method of {@link SeqMapper}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code SeqMapper} instance.
 * <P>
 * For simple factory methods for initial producer implementations, see the
 * factory methods in {@link SeqMapper}.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqMapper}.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements of the input stream
 * @param <R> the type of the elements this mapper is producing
 *
 * @see SeqMapper#toFluent()
 * @see FluentSeqGroupMapper
 */
public final class FluentSeqMapper<T, R> {
    private final SeqMapper<T, R> wrapped;

    FluentSeqMapper(SeqMapper<T, R> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    /**
     * Transforms the wrapped {@code SeqMapper} using the given function and returns a
     * fluent style builder for the transformation result. This method exists to allow
     * fluent style continuation using custom transformations.
     * <P>
     * For an instance of {@code FluentSeqMapper} named {@code builder}, this method call is
     * equivalent to {@code configurer.apply(builder.unwrap()).toFluent()}.
     *
     * @param <T1> the type of the elements mapped by the returned mapper
     * @param <R1> the type of the elements the returned mapper maps element to
     * @param configurer the transformation transforming the wrapped mapper. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqMapper} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     */
    public <T1, R1> FluentSeqMapper<T1, R1> apply(
            Function<? super SeqMapper<T, R>, ? extends SeqMapper<T1, R1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    /**
     * Transforms the wrapped {@code SeqMapper} using the given function and returns a
     * fluent style builder for the transformation result. This method is effectively the same as
     * {@link #apply(Function) apply}, but works on fluent builders which is more convenient in some cases.
     *
     * @param <T1> the type of the elements mapped by the returned mapper
     * @param <R1> the type of the elements the returned mapper maps element to
     * @param configurer the transformation transforming the wrapped mapper. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqMapper} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     */
    public <T1, R1> FluentSeqMapper<T1, R1> applyFluent(
            Function<? super FluentSeqMapper<T, R>, ? extends FluentSeqMapper<T1, R1>> configurer
    ) {
        return apply(src -> configurer.apply(src.toFluent()).unwrap());
    }

    /**
     * Returns a mapper further mapping the output elements of this mapper.
     *
     * @param <R2> the type of the elements the returned mapper maps element to
     * @param mapper the mapper further mapping the output elements of this mapper. This argument
     *   cannot be {@code null}.
     * @return a mapper further mapping the output elements of this mapper. This method never returns {@code null}.
     */
    public <R2> FluentSeqMapper<T, R2> map(SeqMapper<? super R, ? extends R2> mapper) {
        return ElementMappers.<T, R, R2>concatSeqMapper(wrapped, mapper).toFluent();
    }

    /**
     * Returns a mapper further mapping the output elements of this mapper.
     *
     * @param <R2> the type of the elements the returned mapper maps element to
     * @param mapper the mapper further mapping the output elements of this mapper. This argument
     *   cannot be {@code null}.
     * @return a mapper further mapping the output elements of this mapper. This method never returns {@code null}.
     */
    public <R2> FluentSeqMapper<T, R2> map(FluentSeqMapper<? super R, ? extends R2> mapper) {
        return map(mapper.unwrap());
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
    public <R2> FluentSeqMapper<T, R2> mapContextFree(ElementMapper<? super R, ? extends R2> mapper) {
        return map(ElementMappers.contextFreeSeqMapper(mapper));
    }

    /**
     * Returns a mapper mapping the elements on a background thread. That is, this method does the same thing
     * as the {@link FluentSeqProducer#toBackground(String, int)} method.
     *
     * @param executorName the name given to the executor running the mapper tasks. This name will
     *   appear in the name of the executing thread. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   downtime due to variance in producing and processing times.
     * @return a mapper mapping the elements on a background thread. This method never returns {@code null}.
     */
    public FluentSeqMapper<T, R> inBackground(
            String executorName,
            int queueSize
    ) {
        return ElementMappers.inBackgroundSeqMapper(wrapped, executorName, queueSize).toFluent();
    }

    /**
     * Returns a mapper mapping the elements on a background thread. That is, this method does the same thing
     * as the {@link FluentSeqProducer#toBackground(ThreadFactory, int)} method.
     *
     * @param threadFactory the thread factory creating the threads running the mapper tasks.
     *   This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   downtime due to variance in producing and processing times.
     * @return a mapper mapping the elements on a background thread. This method never returns {@code null}.
     */
    public FluentSeqMapper<T, R> inBackground(
            ThreadFactory threadFactory,
            int queueSize
    ) {
        return ElementMappers.inBackgroundSeqMapper(wrapped, threadFactory, queueSize).toFluent();
    }

    /**
     * Returns a mapper mapping the elements on a background thread. That is, this method does the same thing
     * as the {@link FluentSeqProducer#toBackground(TaskExecutor, int)} method.
     *
     * @param executor the executor running the mapper tasks. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   downtime due to variance in producing and processing times.
     * @return a mapper mapping the elements on a background thread. This method never returns {@code null}.
     */
    public FluentSeqMapper<T, R> inBackground(
            TaskExecutor executor,
            int queueSize
    ) {
        return ElementMappers.inBackgroundSeqMapper(wrapped, executor, queueSize).toFluent();
    }

    /**
     * Returns a mapper mapping (potentially) multiple sequences using this mapper.
     * That is, each sequence will be mapped by the same mapper.
     * <P>
     * If this mapper is not stateless, and should not be called for multiple
     * sequences, then consider using the {@link #toSingleShotGroupMapper() toSingleShotGroupMapper}
     * method for safety.
     *
     * @return a mapper mapping (potentially) multiple sequences using this mapper.
     *   This method never returns {@code null}.
     *
     * @see #toSingleShotGroupMapper()
     */
    public FluentSeqGroupMapper<T, R> toContextFreeGroupMapper() {
        return ElementMappers.contextFreeSeqGroupMapper(wrapped).toFluent();
    }

    /**
     * Returns a mapper mapping a single sequence using this mapper. That is, when mapping
     * a group of sequences, the returned mapper will fail, if it was attempted to map multiple
     * sequences.
     * <P>
     * Note that the returned mapper is still reusable, that is it can be used for multiple mapping,
     * the restriction applies within the context of a single
     * {@link SeqGroupConsumer#consumeAll(org.jtrim2.cancel.CancellationToken, SeqGroupProducer) SeqGroupConsumer.consumeAll}
     * call.
     *
     * @return a mapper mapping a single sequence using this mapper. This method never returns {@code null}.
     *
     * @see #toContextFreeGroupMapper()
     */
    public FluentSeqGroupMapper<T, R> toSingleShotGroupMapper() {
        return ElementMappers.toSingleShotSeqGroupMapper(wrapped).toFluent();
    }

    /**
     * Returns the underlying {@code SeqMapper} instance.
     *
     * @return the underlying {@code SeqMapper} instance. This method never returns {@code null}.
     */
    public SeqMapper<T, R> unwrap() {
        return wrapped;
    }
}
