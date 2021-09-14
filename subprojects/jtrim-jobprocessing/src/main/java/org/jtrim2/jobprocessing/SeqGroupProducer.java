package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;

public interface SeqGroupProducer<T> {
    public static <T> SeqGroupProducer<T> empty() {
        return ElementProducers.emptySeqGroupProducer();
    }

    public void transferAll(CancellationToken cancelToken, SeqConsumer<? super T> seqConsumer) throws Exception;

    public default void transferAllSimple(
            CancellationToken cancelToken,
            ElementConsumer<? super T> consumer) throws Exception {

        transferAll(cancelToken, ElementConsumers.contextFreeSeqConsumer(consumer));
    }

    public default FluentSeqGroupProducer<T> toFluent() {
        return new FluentSeqGroupProducer<>(this);
    }
}
