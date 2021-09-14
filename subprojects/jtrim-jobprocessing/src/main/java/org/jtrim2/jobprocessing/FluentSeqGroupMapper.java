package org.jtrim2.jobprocessing;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;

public final class FluentSeqGroupMapper<T, R> {
    private final SeqGroupMapper<T, R> wrapped;

    FluentSeqGroupMapper(SeqGroupMapper<T, R> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public static <T> SeqGroupMapper<Collection<T>, T> flatteningMapper() {
        return ElementMappers.flatteningSeqGroupMapper();
    }

    public <T1, R1> FluentSeqGroupMapper<T1, R1> apply(
            Function<? super SeqGroupMapper<T, R>, ? extends SeqGroupMapper<T1, R1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    public <R2> FluentSeqGroupMapper<T, R2> map(SeqGroupMapper<? super R, ? extends R2> mapper) {
        return ElementMappers.concatSeqGroupMapper(wrapped, mapper).toFluent();
    }

    public <R2> FluentSeqGroupMapper<T, R2> mapContextFree(ElementMapper<? super R, ? extends R2> mapper) {
        return map(ElementMappers.contextFreeSeqGroupMapper(mapper));
    }

    public FluentSeqGroupMapper<T, R> inBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.owned(executorName, consumerThreadCount);
        return new ParallelSeqGroupMapper<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
    }

    public FluentSeqGroupMapper<T, R> inBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.external(executor);
        return new ParallelSeqGroupMapper<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
    }

    public FluentSeqGroupConsumer<T> toDrainingConsumer() {
        return ElementConsumers.toDrainingSeqGroupConsumer(wrapped).toFluent();
    }

    public FluentSeqGroupConsumer<T> toConsumer(SeqGroupConsumer<? super R> seqGroupConsumer) {
        return ElementConsumers.mapToSeqGroupConsumer(wrapped, seqGroupConsumer).toFluent();
    }

    public SeqGroupMapper<T, R> unwrap() {
        return wrapped;
    }
}
