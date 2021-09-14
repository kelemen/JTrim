package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;

public interface SeqConsumer<T> {
    public static <T> SeqConsumer<T> draining() {
        return ElementConsumers.drainingSeqConsumer();
    }

    public void consumeAll(
            CancellationToken cancelToken,
            SeqProducer<? extends T> seqProducer) throws Exception;

    public default FluentSeqConsumer<T> toFluent() {
        return new FluentSeqConsumer<>(this);
    }
}
