package org.jtrim.collections;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class RandomAccessConcatListViewTest {

    public RandomAccessConcatListViewTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSimpleCreate() {
        ConcatListTestMethods.checkSimpleCreate(RandomListFactory.INSTANCE);
    }

    @Test
    public void testContains() {
        ConcatListTestMethods.checkContains(RandomListFactory.INSTANCE);
    }

    @Test
    public void testGet() {
        ConcatListTestMethods.checkGet(RandomListFactory.INSTANCE);
    }

    @Test
    public void testIndexOf() {
        ConcatListTestMethods.checkIndexOf(RandomListFactory.INSTANCE);
    }

    @Test
    public void testIteratorAfterRemove() {
        ConcatListTestMethods.checkIteratorAfterRemove(RandomListFactory.INSTANCE);
    }

    @Test
    public void testLastIndexOf() {
        ConcatListTestMethods.checkLastIndexOf(RandomListFactory.INSTANCE);
    }

    @Test
    public void testToArray() {
        ConcatListTestMethods.checkToArray(RandomListFactory.INSTANCE);
    }

    private enum RandomListFactory implements ConcatListTestMethods.ListFactory {
        INSTANCE;

        @Override
        public <E> List<E> concatView(List<? extends E> list1, List<? extends E> list2) {
            return new RandomAccessConcatListView<>(list1, list2);
        }
    }
}
