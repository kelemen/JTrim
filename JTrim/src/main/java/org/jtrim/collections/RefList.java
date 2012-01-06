package org.jtrim.collections;

import java.util.*;

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
 * @author Kelemen Attila
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

        public ElementRef<E> addAfter(E newElement);
        public ElementRef<E> addBefore(E newElement);
    }

    public ElementRef<E> findFirstReference(E o);
    public ElementRef<E> findLastReferece(E o);

    public ElementRef<E> getFirstReference();
    public ElementRef<E> getLastReference();
    public ElementRef<E> getReference(int index);

    public ElementRef<E> addFirstGetReference(E element);
    public ElementRef<E> addLastGetReference(E element);
    public ElementRef<E> addGetReference(int index, E element);
}
