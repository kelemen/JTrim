package org.jtrim2.collections;

/**
 * Defines an equality comparison between two objects. This interface might be
 * used instead of a {@code java.util.Comparator} if no ordering is needed and
 * the natural comparison isn't adequate.
 * <P>
 * For standard implementations, see the {@link Equality} class.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safely usable by
 * multiple threads concurrently; with the assumption that reading properties of
 * the compared objects are thread-safe.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of this interface are required to be
 * <I>synchronization transparent</I>.
 *
 * @param <T> the type of the objects to be compared
 *
 * @see Equality
 */
public interface EqualityComparator<T> {
    /**
     * Returns {@code true} if the specified objects are to be considered
     * equivalent, {@code false} if not.
     * <P>
     * The equality comparison must have the same properties as defined for
     * the {@link Object#equals(Object)} method: reflexive, symmetric,
     * transitive and consistent.
     * <P>
     * Special case for {@code null} references:
     * <ul>
     *  <li>
     *   If both references are {@code null}, this method must return
     *   {@code true}.
     *  </li>
     *  <li>
     *   If only one of the references are {@code null}, this method must return
     *   {@code false}.
     *  </li>
     * </ul>
     *
     * @param obj1 the first object to be compared. This argument is allowed to
     *   be {@code null}.
     * @param obj2 the second object to be compared. This argument is allowed to
     *   be {@code null}.
     * @return {@code true} if the specified objects are to be considered
     *   equivalent, {@code false} if not
     */
    public boolean equals(T obj1, T obj2);
}
