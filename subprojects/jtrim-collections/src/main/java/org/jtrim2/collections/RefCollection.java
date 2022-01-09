package org.jtrim2.collections;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Defines a collection whose elements can be referenced independently. These
 * {@link RefCollection.ElementRef element references} remain valid even
 * after the collection was modified.
 * <P>
 * The elements of this collection can easily and efficiently be replaced and
 * removed.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are not required to be thread-safe and
 * in general cannot be modified concurrently by multiple concurrent threads.
 * However reading by multiple concurrent threads are allowed. Note that
 * accessing the element references of a {@code RefCollection} is equivalent
 * to accessing the collection itself regarding thread safety.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are required to be
 * <I>synchronization transparent</I>, so they can be called in any context
 * (e.g.: while holding a lock).
 *
 * @param <E> the type of the elements in this collection
 *
 * @see RefLinkedList
 * @see RefList
 */
public interface RefCollection<E> extends Collection<E>, Streamable<E> {
    /**
     * Defines a reference to an element of a {@code RefCollection}. The
     * reference remains valid no matter how the underlying collection was
     * modified.
     *
     * <h3>Thread safety</h3>
     * Instances of this class derive their thread-safety properties from the
     * underlying collection.
     *
     * @param <E> the type of the referenced element
     */
    public static interface ElementRef<E> {
        /**
         * Replaces the element referenced by this reference. This method
         * will actually modify the content of the underlying collection.
         * <P>
         * This method works even if this element reference was already
         * removed from the collection. Even in this case the
         * {@link #getElement() getElement} method can be used to retrieve the
         * value set by this method.
         *
         * @param newElement the new value which will replace the currently
         *   stored element. Subsequent {@link #getElement() getElement()}
         *   method call will return this value. This argument can be
         *   {@code null} only if the underlying collection supports
         *   {@code null} elements.
         * @return the previous value which was replaced by this method call.
         *   This method may return {@code null} if the underlying collection
         *   supports {@code null} elements.
         *
         * @throws NullPointerException thrown if this collection does not
         *   support {@code null} elements
         * @throws UnsupportedOperationException thrown if the underlying
         *   collection is read-only and so it cannot be modified
         */
        public E setElement(E newElement);

        /**
         * Returns the element of the underlying collection stored by this
         * reference.
         * <P>
         * This method will work even after this reference was removed from the
         * underlying collection.
         *
         * @return the element of the underlying collection stored by this
         *   reference. This method may return {@code null} if the underlying
         *   collection supports {@code null} elements.
         */
        public E getElement();

        /**
         * Checks whether this element reference is still part of the underlying
         * collection or not. Once this method returns {@code true}, subsequent
         * calls will always return {@code true} because the reference cannot
         * be reinserted into the underlying collection.
         * <P>
         * After this element was removed, updating this reference will have no
         * effect on the underlying collection (since there will be no
         * underlying collection). Note however, that the referenced element is
         * still accessible through this element reference.
         *
         * @return {@code true} if this element reference is no longer part
         *   of the underlying collection, {@code false} otherwise
         */
        public boolean isRemoved();

        /**
         * Removes this element reference from the underlying collection. The
         * element referenced by this reference will no longer be part of the
         * underlying collection.
         * <P>
         * This method call is idempotent, that is once this reference was
         * removed, removing it again has no effect.
         * <P>
         * After this element was removed, updating this reference will have no
         * effect on the underlying collection (since there will be no
         * underlying collection). Note however, that the referenced element is
         * still accessible through this element reference. So even after this
         * method call the {@link #setElement(Object) setElement(E)} and
         * {@link #getElement() getElement()} methods will still work.
         */
        public void remove();
    }

    /**
     * Adds a new element to this collection and returns a reference to the
     * newly added element. The newly added element can later be replaced
     * or removed by the returned reference even if this collection was modified
     * since then.
     *
     * @param element the new element to be added to this collection. This
     *   argument can be {@code null} only if this collection supports
     *   {@code null} elements.
     * @return the reference which can be used to replace or remove the
     *   newly added element. This method never returns {@code null}.
     *
     * @throws UnsupportedOperationException thrown if this collection does not
     *   support adding new elements to it (possibly because it is readonly)
     * @throws ClassCastException thrown if the class of the specified element
     *   prevents it from being added to this collection. Note that since
     *   generics are implemented using erasure, this cannot always be verified.
     * @throws NullPointerException thrown if the specified element is null and
     *   this collection does not permit null elements
     * @throws IllegalArgumentException thrown if some property of the element
     *   prevents it from being added to this collection
     * @throws IllegalStateException thrown if the element cannot be added at
     *   this time due to insertion restrictions
     */
    public ElementRef<E> addGetReference(E element);

    /**
     * Returns an element reference to the specified reference found in this
     * collection. The element is looked up based on the {@code equals} method
     * of the elements the same way as the {@link #contains(Object) contains}
     * method does. In case there the element is contained in this collection
     * multiple times, reference to any of them can be returned.
     *
     * @param element the element to which a reference is to be returned. This
     *   argument can be {@code null} if this collection permits {@code null}
     *   elements. In case this implementation does not permit {@code null}
     *   elements but accept {@code null} for this argument, it must return
     *   {@code null} if {@code null} is passed for this argument.
     * @return an element reference to the specified reference found in this
     *   collection or {@code null} if the specified element is not in this
     *   collections
     *
     * @throws NullPointerException implementations may throw this exception
     *   if this collection does not permit {@code null} elements. However even
     *   in this case, an implementation is not required to raise this
     *   exception.
     */
    public ElementRef<E> findReference(E element);

    /**
     * {@inheritDoc }
     */
    @Override
    public default Stream<E> stream() {
        return Collection.super.stream();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public default void forEach(Consumer<? super E> action) {
        Collection.super.forEach(action);
    }
}
