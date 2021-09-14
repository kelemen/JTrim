package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.utils.ExceptionHelper;

final class ParallelSeqGroupMapper<T, R> implements SeqGroupMapper<T, R> {
    private final SeqGroupMapper<? super T, ? extends R> seqGroupMapper;
    private final Supplier<ExecutorRef> executorProvider;
    private final int consumerThreadCount;
    private final int extraQueueCapacity;

    public ParallelSeqGroupMapper(
            Supplier<ExecutorRef> executorProvider,
            int consumerThreadCount,
            int extraQueueCapacity,
            SeqGroupMapper<? super T, ? extends R> seqGroupMapper) {

        this.seqGroupMapper = Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");
        this.executorProvider = Objects.requireNonNull(executorProvider, "executorProvider");
        this.consumerThreadCount = ExceptionHelper
                .checkArgumentInRange(consumerThreadCount, 1, Integer.MAX_VALUE, "consumerThreadCount");
        this.extraQueueCapacity = ExceptionHelper
                .checkArgumentInRange(extraQueueCapacity, 0, Integer.MAX_VALUE, "extraQueueCapacity");
    }

    @Override
    public void mapAll(
            CancellationToken cancelToken,
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqGroupConsumer<? super R> seqGroupConsumer) throws Exception {

        SeqGroupProducer<T> parallelProducer = new ParallelSeqGroupProducer<>(
                executorProvider,
                consumerThreadCount,
                extraQueueCapacity,
                seqGroupProducer
        );

        seqGroupMapper.mapAll(cancelToken, parallelProducer, seqGroupConsumer);
    }
}
