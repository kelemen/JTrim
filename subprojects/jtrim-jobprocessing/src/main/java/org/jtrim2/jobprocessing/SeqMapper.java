package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;

public interface SeqMapper<T, R> {
    public static <T> SeqMapper<T, T> identity() {
        return ElementMappers.identitySeqMapper();
    }

    public void mapAll(
            CancellationToken cancelToken,
            SeqProducer<? extends T> seqProducer,
            SeqConsumer<? super R> seqConsumer) throws Exception;

    public default FluentSeqMapper<T, R> toFluent() {
        return new FluentSeqMapper<>(this);
    }
}
