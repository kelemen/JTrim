package org.jtrim2.collections;

import java.util.Comparator;
import java.util.Objects;

/**
 * @see CollectionsEx#naturalOrder()
 *
 * @author Kelemen Attila
 */
enum NaturalComparator implements Comparator<Comparable<Object>> {
    INSTANCE;

    @Override
    public int compare(Comparable<Object> o1, Comparable<Object> o2) {
        Objects.requireNonNull(o1, "o1");
        Objects.requireNonNull(o2, "o2");

        return o1.compareTo(o2);
    }
}
