package org.jtrim2.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class TestSublistFactory implements TestListFactory<List<Integer>> {
    private final TestListFactory<? extends List<Integer>> wrapped;
    private final int prefixSize;
    private final int suffixSize;

    public TestSublistFactory(TestListFactory<? extends List<Integer>> wrapped, int prefixSize, int suffixSize) {
        Objects.requireNonNull(wrapped, "wrapped");
        this.wrapped = wrapped;
        this.prefixSize = prefixSize;
        this.suffixSize = suffixSize;
    }

    private Integer[] withBorders(Integer[] array) {
        List<Integer> result = new ArrayList<>(prefixSize + suffixSize + array.length);
        for (int i = 0; i < prefixSize; i++) {
            result.add(589);
        }
        result.addAll(Arrays.asList(array));
        for (int i = 0; i < suffixSize; i++) {
            result.add(590);
        }
        return result.toArray(new Integer[result.size()]);
    }

    @Override
    public List<Integer> createList(Integer... content) {
        return wrapped.createList(withBorders(content)).subList(prefixSize, content.length + prefixSize);
    }

    @Override
    public void checkListContent(List<Integer> list, Integer... content) {
        CollectionsExTest.checkListContent(list, content);
    }

    @Override
    public boolean isSublistFactory() {
        return true;
    }

    public static void addSublistFactories(
            Collection<? super TestListFactory<? extends List<Integer>>> result,
            TestListFactory<? extends List<Integer>> factory) {
        for (int subPrefix = 0; subPrefix < 2; subPrefix++) {
            for (int subSuffix = 0; subSuffix < 2; subSuffix++) {
                result.add(new TestSublistFactory(factory, subPrefix, subSuffix));
            }
        }
    }
}
