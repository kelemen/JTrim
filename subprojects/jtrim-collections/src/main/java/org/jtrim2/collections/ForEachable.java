package org.jtrim2.collections;

import java.util.function.Consumer;

/**
 * Defines the most basic interface for a collection of elements. The elements can be looped
 * over by the {@link #forEach(Consumer) forEach} method, providing a very flexible implementation
 * possibility.
 *
 * @param <T> the type of the elements of this collection
 */
public interface ForEachable<T> {
    /**
     * Iterates over the backing data source, and calls the given {@code action} for each element
     * (exactly once per element). The order of the iteration (and if it is done concurrently or not)
     * is implementation dependent.
     * <P>
     * Exceptions thrown by the provided action are propagated to the caller of {@code forEach}.
     *
     * @param action the action to be called for each element of the backing data source.
     *   This argument cannot be {@code null}.
     */
    void forEach(Consumer<? super T> action);
}
