package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;

public interface SeqProducer<T> {
    public static <T> SeqProducer<T> empty() {
        return ElementProducers.emptySeqProducer();
    }

    public void transferAll(CancellationToken cancelToken, ElementConsumer<? super T> consumer) throws Exception;

    public default FluentSeqProducer<T> toFluent() {
        return new FluentSeqProducer<>(this);
    }
}
