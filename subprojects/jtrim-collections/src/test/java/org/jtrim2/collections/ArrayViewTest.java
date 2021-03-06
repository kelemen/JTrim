package org.jtrim2.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ArrayViewTest {
    public static class ReadTests extends ReadableListTests {
        public ReadTests() {
            super(getFactories());
        }
    }

    private static Collection<TestListFactory<? extends List<Integer>>> getFactories() {
        Collection<TestListFactory<? extends List<Integer>>> result = new ArrayList<>();

        for (int prefixSize = 0; prefixSize < 2; prefixSize++) {
            for (int suffixSize = 0; suffixSize < 2; suffixSize++) {
                ViewListFactory factory = new ViewListFactory(prefixSize, suffixSize);

                result.add(factory);
                TestSublistFactory.addSublistFactories(result, factory);
            }
        }

        return result;
    }

    @Test
    public void testReadOnly() {
        int offset = 2;
        int count = 3;
        Integer[] array = new Integer[]{10, 11, 12, 13, 14, 15, 16};

        List<Integer> view = new ArrayView<>(array, offset, count);
        CollectionsExTest.checkIfReadOnly(view);
    }

    private static ArrayView<Integer> create(Integer[] array, int offset, int length) {
        return new ArrayView<>(array, offset, length);
    }

    @Test
    public void testEndOfArray() {
        Assert.assertEquals(0, create(new Integer[5], 5, 0).size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLengthNegative() {
        create(new Integer[5], 3, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOffsetNegative() {
        create(new Integer[5], -1, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testWrongSlice1() {
        create(new Integer[5], 4, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testWrongSlice2() {
        create(new Integer[5], 5, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testWrongSlice3() {
        create(new Integer[5], 6, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullArray() {
        create(null, 0, 0);
    }

    private static class ViewListFactory implements TestListFactory<List<Integer>> {
        private final int prefixSize;
        private final int suffixSize;

        public ViewListFactory(int prefixSize, int suffixSize) {
            this.prefixSize = prefixSize;
            this.suffixSize = suffixSize;
        }

        @Override
        public List<Integer> createList(Integer... content) {
            Integer[] array = new Integer[content.length + prefixSize + suffixSize];
            for (int i = 0; i < prefixSize; i++) {
                array[i] = -(i + 1);
            }

            System.arraycopy(content, 0, array, prefixSize, content.length);

            for (int i = 0; i < suffixSize; i++) {
                array[content.length + prefixSize + i] = -(i + 1);
            }
            return new ArrayView<>(array, prefixSize, content.length);
        }

        @Override
        public void checkListContent(List<Integer> list, Integer... content) {
            CollectionsExTest.checkListContent(list, content);
        }

        @Override
        public boolean isSublistFactory() {
            return false;
        }
    }
}
