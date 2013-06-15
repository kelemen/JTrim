package org.jtrim.collections;

import java.util.Objects;

/**
 *
 * @author Kelemen Attila
 */
enum NaturalEquality implements EqualityComparator<Object> {
    INSTANCE;

    @Override
    public boolean equals(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2);
    }
}
