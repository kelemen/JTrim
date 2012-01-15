package org.jtrim.collections;

import java.util.*;
import org.jtrim.collections.RefList.ElementRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * A doubly-linked list implementation of the {@link RefList} interface. This
 * implementation is very similar to {@link java.util.LinkedList} but implements
 * the more powerful {@code RefList} interface which allows users to take full
 * advantage of the linked list. The performance is roughly the same as the
 * the performance of {@code java.util.LinkedList}.
 * <P>
 * This implementation allows {@code null} elements to be stored and implements
 * all optional operations.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are not safe to share across multiple concurrent
 * threads if at least one of those threads modify the list. To modify this
 * class concurrently, access to instances must be synchronized.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * <h3>Implementation notes</h3>
 * There some features yet to be implemented for this class:
 * <ul>
 *  <li>
 *   This class is not yet serializable.
 *  </li>
 *  <li>
 *   This class does not implement fail-fast behaviour like most collection
 *   implementation. This will be fixed in the future.
 *  </li>
 * </ul>
 *
 * @param <E> the type of the elements in this list
 *
 * @author Kelemen Attila
 */
public final class RefLinkedList<E>
extends
        AbstractSequentialList<E>
implements
        RefList<E>, Deque<E> {

    private static final String REMOVED_REF
            = "The reference was detached from the list.";

    private class LinkedRef implements ElementRef<E> {
        private E element;
        private LinkedRef prev;
        private LinkedRef next;

        private LinkedRef(E element) {
            this.element = element;
        }

        @Override
        public int getIndex() {
            assert this != head && this != tail
                    : "The head and the tail element does not have an index.";

            if (isRemoved()) {
                throw new IllegalStateException(REMOVED_REF);
            }

            int index = 0;
            LinkedRef currentRef = prev;
            while (currentRef != head) {
                currentRef = currentRef.prev;
                index++;
            }

            return index;
        }

        @Override
        public E setElement(E newElement) {
            assert this != head && this != tail
                    : "The head and tail of the list cannot have an element.";

            E oldElement = element;
            element = newElement;
            return oldElement;
        }

        @Override
        public E getElement() {
           assert this != head && this != tail
                    : "The head and tail of the list cannot have an element.";

            return element;
        }

        @Override
        public ListIterator<E> getIterator() {
            if (!isRemoved()) {
                return new ReferenceIterator(this);
            }
            else {
                throw new IllegalStateException(REMOVED_REF);
            }
        }

        private LinkedRef getNext() {
            return next != tail ? next : null;
        }

        private LinkedRef getPrevious() {
            return prev != head ? prev : null;
        }

        @Override
        public LinkedRef getNext(int step) {
            if (step < 0) {
                // Math.abs does not work on Integer.MIN_VALUE
                if (step == Integer.MIN_VALUE) {
                    LinkedRef result = getPrevious(Integer.MAX_VALUE);
                    if (result != null) {
                        result = getPrevious();
                    }
                    return result;
                }

                return getPrevious(Math.abs(step));
            }
            else {
                LinkedRef result = this;
                for (int i = 0; i < step && result != null; i++) {
                    result = result.getNext();
                }
                return result;
            }
        }

        @Override
        public LinkedRef getPrevious(int step) {
            if (step < 0) {
                // Math.abs does not work on Integer.MIN_VALUE
                if (step == Integer.MIN_VALUE) {
                    LinkedRef result = getNext(Integer.MAX_VALUE);
                    if (result != null) {
                        result = getNext();
                    }
                    return result;
                }

                return getNext(Math.abs(step));
            }
            else {
                LinkedRef result = this;
                for (int i = 0; i < step && result != null; i++) {
                    result = result.getPrevious();
                }
                return result;
            }
        }

        @Override
        public void moveLast() {
            if (isRemoved()) {
                throw new IllegalStateException(REMOVED_REF);
            }

            if (next != tail) {
                // detach
                next.prev = prev;
                prev.next = next;

                // insert
                prev = tail.prev;
                next = tail;

                tail.prev = this;
            }
        }

        @Override
        public void moveFirst() {
            if (isRemoved()) {
                throw new IllegalStateException(REMOVED_REF);
            }

            if (prev != head) {
                // detach
                next.prev = prev;
                prev.next = next;

                // insert
                prev = head;
                next = head.next;

                head.next = this;
            }
        }

        private boolean moveBackwardOne() {
            if (prev == head) {
                return false;
            }
            else {
                LinkedRef prevRef = prev;
                // detach
                next.prev = prevRef;
                prevRef.next = next;

                // insert
                prev = prevRef.prev;
                next = prevRef;
                return true;
            }
        }

        @Override
        public int moveBackward(int count) {
            if (isRemoved()) {
                throw new IllegalStateException(REMOVED_REF);
            }

            if (count < 0) {
                // Math.abs does not work on Integer.MIN_VALUE
                if (count == Integer.MIN_VALUE) {
                    int moveF1 = moveForward(Integer.MAX_VALUE);
                    int moveF2 = 0;
                    if (moveF1 == Integer.MAX_VALUE) {
                        moveF2 = moveForwardOne() ? 1 : 0;
                    }

                    return -moveF1 - moveF2;
                }

                return -moveForward(Math.abs(count));
            }

            for (int i = 0; i < count; i++) {
                if (!moveBackwardOne()) {
                    return i;
                }
            }
            return count;
        }

        private boolean moveForwardOne() {
            if (next == tail) {
                return false;
            }
            else {
                LinkedRef nextRef = next;
                // detach
                prev.next = nextRef;
                nextRef.prev = prev;

                // insert
                prev = nextRef;
                next = nextRef.next;
                return true;
            }
        }

        @Override
        public int moveForward(int count) {
            if (isRemoved()) {
                throw new IllegalStateException(REMOVED_REF);
            }

            if (count < 0) {
                // Math.abs does not work on Integer.MIN_VALUE
                if (count == Integer.MIN_VALUE) {
                    int moveB1 = moveBackward(Integer.MAX_VALUE);
                    int moveB2 = 0;
                    if (moveB1 == Integer.MAX_VALUE) {
                        moveB2 = moveBackwardOne() ? 1 : 0;
                    }

                    return -moveB1 - moveB2;
                }

                return -moveBackward(Math.abs(count));
            }

            for (int i = 0; i < count; i++) {
                if (!moveForwardOne()) {
                    return i;
                }
            }
            return count;
        }

        @Override
        public LinkedRef addAfter(E newElement) {
            LinkedRef newRef = new LinkedRef(newElement);

            newRef.next = next;
            newRef.prev = this;

            next.prev = newRef;
            next = newRef;

            size++;

            return newRef;
        }

        @Override
        public LinkedRef addBefore(E newElement) {
            LinkedRef newRef = new LinkedRef(newElement);

            newRef.next = this;
            newRef.prev = prev;

            prev.next = newRef;
            prev = newRef;

            size++;

            return newRef;
        }

        private boolean isConsistent() {
            return (next != null && prev != null) || (next == prev);
        }

        @Override
        public boolean isRemoved() {
            assert this != head && this != tail
                    : "isRemoved() is not defined on the head and tail of the list.";

            assert isConsistent()
                    : "Either next and previous element must be null or neither of them.";

            return next == null;
        }

        @Override
        public void remove() {
            assert this != head && this != tail
                    : "The head and the tail of the list cannot be removed.";

            if (!isRemoved()) {
                prev.next = next;
                next.prev = prev;

                prev = null;
                next = null;

                size--;
            }
        }
    }

    private int size;
    private final LinkedRef head;
    private final LinkedRef tail;

    /**
     * Creates an empty list.
     */
    public RefLinkedList() {
        size = 0;
        head = new LinkedRef(null);
        tail = new LinkedRef(null);

        head.prev = null;
        head.next = tail;

        tail.prev = head;
        tail.next = null;
    }

    /**
     * Creates a list containing the elements of the specified collection
     * in the order its iterator returns them.
     *
     * @param collection the elements to be copied into the new list. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified collection is
     *   {@code null}
     */
    public RefLinkedList(Collection<? extends E> collection) {
        ExceptionHelper.checkNotNullArgument(collection, "collection");

        size = 0;
        head = new LinkedRef(null);
        tail = new LinkedRef(null);

        head.prev = null;
        head.next = tail;

        tail.prev = head;
        tail.next = null;

        addAll(collection);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    private ElementRef<E> findRawFirstReference(Object o) {
        for (LinkedRef element = head.next; element != tail; element = element.next) {
            E currentElement = element.element;

            if (o == currentElement || o.equals(currentElement)) {
                return element;
            }
        }

        return null;
    }

    private ElementRef<E> findRawLastReferece(Object o) {
        for (LinkedRef element = tail.prev; element != head; element = element.prev) {
            E currentElement = element.element;

            if (o == currentElement || o.equals(currentElement)) {
                return element;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public ElementRef<E> findFirstReference(E element) {
        return findRawFirstReference(element);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public ElementRef<E> findLastReferece(E element) {
        return findRawLastReferece(element);
    }

    /**
     * Returns the reference to the element equivalent (based on the
     * {@code equals} method) to the given element with the lowest index.
     * This method is equivalent to the
     * {@link #findFirstReference(java.lang.Object) findFirstReference} method.
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     *
     * @param element the reference to the element equivalent (based on the
     *   to the given element with the lowest index or {@code null} if it cannot
     *   be found
     * @return {@inheritDoc }
     */
    @Override
    public ElementRef<E> findReference(E element) {
        return findFirstReference(element);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public ElementRef<E> getFirstReference() {
        LinkedRef result = head.next;
        if (result == tail) {
            result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public ElementRef<E> getLastReference() {
        LinkedRef result = tail.prev;
        if (result == head) {
            result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public ElementRef<E> getReference(int index) {
        return getInternalRef(index);
    }

    private LinkedRef getInternalRef(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " is not within [0, " + (size - 1) + "]");
        }

        LinkedRef result;

        if (index < size / 2) {
            result = head;
            for (int i = 0; i <= index; i++) {
                result = result.next;
            }
        }
        else {
            result = tail;
            for (int i = size; i > index; i--) {
                result = result.prev;
            }
        }

        return result;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public boolean contains(Object o) {
        return findRawFirstReference(o) != null;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public Iterator<E> iterator() {
        return new ReferenceIterator(head.next, 0);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public boolean add(E e) {
        tail.addBefore(e);
        return true;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public boolean remove(Object o) {
        ElementRef<E> ref = findRawFirstReference(o);

        if (ref != null) {
            ref.remove();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in the size
     * of this list.
     */
    @Override
    public void clear() {
        size = 0;

        // We have to remove the references from the list
        // so even if someoneelse calls remove() on them it does not
        // corrupt this list.
        LinkedRef ref = head.next;
        while (ref != tail) {
            LinkedRef nextRef = ref.next;
            ref.next = null;
            ref.prev = null;
            ref = nextRef;
        }

        head.next = tail;
        tail.prev = head;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public E get(int index) {
        return getInternalRef(index).element;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public E set(int index, E element) {
        LinkedRef ref = getInternalRef(index);
        E oldValue = ref.element;
        ref.element = element;

        return oldValue;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public ElementRef<E> addFirstGetReference(E element) {
        return head.addAfter(element);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public ElementRef<E> addLastGetReference(E element) {
        return tail.addBefore(element);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public ElementRef<E> addGetReference(E element) {
        return addLastGetReference(element);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public ElementRef<E> addGetReference(int index, E element) {
        if (index == size) {
            return tail.addBefore(element);
        }
        else {
            return getReference(index).addBefore(element);
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public void add(int index, E element) {
        addGetReference(index, element);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public E remove(int index) {
        LinkedRef ref = getInternalRef(index);
        ref.remove();

        return ref.element;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public ListIterator<E> listIterator() {
        return new ReferenceIterator(head.next, 0);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation requires linear time in {@code index}
     * or {@code size() - index} whichever is lower.
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        return new ReferenceIterator(getInternalRef(index), index);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public void addFirst(E e) {
        head.addAfter(e);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public void addLast(E e) {
        tail.addBefore(e);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public boolean offerFirst(E e) {
        head.addAfter(e);
        return true;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public boolean offerLast(E e) {
        tail.addBefore(e);
        return true;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E removeFirst() {
        LinkedRef first = head.next;
        if (first != tail) {
            first.remove();
            return first.element;
        }
        else {
            throw new NoSuchElementException("The list is empty.");
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E removeLast() {
        LinkedRef last = tail.prev;
        if (last != head) {
            last.remove();
            return last.element;
        }
        else {
            throw new NoSuchElementException("The list is empty.");
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E pollFirst() {
        LinkedRef first = head.next;
        if (first != tail) {
            first.remove();
            return first.element;
        }
        else {
            return null;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E pollLast() {
        LinkedRef last = tail.prev;
        if (last != head) {
            last.remove();
            return last.element;
        }
        else {
            return null;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E getFirst() {
        LinkedRef first = head.next;
        if (first != tail) {
            return first.element;
        }
        else {
            throw new NoSuchElementException("The list is empty.");
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E getLast() {
        LinkedRef last = tail.prev;
        if (last != head) {
            return last.element;
        }
        else {
            throw new NoSuchElementException("The list is empty.");
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E peekFirst() {
        LinkedRef first = head.next;
        if (first != tail) {
            return first.element;
        }
        else {
            return null;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This is constant time operation.
     */
    @Override
    public E peekLast() {
        LinkedRef last = tail.prev;
        if (last != head) {
            return last.element;
        }
        else {
            return null;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public boolean removeLastOccurrence(Object o) {
        ElementRef<E> ref = findRawLastReferece(o);
        if (ref != null) {
            ref.remove();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public boolean offer(E e) {
        add(e);
        return true;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public E remove() {
        return removeFirst();
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public E poll() {
        return pollFirst();
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public E element() {
        return getFirst();
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public E peek() {
        return peekFirst();
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public void push(E e) {
        addFirst(e);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public E pop() {
        return removeFirst();
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This operation may require linear time in the size
     * of this list.
     */
    @Override
    public Iterator<E> descendingIterator() {
        return new DescItr<>(new ReferenceIterator(tail, size - 1));
    }

    private static class DescItr<T> implements Iterator<T> {
        private final ListIterator<T> listItr;

        public DescItr(ListIterator<T> listItr) {
            this.listItr = listItr;
        }

        @Override
        public boolean hasNext() {
            return listItr.hasPrevious();
        }

        @Override
        public T next() {
            return listItr.previous();
        }

        @Override
        public void remove() {
            listItr.remove();
        }
    }

    private class ReferenceIterator implements ListIterator<E> {
        private LinkedRef lastRef;
        private LinkedRef nextRef;
        private int nextIndex;

        public ReferenceIterator(LinkedRef startRef) {
            this.lastRef = null;
            this.nextRef = startRef;
            this.nextIndex = startRef != tail ? startRef.getIndex() : size();
        }

        public ReferenceIterator(LinkedRef startRef, int startIndex) {
            this.lastRef = null;
            this.nextRef = startRef;
            this.nextIndex = startIndex;

            assert (startRef == tail && startIndex == size()) || startIndex == startRef.getIndex();
        }

        @Override
        public boolean hasNext() {
            return nextRef != tail;
        }

        @Override
        public E next() {
            if (nextRef != tail) {
                lastRef = nextRef;
                nextRef = nextRef.next;
                nextIndex++;

                return lastRef.getElement();
            }
            else {
                throw new NoSuchElementException("Last element was reached.");
            }
        }

        @Override
        public boolean hasPrevious() {
            return nextRef.getPrevious() != null;
        }

        @Override
        public E previous() {
            if (hasPrevious()) {
                lastRef = nextRef.prev;
                nextRef = lastRef;
                nextIndex--;

                return lastRef.getElement();
            }
            else {
                throw new NoSuchElementException("First element was reached.");
            }
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            if (lastRef == null) {
                throw new IllegalStateException();
            }

            if (lastRef == nextRef) {
                nextRef = nextRef.prev;
                lastRef.remove();
            }
            else {
                lastRef.remove();
                nextIndex--;
            }

            lastRef = null;
        }

        @Override
        public void set(E e) {
            if (lastRef == null) {
                throw new IllegalStateException();
            }

            lastRef.element = e;
        }

        @Override
        public void add(E e) {
            nextRef.addBefore(e);
        }
    }
}
