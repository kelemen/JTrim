package org.jtrim2.collections;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class IterableStreamable<T> implements Streamable<T> {
    private final Collection<T> src;

    public IterableStreamable(Collection<T> src) {
        this.src = Objects.requireNonNull(src, "src");
    }

    @Override
    public Stream<T> stream() {
        return src.stream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        src.forEach(action);
    }
}
