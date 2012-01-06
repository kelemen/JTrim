package org.jtrim.collections;

import java.util.*;

import org.jtrim.utils.*;

/**
 * @see CollectionsEx#naturalOrder()
 *
 * @author Kelemen Attila
 */
enum NaturalComparator implements Comparator<Comparable<Object>> {
    INSTANCE;

    @Override
    public int compare(Comparable<Object> o1, Comparable<Object> o2) {
        ExceptionHelper.checkNotNullArgument(o1, "o1");
        ExceptionHelper.checkNotNullArgument(o2, "o2");

        return o1.compareTo(o2);
    }
}
