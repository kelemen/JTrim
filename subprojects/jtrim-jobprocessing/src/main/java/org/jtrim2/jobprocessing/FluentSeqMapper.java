package org.jtrim2.jobprocessing;

import java.util.Collection;
import java.util.Objects;

public final class FluentSeqMapper<T, R> {
    private final SeqMapper<T, R> wrapped;

    FluentSeqMapper(SeqMapper<T, R> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public static <T> SeqMapper<Collection<T>, T> flatteningMapper() {
        return ElementMappers.flatteningSeqMapper();
    }

    public <R2> FluentSeqMapper<T, R2> map(SeqMapper<? super R, ? extends R2> mapper) {
        return ElementMappers.concatSeqMapper(wrapped, mapper).toFluent();
    }

    public FluentSeqGroupMapper<T, R> asContextFreeGroupMapper() {
        return ElementMappers.contextFreeSeqGroupMapper(wrapped).toFluent();
    }

    public SeqMapper<T, R> unwrap() {
        return wrapped;
    }
}
