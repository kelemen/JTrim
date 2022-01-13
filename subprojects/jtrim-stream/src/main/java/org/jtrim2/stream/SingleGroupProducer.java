package org.jtrim2.stream;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;

final class SingleGroupProducer<T> implements SeqGroupProducer<T> {
    private final SeqProducer<? extends T> src;

    public SingleGroupProducer(SeqProducer<? extends T> src) {
        this.src = Objects.requireNonNull(src, "src");
    }

    @Override
    public void transferAll(CancellationToken cancelToken, SeqConsumer<? super T> seqConsumer) throws Exception {
        seqConsumer.consumeAll(cancelToken, src);
    }

    @Override
    public void transferAllSimple(CancellationToken cancelToken, ElementConsumer<? super T> consumer) throws Exception {
        src.transferAll(cancelToken, consumer);
    }
}
