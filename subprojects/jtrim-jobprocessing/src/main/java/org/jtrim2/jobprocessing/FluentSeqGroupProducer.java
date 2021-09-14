package org.jtrim2.jobprocessing;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.TaskExecutor;

public final class FluentSeqGroupProducer<T> {
    private final SeqGroupProducer<T> wrapped;

    FluentSeqGroupProducer(SeqGroupProducer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public static <T> SeqGroupProducer<T> flatteningProducer(
            SeqGroupProducer<? extends Iterable<? extends T>> src) {

        return ElementProducers.flatteningSeqGroupProducer(src);
    }

    public <T1> FluentSeqGroupProducer<T1> apply(Function<? super SeqGroupProducer<T>, ? extends SeqGroupProducer<T1>> configurer) {
        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    public <R> FluentSeqGroupProducer<R> map(SeqMapper<? super T, ? extends R> mapper) {
        return ElementProducers.contextFreeMapSeqGroupProducer(wrapped, mapper).toFluent();
    }

    public <R> FluentSeqGroupProducer<R> mapGroups(SeqGroupMapper<? super T, ? extends R> mapper) {
        return ElementProducers.mapSeqGroupProducer(wrapped, mapper).toFluent();
    }

    public <R> FluentSeqGroupProducer<R> mapContextFree(ElementMapper<? super T, ? extends R> mapper) {
        return mapGroups(ElementMappers.contextFreeSeqGroupMapper(mapper));
    }

    public FluentSeqGroupProducer<List<T>> batch(int batchSize) {
        return ElementProducers.batchProducer(batchSize, wrapped).toFluent();
    }

    public FluentSeqGroupProducer<T> peek(SeqGroupConsumer<? super T> seqGroupPeeker) {
        return ElementProducers.peekedSeqGroupProducer(wrapped, seqGroupPeeker).toFluent();
    }

    public FluentSeqGroupProducer<T> peekConextFree(ElementConsumer<? super T> peeker) {
        return peek(ElementConsumers.contextFreeSeqGroupConsumer(peeker));
    }

    public FluentSeqGroupProducer<T> toBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducer(executorName, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    public FluentSeqGroupProducer<T> toBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        return ElementProducers
                .backgroundSeqGroupProducer(executor, consumerThreadCount, queueSize, wrapped)
                .toFluent();
    }

    public CancelableTask withConsumer(SeqGroupConsumer<? super T> seqGroupConsumer) {
        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");

        SeqGroupProducer<T> wrappedCapture = wrapped;
        return cancelToken -> seqGroupConsumer.consumeAll(cancelToken, wrappedCapture);
    }

    public CancelableTask withContextFreeConsumer(SeqConsumer<? super T> seqConsumer) {
        return withConsumer(ElementConsumers.contextFreeSeqGroupConsumer(seqConsumer));
    }

    public CancelableTask withSingleShotConsumer(SeqConsumer<? super T> seqConsumer) {
        return withConsumer(ElementConsumers.toSingleShotSeqGroupConsumer(seqConsumer));
    }

    public <R> R collect(
            CancellationToken cancelToken,
            Collector<? super T, ?, ? extends R> collector) throws Exception {

        return ElementProducers.collect(cancelToken, wrapped, collector);
    }

    public SeqGroupProducer<T> unwrap() {
        return wrapped;
    }
}
