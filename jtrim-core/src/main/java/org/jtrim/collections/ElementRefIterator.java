package org.jtrim.collections;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.jtrim.collections.RefList.ElementRef;

/**
 * A convenient {@link java.util.ListIterator ListIterator} for iterating
 * through the element references of a {@link RefList}. This {@code Iterator}
 * will use {@link RefList.ElementRef#getNext(int)} and
 * {@link RefList.ElementRef#getPrevious(int)} method of the element references
 * and the iterator remains valid even if the underlying list is changed while
 * it is being iterated (<B>Removing from the underlying list however risks
 * making the iterator an empty iterator</B>). Note however that it doesn't
 * imply thread-safety.
 * <P>
 * This implementation supports {@link #remove() removing} elements if the
 * underlying list implementation supports it. Note that the
 * {@link #set(org.jtrim.collections.RefList.ElementRef) set} and the
 * {@link #add(org.jtrim.collections.RefList.ElementRef) add} methods are not
 * supported and will throw unchecked exceptions. Instead it provides the
 * {@link #setElement(java.lang.Object) setElement(E)} and the
 * {@link #addElement(java.lang.Object) addElement(E)} methods as a replacement.
 * <P>
 * <B>Warning</B>: There are three limitations of this implementation:
 * <ul>
 *  <li>
 *   If the element last returned by the iterator (either by
 *   {@link #next() next()} or {@link #previous() previous()}), this iterator
 *   immediately becomes an empty iterator. In case no element was returned yet,
 *   the current element is the starting element specified at construction time.
 *   Removing an element returned by {@code next()} makes the previous element
 *   the current element (except when at the beginning of the underlying list
 *   where chooses the next element) and removing an element returned by
 *   {@code previous()} makes the next element the current element (except when
 *   at the end of the underlying list where chooses the previous element).
 *  </li>
 *  <li>
 *   Once the iterator becomes an empty iterator (by any means), adding new
 *   elements will no longer modify the underlying list.
 *  </li>
 *  <li>
 *   If the iterator is empty and new element is
 *   {@link #addElement(java.lang.Object) added} through this iterator, it will
 *   use a newly created {@link RefLinkedList} as an underlying list and not
 *   the one used to back this list before it contained element.
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * Instances of this class are not safe to use by multiple concurrent threads
 * if at least one of them modifies the underlying list even if the underlying
 * list implementation is thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * This class derives its synchronization transparency from the underlying
 * {@code RefList} object. Note however that in general
 * {@code java.util.Collection Collection} implementations are expected to be
 * completely synchronization transparent.
 *
 * @param <E> the type of the elements iterated by this {@code Iterator}
 *
 * @author Kelemen Attila
 */
public final class ElementRefIterator<E>
implements
        ListIterator<RefList.ElementRef<E>> {
    // last non-null return value if firstRef was not null
    private RefList.ElementRef<E> lastReturned;
    private ReturnedBy returnedBy;
    private boolean mayRemove;

    /**
     * Creates an iterator with a specified first element reference. Note that
     * this iterator may iterate before the specified starting reference if
     * it is not at the start of the underlying list.
     *
     * @param firstRef the first element reference to be returned by
     *   this {@code Iterator}. This argument can be {@code null} in which case
     *   the iterator is empty.
     */
    public ElementRefIterator(RefList.ElementRef<E> firstRef) {
        this.mayRemove = false;
        this.returnedBy = ReturnedBy.PREVIOUS;
        this.lastReturned = firstRef;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean hasNext() {
        if (lastReturned == null) {
            return false;
        }

        switch (returnedBy) {
            case PREVIOUS:
                return true;
            case NEXT:
                return lastReturned.getNext(1) != null;
            default:
                throw new AssertionError();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public RefList.ElementRef<E> next() {
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "The last reference has been reached.");
        }

        switch (returnedBy) {
            case PREVIOUS:
                // return lastReturned
                break;
            case NEXT:
                lastReturned = lastReturned.getNext(1);
                break;
            default:
                throw new AssertionError();
        }

        mayRemove = true;
        returnedBy = ReturnedBy.NEXT;
        return lastReturned;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void remove() {
        if (!mayRemove) {
            throw new IllegalStateException("next() was not called yet.");
        }

        RefList.ElementRef<E> nextRef;
        switch (returnedBy) {
            case PREVIOUS:
                nextRef = lastReturned.getNext(1);
                if (nextRef == null) {
                    nextRef = lastReturned.getPrevious(1);
                    returnedBy = ReturnedBy.NEXT;
                }
                break;
            case NEXT:
                nextRef = lastReturned.getPrevious(1);
                if (nextRef == null) {
                    nextRef = lastReturned.getNext(1);
                    returnedBy = ReturnedBy.PREVIOUS;
                }
                break;
            default:
                throw new AssertionError();
        }

        lastReturned.remove();
        lastReturned = nextRef;
        mayRemove = false;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean hasPrevious() {
        if (lastReturned == null) {
            return false;
        }

        switch (returnedBy) {
            case PREVIOUS:
                return lastReturned.getPrevious(1) != null;
            case NEXT:
                return true;
            default:
                throw new AssertionError();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ElementRef<E> previous() {
        if (!hasPrevious()) {
            throw new NoSuchElementException(
                    "The first reference has been reached.");
        }

        switch (returnedBy) {
            case PREVIOUS:
                lastReturned = lastReturned.getPrevious(1);
                break;
            case NEXT:
                // return lastReturned
                break;
            default:
                throw new AssertionError();
        }

        mayRemove = true;
        returnedBy = ReturnedBy.PREVIOUS;
        return lastReturned;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int nextIndex() {
        if (lastReturned == null) {
            return 0;
        }

        switch (returnedBy) {
            case PREVIOUS:
                return lastReturned.getIndex();
            case NEXT:
                return lastReturned.getIndex() + 1;
            default:
                throw new AssertionError();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int previousIndex() {
        if (lastReturned == null) {
            return -1;
        }

        switch (returnedBy) {
            case PREVIOUS:
                return lastReturned.getIndex() - 1;
            case NEXT:
                return lastReturned.getIndex();
            default:
                throw new AssertionError();
        }
    }

    /**
     * Replaces the last element of the reference returned by {@link #next} or
     * {@link #previous} with the specified element.
     * This call can be made only if neither {@link #remove} nor {@link
     * #add} have been called after the last call to {@code next} or
     * {@code previous}.
     *
     * @param element the element with which to replace the last element
     *   returned by {@code next} or {@code previous}
     * @throws ClassCastException if the class of the specified element
     *   prevents it from being added to this list
     * @throws IllegalArgumentException if some aspect of the specified
     *   element prevents it from being added to the underlying list
     * @throws IllegalStateException if neither {@code next} nor
     *   {@code previous} have been called, or {@code remove} or
     *   {@code add} have been called after the last call to
     *   {@code next} or {@code previous}
     */
    public void setElement(E element) {
        if (!mayRemove) {
            throw new IllegalStateException(
                    "The element cannot be replaced in this state.");
        }

        lastReturned.setElement(element);
    }

    /**
     * Inserts the specified element into the list.
     * The element is inserted immediately before the element that
     * would be returned by {@link #next}, if any, and after the element
     * that would be returned by {@link #previous}, if any. (If the
     * list contains no elements, the new element becomes the sole element
     * on the list.) The new element is inserted before the implicit
     * cursor: a subsequent call to {@code next} would be unaffected, and a
     * subsequent call to {@code previous} would return the new element.
     * (This call increases by one the value that would be returned by a
     * call to {@code nextIndex} or {@code previousIndex}.)
     *
     * @param element the element to insert
     * @throws ClassCastException if the class of the specified element
     *   prevents it from being added to this list
     * @throws IllegalArgumentException if some aspect of this element
     *   prevents it from being added to this list
     */
    public void addElement(E element) {
        if (lastReturned == null || lastReturned.isRemoved()) {
            // Construct a new underlying list if there was nothing specified
            // at construction time.
            RefList<E> newList = new RefLinkedList<>();
            lastReturned = newList.addFirstGetReference(element);
            returnedBy = ReturnedBy.NEXT;
            mayRemove = false;
            return;
        }

        switch (returnedBy) {
            case PREVIOUS:
                lastReturned.addBefore(element);
                break;
            case NEXT:
                lastReturned = lastReturned.addAfter(element);
                break;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @param e this argument is ignored
     *
     * @throws UnsupportedOperationException thrown always
     */
    @Override
    public void set(ElementRef<E> e) {
        throw new UnsupportedOperationException(
                "Element references cannot be replace.");
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @param e this argument is ignored
     *
     * @throws UnsupportedOperationException thrown always
     */
    @Override
    public void add(ElementRef<E> e) {
        throw new UnsupportedOperationException(
                "Element references cannot be inserted.");
    }

    private static enum ReturnedBy {
        NEXT, PREVIOUS
    }
}
