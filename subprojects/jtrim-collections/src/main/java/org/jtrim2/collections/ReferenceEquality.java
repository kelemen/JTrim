package org.jtrim2.collections;

/**
 * @see Comparators#referenceEquality()
 */
enum ReferenceEquality implements EqualityComparator<Object> {
    INSTANCE;

    @Override
    public boolean equals(Object obj1, Object obj2) {
        return obj1 == obj2;
    }
}
