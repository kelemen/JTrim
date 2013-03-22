package org.jtrim.collections;

import java.util.List;
import java.util.NoSuchElementException;
import org.jtrim.collections.ListTestMethods.SublistFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class ArrayViewTest {
    @BeforeClass
    public static void setUpClass() throws Throwable {
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
    }

    @Before
    public void setUp() throws Throwable {
    }

    @After
    public void tearDown() throws Throwable {
    }

    private static void execute(String methodName) throws Throwable {
        for (int prefixSize = 0; prefixSize < 2; prefixSize++) {
            for (int suffixSize = 0; suffixSize < 2; suffixSize++) {
                ViewListFactory factory = new ViewListFactory(prefixSize, suffixSize);

                ListTestMethods.executeTest(methodName, factory);
                for (int subPrefix = 0; subPrefix < 2; subPrefix++) {
                    for (int subSuffix = 0; subSuffix < 2; subSuffix++) {
                        ListTestMethods.executeTest(methodName, new SublistFactory(factory, subPrefix, subSuffix));
                    }
                }
            }
        }
    }

    @Test
    public void testSerialize() throws Throwable {
        execute("testSerialize");
    }

    @Test
    public void testSize() throws Throwable {
        execute("testSize");
    }

    @Test
    public void testIsEmpty() throws Throwable  {
        execute("testIsEmpty");
    }

    @Test
    public void testContains() throws Throwable {
        execute("testContains");
    }

    @Test
    public void testIterator() throws Throwable {
        execute("testIterator");
    }

    @Test
    public void testAddAndGetAtIndex() throws Throwable {
        execute("testAddAndGetAtIndex");
    }

    @Test(expected = NoSuchElementException.class)
    public void testListIteratorTooManyNext() throws Throwable {
        execute("testListIteratorTooManyNext");
    }

    @Test(expected = NoSuchElementException.class)
    public void testListIteratorTooManyPrevious() throws Throwable {
        execute("testListIteratorTooManyPrevious");
    }

    @Test
    public void testListIteratorRead() throws Throwable {
        execute("testListIteratorRead");
    }

    @Test
    public void testListIteratorFromIndex() throws Throwable {
        execute("testListIteratorFromIndex");
    }

    @Test
    public void testListIteratorFromIndex0() throws Throwable {
        execute("testListIteratorFromIndex0");
    }

    @Test
    public void testListIteratorFromEnd() throws Throwable {
        execute("testListIteratorFromEnd");
    }

    @Test
    public void testIndexOf() throws Throwable {
        execute("testIndexOf");
    }

    @Test
    public void testIndexOfNulls() throws Throwable {
        execute("testIndexOfNulls");
    }

    @Test
    public void testLastIndexOf() throws Throwable {
        execute("testLastIndexOf");
    }

    @Test
    public void testLastIndexOfNulls() throws Throwable {
        execute("testLastIndexOfNulls");
    }

    @Test
    public void testToArray() throws Throwable {
        execute("testToArray");
    }

    @Test
    public void testToProvidedArray() throws Throwable {
        execute("testToProvidedArray");
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

    private static class ViewListFactory implements ListTestMethods.ListFactory<List<Integer>> {
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
    }
}
