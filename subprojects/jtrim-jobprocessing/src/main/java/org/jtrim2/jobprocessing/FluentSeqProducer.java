package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.function.Function;

public final class FluentSeqProducer<T> {
    private final SeqProducer<T> wrapped;

    FluentSeqProducer(SeqProducer<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public <T1> FluentSeqProducer<T1> apply(Function<? super SeqProducer<T>, ? extends SeqProducer<T1>> configurer) {
        return Objects.requireNonNull(configurer, "configurer")
                .apply(wrapped)
                .toFluent();
    }

    public FluentSeqProducer<T> concat(SeqProducer<? extends T> nextProducer) {
        return ElementProducers.concat(wrapped, nextProducer).toFluent();
    }

    public FluentSeqGroupProducer<T> toSingleGroupProducer() {
        return ElementProducers.toSingleGroupProducer(wrapped).toFluent();
    }

    public SeqProducer<T> unwrap() {
        return wrapped;
    }
}
