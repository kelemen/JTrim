package org.jtrim2.collections;

import java.util.List;
import java.util.ListIterator;

/**
 * Defines a list whose elements can be referenced independently. These
 * {@link RefList.ElementRef element references} remain valid even
 * after the list was modified.
 * <P>
 * The references can be used for various purposes, like inserting a new element
 * after or before a given element, incrementing or decrementing the position
 * of the element is the list.
 * <P>
 * Unlike general {@link List} implementations, implementations of this
 * interface in general are not allowed to have more than
 * {@link Integer#MAX_VALUE} elements. Some implementations may ignore this
 * restriction but there is no general rule how methods should work when
 * the size of such lists exceed {@code Integer#MAX_VALUE}.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are not required to be thread-safe and
 * in general cannot be modified concurrently by multiple concurrent threads.
 * However reading by multiple concurrent threads are allowed. Note that
 * accessing the element references of a {@code RefList} is equivalent to
 * accessing the collection itself regarding thread safety.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are required to be
 * <I>synchronization transparent</I>, so they can be called in any context
 * (e.g.: while holding a lock).
 *
 * @param <E> the type of the elements in this list
 *
 * @see RefLinkedList
 */
public interface RefList<E> extends List<E>, RefCollection<E> {
    /**
     * Defines a reference to an element of a {@code RefList}. The
     * reference remains valid no matter how the underlying list was
     * modified.
     *
     * <h3>Thread safety</h3>
     * Instances of this class derive their thread-safety properties from the
     * underlying collection.
     *
     * @param <E> the type of the referenced element
     */
    public static interface ElementRef<E> extends RefCollection.ElementRef<E> {
        /**
         * Returns the index of the element of this reference in the underlying
         * list. Assume that {@code list} is the underlying list of {@code ref},
         * in this case the following condition must hold:
         * {@code ref.isRemoved() || ref == list.getReference(ref.getIndex())}.
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @return the index of the element of this reference in the underlying
         *   list. This method always returns an integer greater than or equal
         *   to zero.
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         */
        public int getIndex();

        /**
         * Returns an iterator which will iterate through the elements of the
         * underlying list starting from this element. The first element
         * returned therefore will be this element.
         * <P>
         * It is undefined how the returned iterator should work if the
         * underlying list was modified while iterating through the returned
         * iterator. Implementations may or may not fail to work under such
         * condition. Therefore it is highly advised that the underlying list
         * should not be modified while iterating through the elements of the
         * return iterator.
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @return an iterator which will iterate through the elements of the
         *   underlying list starting from this element. The first element
         *   returned therefore will be this element. This method never returns
         *   {@code null}.
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         */
        public ListIterator<E> getIterator();

        /**
         * Returns the next {@code step}th element of the underlying list. That
         * is, this method returns the element with an {@link #getIndex() index}
         * larger by {@code step} than the index of this element.
         * <P>
         * This method is allowed to be called with a negative {@code step}
         * argument, this is effectively calling
         * {@link #getPrevious(int) getPrevious(Math.abs(step))} except when
         * {@code step == Integer.MIN_VALUE} where it is the element one step
         * before {@code getPrevious(Integer.MAX_VALUE)}.
         * <P>
         * Notice that {@code getNext(0)} always returns this reference.
         * <P>
         * This method can be called after this referenced element was removed
         * from the underlying list. In this case this method is defined to
         * return {@code null}.
         * <P>
         * <B>Note to implementors</B>: Don't forget that
         * {@code Math.abs(Integer.MIN_VALUE) == Integer.MIN_VALUE}.
         *
         * @param step the number of elements to skip forward (including this
         *   reference). This argument can be any integer and negative values
         *   are effectively stepping backward with the absolute {@code step}
         *   value.
         * @return the next element of the underlying list after skipping
         *   {@code step} number of elements (including this reference) or
         *   {@code null} if no such element exists (either because there is no
         *   such element in the underlying list or this element was already
         *   removed from the list)
         */
        public ElementRef<E> getNext(int step);

        /**
         * Returns the previous {@code step}th element of the underlying list.
         * That is, this method returns the element with an
         * {@link #getIndex() index} lower by {@code step} than the index of
         * this element.
         * <P>
         * This method is allowed to be called with a negative {@code step}
         * argument, this is effectively calling
         * {@link #getNext(int) getNext(Math.abs(step))} except when
         * {@code step == Integer.MIN_VALUE} where it is the element one step
         * after {@code getNext(Integer.MAX_VALUE)}.
         * <P>
         * Notice that {@code getPrevious(0)} always returns this reference.
         * <P>
         * This method can be called after this referenced element was removed
         * from the underlying list. In this case this method is defined to
         * return {@code null}.
         * <P>
         * <B>Note to implementors</B>: Don't forget that
         * {@code Math.abs(Integer.MIN_VALUE) == Integer.MIN_VALUE}.
         *
         * @param step the number of elements to skip backward (including this
         *   reference). This argument can be any integer and negative values
         *   are effectively stepping forward with the absolute {@code step}
         *   value.
         * @return the previous element of the underlying list after skipping
         *   {@code step} number of elements (including this reference) or
         *   {@code null} if no such element exists (either because there is no
         *   such element in the underlying list or this element was already
         *   removed from the list)
         */
        public ElementRef<E> getPrevious(int step);

        /**
         * Makes the element referenced by this reference to be the last element
         * of the underlying list. After this method returns successfully the
         * {@link #getIndex() index} of this element will be the size of the
         * underlying list minus one.
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         */
        public void moveLast();

        /**
         * Makes the element referenced by this reference to be the last element
         * of the underlying list. After this method returns successfully the
         * {@link #getIndex() index} of this element will be zero.
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         */
        public void moveFirst();

        /**
         * Moves the element referenced by this reference backward in the
         * list by {@code count} if possible. This method will not move the
         * reference out of the underlying list, instead this method will
         * then make this reference as the first or last element of the
         * underlying list (depending on the sign of {@code count}).
         * <P>
         * This method is allowed to be called with negative argument. In this
         * case this method call is effectively the same as calling
         * {@link #moveForward(int) moveForward(Math.abs(count))} except when
         * {@code count == Integer.MIN_VALUE} where this method will move one
         * step after {@code moveForward(Integer.MAX_VALUE)}.
         * <P>
         * Notice that calling {@code moveBackward(0)} does nothing.
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @param count the amount to push this element backward in the
         *   underlying list. This argument can be any integer and negative
         *   values are effectively the same as moving forward with the absolute
         *   value.
         * @return the amount this element was pushed backward in the underlying
         *   list. The new index of this reference was reduced by this amount.
         *   Note this value may not equal to the argument only if this element
         *   would needed to be pushed beyond the ends of the list.
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         */
        public int moveBackward(int count);

        /**
         * Moves the element referenced by this reference forward in the
         * list by {@code count} if possible. This method will not move the
         * reference out of the underlying list, instead this method will
         * then make this reference as the first or last element of the
         * underlying list (depending on the sign of {@code count}).
         * <P>
         * This method is allowed to be called with negative argument. In this
         * case this method call is effectively the same as calling
         * {@link #moveBackward(int) moveBackward(Math.abs(count))} except when
         * {@code count == Integer.MIN_VALUE} where this method will move one
         * step before {@code moveBackward(Integer.MAX_VALUE)}.
         * <P>
         * Notice that calling {@code moveForward(0)} does nothing.
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @param count the amount to push this element forward in the
         *   underlying list. This argument can be any integer and negative
         *   values are effectively the same as moving backward with the
         *   absolute value.
         * @return the amount this element was pushed forward in the underlying
         *   list. The new index of this reference was increased by this amount.
         *   Note this value may not equal to the argument only if this element
         *   would needed to be pushed beyond the ends of the list.
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         */
        public int moveForward(int count);

        /**
         * Adds an element after the element referenced by this reference. The
         * newly added element will be the next element in the underlying list
         * after this reference.
         * <P>
         * The following two asserts succeeds given a {@code ref} element
         * reference and a {@code newElement} element:
         * <ul>
         *  <li>
         *   <code>ref.addAfter(newElement);<P>
         *   assert ref.getNext(1).getElement() == newElement;</code>
         *  </li>
         *  <li>
         *   {@code assert ref.addAfter(newElement).getIndex() == ref.getIndex() + 1;}
         *  </li>
         * </ul>
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @param newElement the new element to be added to the underlying list
         *   after this element. This argument can be {@code null} only if the
         *   underlying collection supports {@code null} elements.
         * @return the element reference to the newly added element. This method
         *   never returns {@code null}.
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         * @throws NullPointerException thrown if this collection does not
         *   support {@code null} elements
         * @throws UnsupportedOperationException thrown if the underlying
         *   collection does not support new elements to added
         */
        public ElementRef<E> addAfter(E newElement);

        /**
         * Adds an element before the element referenced by this reference. The
         * newly added element will be the previous element in the underlying
         * list before this reference.
         * <P>
         * The following two asserts succeeds given a {@code ref} element
         * reference and a {@code newElement} element:
         * <ul>
         *  <li>
         *   <code>ref.addBefore(newElement);<P>
         *   assert ref.getPrevious(1).getElement() == newElement;</code>
         *  </li>
         *  <li>
         *   {@code assert ref.addBefore(newElement).getIndex() == ref.getIndex() - 1;}
         *  </li>
         * </ul>
         * <P>
         * <B>Warning</B>: This method must not be called after this reference
         * was {@link #remove() removed} from the underlying list (i.e.:
         * {@link #isRemoved() isRemoved()} returns {@code true}).
         *
         * @param newElement the new element to be added to the underlying list
         *   before this element. This argument can be {@code null} only if the
         *   underlying collection supports {@code null} elements.
         * @return the element reference to the newly added element. This method
         *   never returns {@code null}.
         *
         * @throws IllegalStateException thrown if this reference was already
         *   removed from the underlying list
         * @throws NullPointerException thrown if this collection does not
         *   support {@code null} elements
         * @throws UnsupportedOperationException thrown if the underlying
         *   collection does not support new elements to added
         */
        public ElementRef<E> addBefore(E newElement);
    }

    /**
     * Returns the reference to the element equivalent (based on the
     * {@code equals} method) to the given element with the lowest index.
     *
     * @param element the element to be looked for in this list. This argument
     *   can be {@code null} if this collection permits {@code null} elements.
     *   In case this implementation does not permit {@code null} elements but
     *   accept {@code null} for this argument, it must return {@code null} if
     *   {@code null} is passed for this argument.
     * @return the reference to the element equivalent to the given element
     *   with the lowest index or {@code null} if no such element was found
     *
     * @throws NullPointerException implementations may throw this exception
     *   if this collection does not permit {@code null} elements. However even
     *   in this case, an implementation is not required to raise this
     *   exception.
     */
    public ElementRef<E> findFirstReference(E element);

    /**
     * Returns the reference to the element equivalent (based on the
     * {@code equals} method) to the given element with the highest index.
     *
     * @param element the element to be looked for in this list. This argument
     *   can be {@code null} if this collection permits {@code null} elements.
     *   In case this implementation does not permit {@code null} elements but
     *   accept {@code null} for this argument, it must return {@code null} if
     *   {@code null} is passed for this argument.
     * @return the reference to the element equivalent to the given element
     *   with the highest index or {@code null} if no such element was found
     *
     * @throws NullPointerException implementations may throw this exception
     *   if this collection does not permit {@code null} elements. However even
     *   in this case, an implementation is not required to raise this
     *   exception.
     */
    public ElementRef<E> findLastReferece(E element);

    /**
     * Returns the reference to the first element of this list or {@code null}
     * if this list is empty.
     *
     * @return the reference to the first element of this list or {@code null}
     *   if this list is empty
     */
    public ElementRef<E> getFirstReference();

    /**
     * Returns the reference to the last element of this list or {@code null}
     * if this list is empty.
     *
     * @return the reference to the last element of this list or {@code null}
     *   if this list is empty
     */
    public ElementRef<E> getLastReference();

    /**
     * Returns the reference to the element at the given index.
     *
     * @param index the index of the requested element. This argument must
     *   be greater than or equal to zero and lesser than the size of this list.
     * @return the reference to the element at the given index. This method may
     *   never return {@code null}.
     *
     * @throws IndexOutOfBoundsException thrown if the specified index points
     *   to an element outside of this list
     */
    public ElementRef<E> getReference(int index);

    /**
     * Adds a new element as the first (with zero index) element of this list
     * and returns a reference to it. A subsequent call to
     * {@link #getFirstReference() getFirstReference()} will return the same
     * reference. The newly added element can later be replaced or removed by
     * the returned reference even if this collection was modified since then.
     * <P>
     * This method is effectively equivalent to calling:
     * {@code addGetReference(0, element)}.
     *
     * @param element the new element to be added to this list as the first
     *   element. This argument can be {@code null} only if this collection
     *   supports {@code null} elements.
     * @return  the reference which can be used to replace or remove the
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
    public ElementRef<E> addFirstGetReference(E element);

    /**
     * Adds a new element as the last (with {@code size() - 1} index) element of
     * this list and returns a reference to it. A subsequent call to
     * {@link #getLastReference() getLastReference()} will return the same
     * reference. The newly added element can later be replaced or removed by
     * the returned reference even if this collection was modified since then.
     * <P>
     * This method is effectively equivalent to calling:
     * {@code addGetReference(0, size())}.
     *
     * @param element the new element to be added to this list as the last
     *   element. This argument can be {@code null} only if this collection
     *   supports {@code null} elements.
     * @return  the reference which can be used to replace or remove the
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
    public ElementRef<E> addLastGetReference(E element);

    /**
     * Adds a new element to this list with the given index and returns a
     * reference to it. A subsequent call to
     * {@link #getReference(int) getReference(index)} will return the same
     * reference. The newly added element can later be replaced or removed by
     * the returned reference even if this collection was modified since then.
     * To append the element to the end of this list specify the size of this
     * list as the index.
     *
     * @param index the index to which the new element is to be inserted.
     *   This argument must be greater than or equal to zero and lesser than
     *   or equal to zero.
     * @param element the new element to be added to this list at the given
     *   index. This argument can be {@code null} only if this collection
     *   supports {@code null} elements.
     * @return  the reference which can be used to replace or remove the
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
     * @throws IndexOutOfBoundsException if the index is out of range
     *   ({@code index &lt; 0 || index &gt; size()}
     */
    public ElementRef<E> addGetReference(int index, E element);
}
