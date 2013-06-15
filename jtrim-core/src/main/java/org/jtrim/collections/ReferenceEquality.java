package org.jtrim.collections;

/**
 *
 * @author Kelemen Attila
 */
enum ReferenceEquality implements EqualityComparator<Object> {
    INSTANCE;

    @Override
    public boolean equals(Object obj1, Object obj2) {
        return obj1 == obj2;
    }
}
