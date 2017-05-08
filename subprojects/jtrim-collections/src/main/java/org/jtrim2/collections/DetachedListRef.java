package org.jtrim2.collections;

import java.util.ListIterator;
import org.jtrim2.collections.RefList.ElementRef;

/**
 * @see CollectionsEx#getDetachedListRef(java.lang.Object)
 */
class DetachedListRef<E> implements RefList.ElementRef<E> {
    private static final String NOT_IN_LIST = "The element is not in a list.";

    private E element;

    public DetachedListRef(E element) {
        this.element = element;
    }

    @Override
    public int getIndex() {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public ListIterator<E> getIterator() {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public ElementRef<E> getNext(int offset) {
        return offset == 0 ? this : null;
    }

    @Override
    public ElementRef<E> getPrevious(int offset) {
        return offset == 0 ? this : null;
    }

    @Override
    public void moveLast() {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public void moveFirst() {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public int moveBackward(int count) {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public int moveForward(int count) {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public ElementRef<E> addAfter(E newElement) {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public ElementRef<E> addBefore(E newElement) {
        throw new IllegalStateException(NOT_IN_LIST);
    }

    @Override
    public E setElement(E newElement) {
        E oldElement = element;
        element = newElement;
        return oldElement;
    }

    @Override
    public E getElement() {
        return element;
    }

    @Override
    public boolean isRemoved() {
        return true;
    }

    @Override
    public void remove() {
    }
}
