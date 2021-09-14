package org.jtrim2.jobprocessing;

import java.util.Objects;

public final class FluentSeqConsumer<T> {
    private final SeqConsumer<T> wrapped;

    FluentSeqConsumer(SeqConsumer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public FluentSeqConsumer<T> then(SeqConsumer<? super T> seqConsumer) {
        return ElementConsumers.concatSeqConsumers(wrapped, seqConsumer).toFluent();
    }

    public FluentSeqConsumer<T> thenContextFree(ElementConsumer<? super T> consumer) {
        return then(ElementConsumers.contextFreeSeqConsumer(consumer));
    }

    public FluentSeqGroupConsumer<T> asContextFreeGroupConsumer() {
        return ElementConsumers.contextFreeSeqGroupConsumer(wrapped).toFluent();
    }

    public <T1 extends T> FluentSeqConsumer<T1> consumeSubtype() {
        return ElementConsumers.<T1>castSeqConsumer(wrapped).toFluent();
    }

    public SeqConsumer<T> unwrap() {
        return wrapped;
    }
}
