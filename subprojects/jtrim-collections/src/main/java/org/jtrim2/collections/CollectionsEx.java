package org.jtrim2.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Consumer;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Contains helper methods for arrays not present in
 * {@link java.util.Collections}.
 * <P>
 * This class cannot be instantiated or inherited.
 */
public final class CollectionsEx {
    private static final float DEFAULT_HASHMAP_LOAD_FACTOR = 0.75f;

    private CollectionsEx() {
        throw new AssertionError();
    }

    private static <T> T configure(T obj, Consumer<? super T> config) {
        config.accept(obj);
        return obj;
    }

    /**
     * Creates and populates a {@code Map}. This method is a convenience to create a non-empty
     * map without requiring explicit declaration. It is useful when passing a map as an argument
     * or when initializing a (static) field. For example:
     *
     * <pre>{@code
     * static final Map<MyKey, String> MY_MAP = CollectionsEx.newMap(MyKey.class, map -> {
     *   map.put(MyKey.KEY1, "Value1");
     *   map.put(MyKey.KEY2, "Value2");
     * });
     * }</pre>
     *
     * The type of the map to be created depends on the type of the key. If the key is an
     * <I>enum</I>, an {@link EnumMap} is created, otherwise a {@link HashMap}.
     *
     * @param <K> the type of the key of the created map
     * @param <V> the type of the value of the created map
     * @param keyType the type of the key of the created map. This argument cannot be
     *   {@code null}.
     * @param contentConfig the lambda to which the map to be returned is passed. This
     *   argument cannot be {@code null}.
     * @return the newly created map, after initialized by the given lambda. This method
     *   never returns {@code null}.
     */
    public static <K, V> Map<K, V> newMap(
            Class<K> keyType,
            Consumer<? super Map<K, V>> contentConfig) {
        if (keyType.isEnum()) {
            return newEnumMapUnsafe(keyType, contentConfig);
        } else {
            return newHashMap(contentConfig);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>, K, V> Map<K, V> newEnumMapUnsafe(
            Class<K> keyType,
            Consumer<? super Map<K, V>> contentConfig) {

        Class<T> unsafeKeyType = (Class<T>) keyType;
        Consumer<? super Map<T, V>> unsafeConfig = (Consumer<? super Map<T, V>>) contentConfig;

        return (Map<K, V>) newEnumMap(unsafeKeyType, unsafeConfig);
    }

    /**
     * Creates and populates a {@code EnumMap}. This method is a convenience to create a non-empty
     * map without requiring explicit declaration. It is useful when passing a map as an argument
     * or when initializing a (static) field. For example:
     *
     * <pre>{@code
     * static final Map<MyEnum, String> MY_MAP = CollectionsEx.newEnumMap(MyEnum.class, map -> {
     *   map.put(MyEnum.KEY1, "Value1");
     *   map.put(MyEnum.KEY2, "Value2");
     * });
     * }</pre>
     *
     * @param <K> the type of the key of the created map
     * @param <V> the type of the value of the created map
     * @param keyType the type of the key of the created map. This argument cannot be
     *   {@code null}.
     * @param contentConfig the lambda to which the map to be returned is passed. This
     *   argument cannot be {@code null}.
     * @return the newly created map, after initialized by the given lambda. This method
     *   never returns {@code null}.
     */
    public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(
            Class<K> keyType,
            Consumer<? super EnumMap<K, V>> contentConfig) {
        return configure(new EnumMap<>(keyType), contentConfig);
    }

    /**
     * Creates and populates a {@code HashMap}. This method is a convenience to create a non-empty
     * map without requiring explicit declaration. It is useful when passing a map as an argument
     * or when initializing a (static) field. For example:
     *
     * <pre>{@code
     * static final Map<String, String> MY_MAP = CollectionsEx.newHashMap(map -> {
     *   map.put("Key1", "Value1");
     *   map.put("Key2", "Value2");
     * });
     * }</pre>
     *
     * @param <K> the type of the key of the created map
     * @param <V> the type of the value of the created map
     * @param contentConfig the lambda to which the map to be returned is passed. This
     *   argument cannot be {@code null}.
     * @return the newly created map, after initialized by the given lambda. This method
     *   never returns {@code null}.
     */
    public static <K, V> HashMap<K, V> newHashMap(Consumer<? super HashMap<K, V>> contentConfig) {
        return configure(new HashMap<>(), contentConfig);
    }

    /**
     * Creates a new {@link java.util.HashMap HashMap} specifying the
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
        return newHashMap(expectedSize, DEFAULT_HASHMAP_LOAD_FACTOR);
    }

    /**
     * Creates a new {@link java.util.HashMap HashMap} specifying the
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
    public static <K, V> HashMap<K, V> newHashMap(
            int expectedSize, float loadFactor) {

        ExceptionHelper.checkArgumentInRange(expectedSize,
                0, Integer.MAX_VALUE, "expectedSize");

        int capacity = (int) ((double) expectedSize / (double) loadFactor) + 1;
        return new HashMap<>(capacity >= 1 ? capacity : 1, loadFactor);
    }

    /**
     * Creates a new {@link java.util.HashSet HashSet} specifying the
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
        return newHashSet(expectedSize, DEFAULT_HASHMAP_LOAD_FACTOR);
    }

    /**
     * Creates a new {@link java.util.HashSet HashSet} specifying the
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
    public static <E> HashSet<E> newHashSet(
            int expectedSize, float loadFactor) {

        ExceptionHelper.checkArgumentInRange(expectedSize,
                0, Integer.MAX_VALUE, "expectedSize");

        int capacity = (int) ((double) expectedSize / (double) loadFactor) + 1;
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
        return Collections.newSetFromMap(new IdentityHashMap<>(expectedSize));
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
        } else {
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
        } else {
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
        return (Comparator<T>) unsafeNaturalOrder();
    }

    private static <T extends Comparable<T>> Comparator<T> unsafeNaturalOrder() {
        return (o1, o2) -> o1.compareTo(o2);
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

    /**
     * Returns a copy of the given map where the key is an enum.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param keyType the type of the keys. This argument cannot be {@code null}.
     * @param src the map to be copied. This argument cannot be {@code null} and
     *   cannot contain {@code null} keys. However, {@code null} values are permitted.
     * @return the copy of the given map. This method never returns {@code null}.
     */
    public static <K extends Enum<K>, V> EnumMap<K, V> copyToEnumMap(
            Class<K> keyType,
            Map<? extends K, ? extends V> src) {
        EnumMap<K, V> result = new EnumMap<>(keyType);
        result.putAll(src);
        return result;
    }

    /**
     * Returns a read-only copy of the given map where the key is an enum. The returned
     * map is backed by an {@link EnumMap}.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param keyType the type of the keys. This argument cannot be {@code null}.
     * @param src the map to be copied. This argument cannot be {@code null} and
     *   cannot contain {@code null} keys. However, {@code null} values are permitted.
     * @return the read-only copy of the given map. This method never returns {@code null}.
     */
    public static <K extends Enum<K>, V> Map<K, V> copyToReadOnlyEnumMap(
            Class<K> keyType,
            Map<? extends K, ? extends V> src) {
        EnumMap<K, V> result = copyToEnumMap(keyType, src);
        return Collections.unmodifiableMap(result);
    }
}
