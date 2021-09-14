package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.utils.ExceptionHelper;

final class ParallelSeqGroupConsumer<T> implements SeqGroupConsumer<T> {
    private final SeqGroupConsumer<? super T> seqGroupConsumer;
    private final Supplier<ExecutorRef> executorProvider;
    private final int consumerThreadCount;
    private final int extraQueueCapacity;

    public ParallelSeqGroupConsumer(
            Supplier<ExecutorRef> executorProvider,
            int consumerThreadCount,
            int extraQueueCapacity,
            SeqGroupConsumer<? super T> seqGroupConsumer) {

        this.seqGroupConsumer = Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");
        this.executorProvider = Objects.requireNonNull(executorProvider, "executorProvider");
        this.consumerThreadCount = ExceptionHelper
                .checkArgumentInRange(consumerThreadCount, 1, Integer.MAX_VALUE, "consumerThreadCount");
        this.extraQueueCapacity = ExceptionHelper
                .checkArgumentInRange(extraQueueCapacity, 0, Integer.MAX_VALUE, "extraQueueCapacity");
    }

    @Override
    public void consumeAll(CancellationToken cancelToken, SeqGroupProducer<? extends T> seqGroupProducer) throws Exception {
        seqGroupConsumer.consumeAll(cancelToken, new ParallelSeqGroupProducer<>(
                executorProvider,
                consumerThreadCount,
                extraQueueCapacity,
                seqGroupProducer
        ));
    }
}
