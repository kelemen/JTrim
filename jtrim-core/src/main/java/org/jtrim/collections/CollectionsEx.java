package org.jtrim.collections;

import java.util.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains helper methods for arrays not present in {@link java.util.Collections}.
 * <P>
 * This class cannot be instantiated or inherited.
 *
 * @author Kelemen Attila
 */
public final class CollectionsEx {
    private CollectionsEx() {
        throw new AssertionError();
    }

    /**
     * Creates a new {@link java.util.HashMap HashMap<K, V>} specifying the
     * expected number of mappings. The returned map will never do a rehash
     * if the number of mappings remain bellow the specified size. So if a
     * reasonable upper bound can be specified for the number of mappings
     * the expensive rehash operation can be avoided.
     *
     * @param <K> the type of the key of the returned map
     * @param <V> the type of the value of the returned map
     * @param expectedSize the expected number mappings. This argument must not
     *   be a negative value.
     * @return a hash map with a {@code loadFactor == 0.75} and a minimal
     *   capacity which is enough to store the specified number of elements
     *   without a rehash. This method always returns a new unique object.
     *
     * @throws IllegalArgumentException thrown if the expectedSize is negative
     */
    public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
        return new HashMap<>(expectedSize, 0.75f);
    }

    /**
     * Creates a new {@link java.util.HashMap HashMap<K, V>} specifying the
     * expected number of mappings and the load factor. The returned map will
     * never do a rehash if the number of mappings remain bellow the specified
     * size. So if a reasonable upper bound can be specified for the number of
     * mappings the expensive rehash operation can be avoided.
     *
     * @param <K> the type of the key of the returned map
     * @param <V> the type of the value of the returned map
     * @param expectedSize the expected number mappings. This argument must not
     *   be a negative value.
     * @param loadFactor the load factor of the returned hash map. This argument
     *   must be a positive value
     * @return a hash map with the specified load factor and a minimal capacity
     *   which is enough to store the specified number of elements without a
     *   rehash. This method always returns a new unique object.
     *
     * @throws IllegalArgumentException thrown if the expectedSize is negative
     *   or the loadFactor is nonpositive
     */
    public static <K, V> HashMap<K, V> newHashMap(int expectedSize, float loadFactor) {
        ExceptionHelper.checkArgumentInRange(expectedSize, 0, Integer.MAX_VALUE, "expectedSize");

        int capacity = (int)((double)expectedSize / (double)loadFactor) + 1;
        return new HashMap<>(capacity >= 1 ? capacity : 1, loadFactor);
    }

    /**
     * Creates a new {@link java.util.HashSet HashSet<K>} specifying the
     * expected number of elements. The returned set will never do a rehash
     * if the number of elements remain bellow the specified size. So if a
     * reasonable upper bound can be specified for the number of elements
     * the expensive rehash operation can be avoided.
     *
     * @param <E> the type of the values of the returned set
     * @param expectedSize the expected number elements. This argument must not
     *   be a negative value.
     * @return a hash set with a {@code loadFactor == 0.75} and a minimal
     *   capacity which is enough to store the specified number of elements
     *   without a rehash. This method always returns a new unique object.
     *
     * @throws IllegalArgumentException thrown if the expectedSize is negative
     */
    public static <E> HashSet<E> newHashSet(int expectedSize) {
        return newHashSet(expectedSize, 0.75f);
    }

    /**
     * Creates a new {@link java.util.HashSet HashSet<K, V>} specifying the
     * expected number of elements and the load factor. The returned set will
     * never do a rehash if the number of elements remain bellow the specified
     * size. So if a reasonable upper bound can be specified for the number of
     * elements the expensive rehash operation can be avoided.
     *
     * @param <E> the type of the values of the returned set
     * @param expectedSize the expected number elements. This argument must not
     *   be a negative value.
     * @param loadFactor the load factor of the returned hash set. This argument
     *   must be a positive value
     * @return a hash set with the specified load factor and a minimal capacity
     *   which is enough to store the specified number of elements without a
     *   rehash. This method always returns a new unique object.
     *
     * @throws IllegalArgumentException thrown if the expectedSize is negative
     *   or the loadFactor is nonpositive
     */
    public static <E> HashSet<E> newHashSet(int expectedSize, float loadFactor) {
        ExceptionHelper.checkArgumentInRange(expectedSize, 0, Integer.MAX_VALUE, "expectedSize");

        int capacity = (int)((double)expectedSize / (double)loadFactor) + 1;
        return new HashSet<>(capacity >= 1 ? capacity : 1, loadFactor);
    }

    /**
     * Returns a new set which is based on the reference equality operator (==)
     * instead of its {@code equals} method. This method is equivalent to
     * {@code Collections.newSetFromMap(new IdentityHashMap<E, Boolean>(expectedSize))}.
     *
     * <B>Note that this method does not return a general purpose {@code set}
     * implementation</B> because it does not rely on the {@code equals} method
     * of its elements.
     *
     * @param <E> the type of the elements of the set
     * @param expectedSize the expected size the returned set. The returned
     *   set will not execute a time consuming rehash operation if its size will
     *   not grow over this limit.
     *
     * @return the newly returned
     */
    public static <E> Set<E> newIdentityHashSet(int expectedSize) {
        return Collections.newSetFromMap(new IdentityHashMap<E, Boolean>(expectedSize));
    }

    /**
     * Returns an unmodifiable copy of the specified collection.
     * The returned list is always a random access list and will contain
     * the elements in the order the iterator of the specified collection
     * returned them.
     * <P>
     * The result of this method is undefined if the specified collection
     * is modified while calling this method.
     * <P>
     * Note that this method does not necessarily returns a unique object
     * each time, only ensures that the returned list will be independent
     * of the specified collection (except that it will contain the same
     * objects) so subsequent modifications to the specified collection will
     * not be reflected in the returned list.
     *
     * @param <E> the type of the elements of the specified collection
     * @param c the collection which is to be copied. This argument cannot be
     *   {@code null}.
     * @return the readonly copy of the specified collection. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified collection is
     *   {@code null}
     */
    public static <E> List<E> readOnlyCopy(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(new ArrayList<>(c));
        }
    }

    /**
     * Returns a readonly view of two concatenated lists.
     * Changes made to the specified lists will be reflected immediately
     * in the returned list.
     * <P>
     * The returned list will contain the elements in the order they are
     * contained in the specified lists; the elements of the first list coming
     * first.
     *
     * @param <E> the type of the elements in the list
     * @param list1 the first part of the concatenated list. This argument
     *   cannot be {@code null}.
     * @param list2 the second part of the concatenated list. This argument
     *   cannot be {@code null}.
     * @return the concatenated view of the specified lists. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <E> List<E> viewConcatList(
            List<? extends E> list1,
            List<? extends E> list2) {

        if (list1 instanceof RandomAccess
                && list2 instanceof RandomAccess
                && !(list1 instanceof RandomAccessConcatListView<?>)
                && !(list2 instanceof RandomAccessConcatListView<?>)) {
            return new RandomAccessConcatListView<>(list1, list2);
        }
        else {
            return new ConcatListView<>(list1, list2);
        }
    }

    /**
     * Returns a comparator which uses the natural ordering of elements.
     * Note that the returned comparator can only be used on elements
     * implementing the {@link java.util.Comparator} interface.
     * <P>
     * Note that the return comparator is not entirely type safe and must not
     * be abused otherwise unexpected {@link ClassCastException} can be thrown
     * while trying to use it.
     *
     * @param <T> the type of the elements which the returned comparator
     *   compares
     * @return a comparator which uses the natural ordering of elements.
     *   This method never returns {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalOrder() {
        return (Comparator<T>)NaturalComparator.INSTANCE;
    }

    /**
     * Returns an element reference of a list which is not actually attached
     * to any list. The returned element reference will be a detached reference
     * containing the specified element.
     *
     * @param <E> the type of element
     * @param element the element contained in the returned list element
     *   reference. This argument will be returned by
     *   {@link RefList.ElementRef#getElement()}.
     * @return a detached list element reference of the specified element.
     *   This method never returns {@code null}.
     */
    public static <E> RefList.ElementRef<E> getDetachedListRef(E element) {
        return new DetachedListRef<>(element);
    }
}
