package org.jtrim2.collections;

import java.util.Objects;

/**
 * Defines static factory methods for {@link EqualityComparator} instances.
 */
public final class Equality {
    /**
     * Returns an {@code EqualityComparator} which compares objects based on
     * their {@link Object#equals(Object) equals} method. That is, the
     * comparator returns the same value for the comparison as the static
     * method {@link java.util.Objects#equals(Object, Object)}.
     *
     * @return an {@code EqualityComparator} which compares objects based on
     *   their {@link Object#equals(Object) equals} method. This method never
     *   returns {@code null}.
     */
    public static EqualityComparator<Object> naturalEquality() {
        return Objects::equals;
    }

    /**
     * Returns an {@code EqualityComparator} which compares objects based on
     * their references. That is, the comparator returns the same value as a
     * reference comparison (i.e.: "==").
     *
     * @return an {@code EqualityComparator} which compares objects based on
     *   their {@link Object#equals(Object) equals} method. This method never
     *   returns {@code null}.
     */
    public static EqualityComparator<Object> referenceEquality() {
        return (a, b) -> a == b;
    }

    private Equality() {
        throw new AssertionError();
    }
}
