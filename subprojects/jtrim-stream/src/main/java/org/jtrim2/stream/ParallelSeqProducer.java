package org.jtrim2.stream;

import java.util.function.Supplier;
import org.jtrim2.cancel.CancellationToken;

final class ParallelSeqProducer<T> implements SeqProducer<T> {
    private final ParallelSeqGroupProducer<T> impl;

    public ParallelSeqProducer(
            Supplier<ExecutorRef> executorProvider,
            int extraQueueCapacity,
            SeqProducer<? extends T> src) {

        this.impl = new ParallelSeqGroupProducer<>(
                executorProvider,
                1,
                extraQueueCapacity,
                src.toFluent().toSingleGroupProducer().unwrap()
        );
    }

    @Override
    public void transferAll(CancellationToken cancelToken, ElementConsumer<? super T> consumer) throws Exception {
        impl.transferAllSimple(cancelToken, consumer);
    }
}
