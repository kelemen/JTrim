package org.jtrim2.collections;

import java.util.List;

public interface TestListFactory<ListType extends List<Integer>> {
    public ListType createList(Integer... content);
    public void checkListContent(ListType list, Integer... content);

    public boolean isSublistFactory();

    public default ListType createListOfSize(int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        return createList(array);
    }
}
