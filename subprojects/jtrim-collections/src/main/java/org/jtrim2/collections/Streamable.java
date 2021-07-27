package org.jtrim2.collections;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Provides a factory interface for {@link Stream} instances. That is, this interface provides a subset
 * of the functionality of {@link Iterable} with the advantage of being implementable as a lambda.
 * <P>
 * If you want to create an instance of {@code Streamable} from a {@link Collection}, then prefer to use the
 * {@link #fromCollection(Collection) Streamable.fromCollection} factory method, as it provides a (usually)
 * more efficient implementation for the {@link #forEach(Consumer) forEach} method.
 *
 * @param <T> the type of the elements of the streams returned
 */
public interface Streamable<T> extends ForEachable<T> {
    /**
     * Returns a {@code Streamable} backed by the given {@code Collection}. All the methods of
     * this interface will delegate to the method of the backing collection of the same name.
     *
     * @param <T> the type of the elements of the streams returned
     * @param src the backing collection. This argument cannot be {@code null}.
     * @return a {@code Streamable} backed by the given {@code Collection}. This method never
     *   returns {@code null}.
     */
    static <T> Streamable<T> fromCollection(Collection<T> src) {
        return new IterableStreamable<>(src);
    }

    /**
     * Returns a new {@code Stream} which can be used to iterate over the backing data source.
     *
     * @return a new {@code Stream} which can be used to iterate over the backing data source.
     *   This method may never return {@code null}.
     */
    Stream<T> stream();

    /**
     * {@inheritDoc }
     */
    @Override
    default void forEach(Consumer<? super T> action) {
        stream().forEach(action);
    }
}
