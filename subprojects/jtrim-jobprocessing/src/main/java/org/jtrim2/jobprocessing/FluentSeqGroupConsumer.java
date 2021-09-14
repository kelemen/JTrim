package org.jtrim2.jobprocessing;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;

public final class FluentSeqGroupConsumer<T> {
    private final SeqGroupConsumer<T> wrapped;

    FluentSeqGroupConsumer(SeqGroupConsumer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public static <T> SeqGroupConsumer<Collection<T>> flatteningConsumer(SeqGroupConsumer<? super T> src) {
        return ElementConsumers.flatteningSeqGroupConsumer(src);
    }

    public <T1> FluentSeqGroupConsumer<T1> apply(
            Function<? super SeqGroupConsumer<T>, ? extends SeqGroupConsumer<T1>> configurer) {

        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    public <T1 extends T> FluentSeqGroupConsumer<T1> consumeSubtype() {
        return ElementConsumers.<T1>castSeqGroupConsumer(wrapped).toFluent();
    }

    public FluentSeqGroupConsumer<T> then(SeqGroupConsumer<? super T> seqGroupConsumer) {
        return ElementConsumers.concatSeqGroupConsumers(wrapped, seqGroupConsumer).toFluent();
    }

    public FluentSeqGroupConsumer<T> thenContextFree(ElementConsumer<? super T> consumer) {
        return then(ElementConsumers.contextFreeSeqGroupConsumer(consumer));
    }

    public FluentSeqGroupConsumer<T> inBackground(
            String executorName,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.owned(executorName, consumerThreadCount);
        return new ParallelSeqGroupConsumer<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
    }

    public FluentSeqGroupConsumer<T> inBackground(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.external(executor);
        return new ParallelSeqGroupConsumer<>(executorRefProvider, consumerThreadCount, queueSize, wrapped).toFluent();
    }

    public FluentSeqGroupMapper<T, T> toInspectorMapper() {
        return ElementMappers.toInspectorMapper(wrapped).toFluent();
    }

    public SeqGroupConsumer<T> unwrap() {
        return wrapped;
    }
}
