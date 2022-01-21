package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Function;

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
