package org.jtrim.collections;

/**
 * Defines static factory methods for {@link EqualityComparator} instances.
 *
 * @author Kelemen Attila
 */
public final class Comparators {
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
        return NaturalEquality.INSTANCE;
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
        return ReferenceEquality.INSTANCE;
    }

    private Comparators() {
        throw new AssertionError();
    }
}
