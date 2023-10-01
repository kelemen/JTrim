package org.jtrim2.stream;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a convenient fluent style builder for consumers processing a single
 * sequence of elements. Instances of this class can be created through the
 * {@link SeqConsumer#toFluent() toFluent()} method of {@link SeqConsumer}.
 * <P>
 * Mutator like methods always return a new instance and do not change the
 * original {@code FluentSeqConsumer} instance.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are immutable, and are therefor safe to be used by multiple
 * threads concurrently. This property does not extend to the wrapped {@code SeqConsumer}.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements to be processed
 *
 * @see SeqConsumer#toFluent()
 * @see FluentSeqGroupConsumer
 */
public final class FluentSeqConsumer<T> {
    private final SeqConsumer<T> wrapped;

    FluentSeqConsumer(SeqConsumer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    /**
     * Transforms the wrapped {@code SeqConsumer} using the given function and returns a
     * fluent style builder for the transformation result. This method exists to allow
     * fluent style continuation using custom transformations. For example:
     * <pre>{@code
     * builder
     *     .apply(SeqConsumer::flatteningConsumer)
     *     // ...
     * }</pre>
     * <P>
     * For an instance of {@code FluentSeqConsumer} named {@code builder}, this method call is
     * equivalent to {@code configurer.apply(builder.unwrap()).toFluent()}.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param configurer the transformation transforming the wrapped consumer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqConsumer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     *
     * @see SeqConsumer#flatteningConsumer(SeqConsumer) SeqConsumer.flatteningConsumer
     */
    public <T1> FluentSeqConsumer<T1> apply(
            Function<? super SeqConsumer<T>, ? extends SeqConsumer<T1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    /**
     * Transforms the wrapped {@code SeqConsumer} using the given function and returns a
     * fluent style builder for the transformation result. This method is effectively the same as
     * {@link #apply(Function) apply}, but works on fluent builders which is more convenient in some cases.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param configurer the transformation transforming the wrapped consumer. This
     *   argument cannot be {@code null}, and the transformation cannot return {@code null}.
     * @return the wrapped {@code SeqConsumer} transformed using the given function as fluent
     *   style builder. This method never returns {@code null}.
     */
    public <T1> FluentSeqConsumer<T1> applyFluent(
            Function<? super FluentSeqConsumer<T>, ? extends FluentSeqConsumer<T1>> configurer
    ) {
        return apply(src -> configurer.apply(src.toFluent()).unwrap());
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output. This argument cannot be
     *   {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqConsumer<R> mapped(SeqMapper<? super R, ? extends T> mapper) {
        return ElementConsumers.<R, T>mapToSeqConsumer(mapper, wrapped).toFluent();
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output. This argument cannot be
     *   {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqConsumer<R> mapped(FluentSeqMapper<? super R, ? extends T> mapper) {
        return mapped(mapper.unwrap());
    }

    /**
     * Returns a consumer first applying the mapper on the producer output, and then passing the
     * mapped values to this consumer.
     *
     * @param <R> the type the elements are mapped to
     * @param mapper the mapper to be applied on the producer output. This argument cannot be
     *   {@code null}.
     * @return a consumer first applying the mapper on the producer output, and then passing the
     *   mapped values to the wrapped consumer. This method never returns {@code null}.
     */
    public <R> FluentSeqConsumer<R> mappedContextFree(ElementMapper<? super R, ? extends T> mapper) {
        return mapped(SeqMapper.fromElementMapper(mapper));
    }

    /**
     * Returns a consumer which will put the elements produced by the producer to a queue,
     * and then processes a queue on a background thread. That is, this method does the same thing
     * as the {@link FluentSeqProducer#toBackground(String, int)} method.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param executorName the name given to the executor running the consumer task. This name will
     *   appear in the name of the executing thread. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   downtime due to variance in producing and processing times.
     * @return a consumer which will put the elements produced by the producer to a queue,
     *   and then processes a queue on a background thread. This method never returns {@code null}.
     *
     * @see FluentSeqProducer#toBackground(String, int)
     */
    public <T1 extends T> FluentSeqConsumer<T1> inBackground(
            String executorName,
            int queueSize
    ) {
        return ElementConsumers.<T1>backgroundSeqConsumer(wrapped, executorName, queueSize).toFluent();
    }

    /**
     * Returns a consumer which will put the elements produced by the producer to a queue,
     * and then processes a queue on a background thread. That is, this method does the same thing
     * as the {@link FluentSeqProducer#toBackground(ThreadFactory, int)} method.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param threadFactory the thread factory creating consumer threads. This argument cannot be {@code null}.
     *   This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   downtime due to variance in producing and processing times.
     * @return a consumer which will put the elements produced by the producer to a queue,
     *   and then processes a queue on a background thread. This method never returns {@code null}.
     *
     * @see FluentSeqProducer#toBackground(String, int)
     */
    public <T1 extends T> FluentSeqConsumer<T1> inBackground(
            ThreadFactory threadFactory,
            int queueSize
    ) {
        return ElementConsumers.<T1>backgroundSeqConsumer(wrapped, threadFactory, queueSize).toFluent();
    }

    /**
     * Returns a consumer which will put the elements produced by the producer to a queue,
     * and then processes a queue on a background task submitted to the specified executor. That is,
     * this method does the same thing as the {@link FluentSeqProducer#toBackground(TaskExecutor, int)}
     * method.
     *
     * @param <T1> the type of the elements processed by the returned consumer
     * @param executor the executor running the consumer task. This argument cannot be {@code null}.
     * @param queueSize the number of extra elements to store aside from what the consumer thread
     *   is processing. That is, the consumer thread effectively acts as part of the queue. So, the total
     *   outstanding elements are {@code queueSize + 1}. This argument must be greater than or equal to zero.
     *   Setting this argument to zero is often appropriate, but can be set to a higher value to reduce the
     *   downtime due to variance in producing and processing times.
     * @return a consumer which will put the elements produced by the producer to a queue,
     *   and then processes a queue on a background task submitted to the specified executor.
     *   This method never returns {@code null}.
     *
     * @see FluentSeqProducer#toBackground(TaskExecutor, int)
     */
    public <T1 extends T> FluentSeqConsumer<T1> inBackground(
            TaskExecutor executor,
            int queueSize
    ) {
        return ElementConsumers.<T1>backgroundSeqConsumer(wrapped, executor, queueSize).toFluent();
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
     */
    public <T1 extends T> FluentSeqConsumer<T1> then(SeqConsumer<? super T1> seqConsumer) {
        return ElementConsumers.<T1>concatSeqConsumers(wrapped, seqConsumer).toFluent();
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
     */
    public <T1 extends T> FluentSeqConsumer<T1> then(FluentSeqConsumer<? super T1> seqConsumer) {
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
     */
    public <T1 extends T> FluentSeqConsumer<T1> thenContextFree(ElementConsumer<? super T1> consumer) {
        return then(SeqConsumer.fromElementConsumer(consumer));
    }

    /**
     * Returns a consumer processing (potentially) multiple sequences using this consumer.
     * That is, each sequence will be processed by the same consumer.
     * <P>
     * If this consumer is not stateless, and should not be called for multiple
     * sequences, then consider using the {@link #asSingleShotGroupConsumer() asSingleShotGroupConsumer}
     * method for safety.
     *
     * @return a consumer processing (potentially) multiple sequences using this consumer.
     *   This method never returns {@code null}.
     *
     * @see #asSingleShotGroupConsumer()
     */
    public FluentSeqGroupConsumer<T> asContextFreeGroupConsumer() {
        return ElementConsumers.contextFreeSeqGroupConsumer(wrapped).toFluent();
    }

    /**
     * Returns a consumer processing a single sequence using this consumer. That is, when processing
     * a group of sequences, the returned consumer will fail, if it was attempted to process multiple
     * sequences.
     * <P>
     * Note that the returned consumer is still reusable, that is it can be used for multiple processing,
     * the restriction applies within the context of a single
     * {@link SeqGroupConsumer#consumeAll(org.jtrim2.cancel.CancellationToken, SeqGroupProducer) SeqGroupConsumer.consumeAll}
     * call.
     *
     * @return a consumer processing a single sequence using this consumer. This method never returns {@code null}.
     *
     * @see #asContextFreeGroupConsumer()
     */
    public FluentSeqGroupConsumer<T> asSingleShotGroupConsumer() {
        return ElementConsumers.toSingleShotSeqGroupConsumer(wrapped).toFluent();
    }

    /**
     * Returns the underlying {@code SeqConsumer} instance.
     *
     * @return the underlying {@code SeqConsumer} instance. This method never returns {@code null}.
     */
    public SeqConsumer<T> unwrap() {
        return wrapped;
    }
}
