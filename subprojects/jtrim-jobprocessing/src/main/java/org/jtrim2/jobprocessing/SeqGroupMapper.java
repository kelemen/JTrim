package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;

public interface SeqGroupMapper<T, R> {
    public static <T> SeqGroupMapper<T, T> identity() {
        return ElementMappers.identitySeqGroupMapper();
    }

    public void mapAll(
            CancellationToken cancelToken,
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqGroupConsumer<? super R> seqGroupConsumer) throws Exception;

    public default FluentSeqGroupMapper<T, R> toFluent() {
        return new FluentSeqGroupMapper<>(this);
    }
}
