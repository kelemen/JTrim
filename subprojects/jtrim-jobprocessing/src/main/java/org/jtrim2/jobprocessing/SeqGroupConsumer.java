package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;

public interface SeqGroupConsumer<T> {
    public static <T> SeqGroupConsumer<T> draining() {
        return ElementConsumers.drainingSeqGroupConsumer();
    }

    public void consumeAll(CancellationToken cancelToken, SeqGroupProducer<? extends T> seqGroupProducer) throws Exception;

    public default FluentSeqGroupConsumer<T> toFluent() {
        return new FluentSeqGroupConsumer<>(this);
    }
}
