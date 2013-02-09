package org.jtrim.collections;

import java.util.Comparator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class NaturalComparatorTest {

    public NaturalComparatorTest() {
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

    /**
     * Test of values method, of class NaturalComparator.
     */
    @Test
    public void testValues() {
        assertEquals(1, NaturalComparator.values().length);
    }

    /**
     * Test of valueOf method, of class NaturalComparator.
     */
    @Test
    public void testValueOf() {
        assertSame(NaturalComparator.INSTANCE, NaturalComparator.valueOf(NaturalComparator.INSTANCE.toString()));
    }

    /**
     * Test of compare method, of class NaturalComparator.
     */
    @Test
    public void testCompare() {
        MyObj obj1 = new MyObj(5);
        MyObj obj2 = new MyObj(2);

        assertEquals(obj1.compareTo(obj2), NaturalComparator.INSTANCE.compare(obj1, obj2));
        assertEquals(obj2.compareTo(obj1), NaturalComparator.INSTANCE.compare(obj2, obj1));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareNull1() {
        NaturalComparator.INSTANCE.compare(null, new MyObj(100));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareNull2() {
        NaturalComparator.INSTANCE.compare(new MyObj(100), null);
    }

    private static final class MyObj implements Comparable<Object> {
        private final int value;

        public MyObj(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(Object o) {
            return value - ((MyObj)o).value;
        }
    }
}
