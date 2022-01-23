package org.jtrim2.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
     * Creates a new {@link java.util.LinkedHashMap LinkedHashMap} specifying the
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
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
        return newLinkedHashMap(expectedSize, DEFAULT_HASHMAP_LOAD_FACTOR);
    }

    /**
     * Creates a new {@link java.util.LinkedHashMap LinkedHashMap} specifying the
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
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize, float loadFactor) {
        int capacity = getRequiredHashCapacity(expectedSize, loadFactor);
        return new LinkedHashMap<>(capacity, loadFactor);
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
    public static <K, V> HashMap<K, V> newHashMap(int expectedSize, float loadFactor) {
        int capacity = getRequiredHashCapacity(expectedSize, loadFactor);
        return new HashMap<>(capacity, loadFactor);
    }

    private static int getRequiredHashCapacity(int expectedSize, float loadFactor) {
        ExceptionHelper.checkArgumentInRange(expectedSize, 0, Integer.MAX_VALUE, "expectedSize");

        int capacity = (int) ((double) expectedSize / (double) loadFactor) + 1;
        return capacity >= 1 ? capacity : 1;
    }

    /**
     * Creates a new {@link java.util.LinkedHashSet LinkedHashSet} specifying the
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
    public static <E> LinkedHashSet<E> newLinkedHashSet(int expectedSize) {
        return newLinkedHashSet(expectedSize, DEFAULT_HASHMAP_LOAD_FACTOR);
    }

    /**
     * Creates a new {@link java.util.LinkedHashSet LinkedHashSet} specifying the
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
    public static <E> LinkedHashSet<E> newLinkedHashSet(int expectedSize, float loadFactor) {
        int capacity = getRequiredHashCapacity(expectedSize, loadFactor);
        return new LinkedHashSet<>(capacity, loadFactor);
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
    public static <E> HashSet<E> newHashSet(int expectedSize, float loadFactor) {
        int capacity = getRequiredHashCapacity(expectedSize, loadFactor);
        return new HashSet<>(capacity, loadFactor);
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
     * Returns a readonly view of the given list with the given head element as the head
     * of the new list. That is, the returned list will have exactly one element more
     * than the given source list.
     * <P>
     * Calling this method is effectively equivalent to calling:
     * {@code viewConcatList(Collections.singletonList(head), list2)}
     *
     * @param <E> the type of the elements in the list
     * @param head the first element of the returned list. This argument can be {@code null},
     *   in which case the returned list will have {@code null} as its first element.
     * @param list2 the second part of the concatenated list. This argument
     *   cannot be {@code null}.
     * @return a readonly view of the given list with the given head element. This mehtod
     *   never returns {@code null}.
     */
    public static <E> List<E> viewListWithHead(E head, List<? extends E> list2) {
        return viewConcatList(Collections.singletonList(head), list2);
    }

    /**
     * Returns a readonly view of the given list with the given tail element as the tail
     * of the new list. That is, the returned list will have exactly one element more
     * than the given source list.
     * <P>
     * Calling this method is effectively equivalent to calling:
     * {@code viewConcatList(list1, Collections.singletonList(tail))}
     *
     * @param <E> the type of the elements in the list
     * @param list1 the first part of the concatenated list. This argument
     *   cannot be {@code null}.
     * @param tail the last element of the returned list. This argument can be {@code null},
     *   in which case the returned list will have {@code null} as its last element.
     * @return a readonly view of the given list with the given tail element. This mehtod
     *   never returns {@code null}.
     */
    public static <E> List<E> viewListWithTail(List<? extends E> list1, E tail) {
        return viewConcatList(list1, Collections.singletonList(tail));
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

    /**
     * Returns a new {@code ArrayList} with the same content as the give source {@code Collection}
     * after filtering using the given {@code Predicate}. That is, this code is logically equivalent to:
     * <pre>{@code
     * src.stream().filter(filter).collect(Collectors.toCollection(ArrayList::new))
     * }</pre>
     * The returned list is guaranteed to preserve the same iteration order as the source collection.
     *
     * @param <E> the type of the elements of the collection to be filtered (and thus the output list as well)
     * @param src the source collection to be filtered. This argument cannot be {@code null}.
     * @param filter the condition returning {@code true} for elements to be kept in the returned list.
     *   This argument cannot be {@code null}.
     * @return a new {@code ArrayList} containing the elements of the given source collection after
     *   being filtered according to the given {@code Predicate}. This method never returns {@code null}.
     */
    public static <E> ArrayList<E> filterToNewList(Collection<? extends E> src, Predicate<? super E> filter) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(filter, "filter");

        ArrayList<E> result = new ArrayList<>();
        src.forEach(e -> {
            if (filter.test(e)) {
                result.add(e);
            }
        });
        return result;
    }

    /**
     * Returns a new {@code ArrayList} with the same content as the give source {@code Collection}
     * after mapping its elements using the given mapper function. Notice that the resulting list will
     * have the same size as the source collection. That is, this code is logically equivalent to:
     * <pre>{@code
     * src.stream().map(mapper).collect(Collectors.toCollection(ArrayList::new))
     * }</pre>
     * The returned list is guaranteed to preserve the same iteration order as the source collection.
     *
     * @param <E> the type of the elements of the collection to be filtered
     * @param <R> the type of the elements of the output list
     * @param src the source collection to be filtered. This argument cannot be {@code null}.
     * @param mapper the function mapping the elements of the source collection. The mapper will be
     *   passed {@code null} only if the source collection contains {@code null} elements. If this
     *   mapper returns {@code null} for an element, then {@code null} will be put into the result list.
     *   This argument cannot be {@code null}.
     * @return a new {@code ArrayList} containing the elements of the given source collection after
     *   being filtered according to the given {@code Predicate}. This method never returns {@code null}.
     */
    public static <E, R> ArrayList<R> mapToNewList(
            Collection<? extends E> src,
            Function<? super E, ? extends R> mapper) {

        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(mapper, "mapper");

        ArrayList<R> result = new ArrayList<>(src.size());
        src.forEach(e -> {
            result.add(mapper.apply(e));
        });
        return result;
    }

    /**
     * Returns a new {@code ArrayList} with the same content as the give source {@code Collection}
     * after filtering then mapping its elements using the given {@code Predicate} and mapper function.
     * That is, this code is logically equivalent to:
     * <pre>{@code
     * src.stream().filter(filter).map(mapper).collect(Collectors.toCollection(ArrayList::new))
     * }</pre>
     * The returned list is guaranteed to preserve the same iteration order as the source collection.
     *
     * @param <E> the type of the elements of the collection to be filtered and mapped
     * @param <R> the type of the elements of the output list
     * @param src the source collection to be filtered. This argument cannot be {@code null}.
     * @param filter the condition returning {@code true} for elements to be kept in the returned list.
     *   This argument cannot be {@code null}.
     * @param mapper the function mapping the elements of the source collection. The mapper will be
     *   passed {@code null} only if the source collection contains {@code null} elements. If this
     *   mapper returns {@code null} for an element, then {@code null} will be put into the result list.
     *   This argument cannot be {@code null}.
     * @return a new {@code ArrayList} with the same content as the give source {@code Collection}
     *   after filtering then mapping its elements using the given {@code Predicate} and mapper function.
     *   This method never returns {@code null}.
     */
    public static <E, R> ArrayList<R> filterAndMapToNewList(
            Collection<? extends E> src,
            Predicate<? super E> filter,
            Function<? super E, ? extends R> mapper) {

        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(mapper, "mapper");

        ArrayList<R> result = new ArrayList<>();
        src.forEach(e -> {
            if (filter.test(e)) {
                result.add(mapper.apply(e));
            }
        });
        return result;
    }

    /**
     * Returns a new {@code ArrayList} with the same content as the give source {@code Collection}
     * after mapping then filtering its elements using the given mapper function and {@code Predicate}.
     * That is, this code is logically equivalent to:
     * <pre>{@code
     * src.stream().filter(filter).map(mapper).collect(Collectors.toCollection(ArrayList::new))
     * }</pre>
     * The returned list is guaranteed to preserve the same iteration order as the source collection.
     *
     * @param <E> the type of the elements of the collection to be filtered and mapped
     * @param <R> the type of the elements of the output list
     * @param src the source collection to be filtered. This argument cannot be {@code null}.
     * @param mapper the function mapping the elements of the source collection. The mapper will be
     *   passed {@code null} only if the source collection contains {@code null} elements. If this
     *   mapper returns {@code null} for an element, then {@code null} will be put into the result list.
     *   This argument cannot be {@code null}.
     * @param filter the condition returning {@code true} for elements to be kept in the returned list.
     *   This argument cannot be {@code null}.
     * @return a new {@code ArrayList} with the same content as the give source {@code Collection}
     *   after mapping then filtering its elements using the given mapper function and {@code Predicate}.
     *   This method never returns {@code null}.
     */
    public static <E, R> ArrayList<R> mapAndFilterToNewList(
            Collection<? extends E> src,
            Function<? super E, ? extends R> mapper,
            Predicate<? super R> filter) {

        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(filter, "filter");

        ArrayList<R> result = new ArrayList<>();
        src.forEach(e -> {
            R mapped = mapper.apply(e);
            if (filter.test(mapped)) {
                result.add(mapped);
            }
        });
        return result;
    }
}
