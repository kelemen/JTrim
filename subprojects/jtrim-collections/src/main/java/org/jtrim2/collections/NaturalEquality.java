package org.jtrim2.collections;

import java.util.Objects;

/**
 * @see Comparators#naturalEquality()
 */
enum NaturalEquality implements EqualityComparator<Object> {
    INSTANCE;

    @Override
    public boolean equals(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2);
    }
}
