package org.jtrim.collections;

import java.util.Comparator;
import java.util.List;
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
public class ArraysExTest {
    public ArraysExTest() {
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
     * Test of viewAsList method, of class ArraysEx.
     */
    @Test
    public void testViewAsListSubArray() {
        int offset = 2;
        int count = 3;
        Integer[] array = new Integer[]{10, 11, 12, 13, 14, 15, 16};

        List<Integer> view = ArraysEx.viewAsList(array, offset, count);
        assertTrue(view instanceof ArrayView);
        assertEquals(count, view.size());
        for (int i = 0; i < count; i++) {
            assertSame(array[offset + i], view.get(i));
        }
        CollectionsExTest.checkIfReadOnly(view);
    }

    /**
     * Test of viewAsList method, of class ArraysEx.
     */
    @Test
    public void testViewAsListArray() {
        Integer[] array = new Integer[]{10, 11, 12, 13, 14, 15, 16};

        List<Integer> view = ArraysEx.viewAsList(array);
        assertTrue(view instanceof ArrayView);
        assertEquals(array.length, view.size());
        for (int i = 0; i < array.length; i++) {
            assertSame(array[i], view.get(i));
        }
        CollectionsExTest.checkIfReadOnly(view);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_byteArr() {
        assertEquals(15, ArraysEx.findMax(new byte[]{15, 13, 12, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new byte[]{12, 13, 15, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new byte[]{12, 13, 12, 14, 15}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_byteArrEmpty() {
        ArraysEx.findMax(new byte[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_charArr() {
        assertEquals(15, ArraysEx.findMax(new char[]{15, 13, 12, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new char[]{12, 13, 15, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new char[]{12, 13, 12, 14, 15}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_charArrEmpty() {
        ArraysEx.findMax(new char[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_shortArr() {
        assertEquals(15, ArraysEx.findMax(new short[]{15, 13, 12, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new short[]{12, 13, 15, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new short[]{12, 13, 12, 14, 15}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_shortArrEmpty() {
        ArraysEx.findMax(new short[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_intArr() {
        assertEquals(15, ArraysEx.findMax(new int[]{15, 13, 12, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new int[]{12, 13, 15, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new int[]{12, 13, 12, 14, 15}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_intArrEmpty() {
        ArraysEx.findMax(new int[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_longArr() {
        assertEquals(15, ArraysEx.findMax(new long[]{15, 13, 12, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new long[]{12, 13, 15, 14, 7}));
        assertEquals(15, ArraysEx.findMax(new long[]{12, 13, 12, 14, 15}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_longArrEmpty() {
        ArraysEx.findMax(new long[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_floatArr() {
        assertEquals(15, ArraysEx.findMax(new float[]{15, 13, 12, 14, 7}), 0.0);
        assertEquals(15, ArraysEx.findMax(new float[]{12, 13, 15, 14, 7}), 0.0);
        assertEquals(15, ArraysEx.findMax(new float[]{12, 13, 12, 14, 15}), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_floatArrEmpty() {
        ArraysEx.findMax(new float[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_doubleArr() {
        assertEquals(15, ArraysEx.findMax(new double[]{15, 13, 12, 14, 7}), 0.0);
        assertEquals(15, ArraysEx.findMax(new double[]{12, 13, 15, 14, 7}), 0.0);
        assertEquals(15, ArraysEx.findMax(new double[]{12, 13, 12, 14, 15}), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_doubleArrEmpty() {
        ArraysEx.findMax(new double[0]);
    }


    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_GenericType_Comparator() {
        IntWrapper[] array;

        array = new IntWrapper[]{new IntWrapper(15), new IntWrapper(13), new IntWrapper(12), new IntWrapper(14), new IntWrapper(7)};
        assertEquals(15, ArraysEx.findMax(array, IntWrapperCmp.INSTANCE).value);

        array = new IntWrapper[]{new IntWrapper(12), new IntWrapper(13), new IntWrapper(12), new IntWrapper(15), new IntWrapper(7)};
        assertEquals(15, ArraysEx.findMax(array, IntWrapperCmp.INSTANCE).value);

        array = new IntWrapper[]{new IntWrapper(12), new IntWrapper(13), new IntWrapper(12), new IntWrapper(14), new IntWrapper(15)};
        assertEquals(15, ArraysEx.findMax(array, IntWrapperCmp.INSTANCE).value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_GenericType_ComparatorEmpty() {
        ArraysEx.findMax(new IntWrapper[0], IntWrapperCmp.INSTANCE);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMax_GenericType() {
        assertEquals(15, (int)ArraysEx.findMax(new Integer[]{15, 13, 12, 14, 7}));
        assertEquals(15, (int)ArraysEx.findMax(new Integer[]{12, 13, 15, 14, 7}));
        assertEquals(15, (int)ArraysEx.findMax(new Integer[]{12, 13, 12, 14, 15}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMax_GenericTypeEmpty() {
        ArraysEx.findMax(new String[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_byteArr() {
        assertEquals(5, ArraysEx.findMin(new byte[]{5, 13, 12, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new byte[]{12, 13, 5, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new byte[]{12, 13, 12, 14, 5}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_byteArrEmpty() {
        ArraysEx.findMin(new byte[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_charArr() {
        assertEquals(5, ArraysEx.findMin(new char[]{5, 13, 12, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new char[]{12, 13, 5, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new char[]{12, 13, 12, 14, 5}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_charArrEmpty() {
        ArraysEx.findMin(new char[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_shortArr() {
        assertEquals(5, ArraysEx.findMin(new short[]{5, 13, 12, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new short[]{12, 13, 5, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new short[]{12, 13, 12, 14, 5}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_shortArrEmpty() {
        ArraysEx.findMin(new short[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_intArr() {
        assertEquals(5, ArraysEx.findMin(new int[]{5, 13, 12, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new int[]{12, 13, 5, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new int[]{12, 13, 12, 14, 5}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_intArrEmpty() {
        ArraysEx.findMax(new int[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_longArr() {
        assertEquals(5, ArraysEx.findMin(new long[]{5, 13, 12, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new long[]{12, 13, 5, 14, 7}));
        assertEquals(5, ArraysEx.findMin(new long[]{12, 13, 12, 14, 5}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_longArrEmpty() {
        ArraysEx.findMin(new long[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_floatArr() {
        assertEquals(5, ArraysEx.findMin(new float[]{5, 13, 12, 14, 7}), 0.0);
        assertEquals(5, ArraysEx.findMin(new float[]{12, 13, 5, 14, 7}), 0.0);
        assertEquals(5, ArraysEx.findMin(new float[]{12, 13, 12, 14, 5}), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_floatArrEmpty() {
        ArraysEx.findMin(new float[0]);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_doubleArr() {
        assertEquals(5, ArraysEx.findMin(new double[]{5, 13, 12, 14, 7}), 0.0);
        assertEquals(5, ArraysEx.findMin(new double[]{12, 13, 5, 14, 7}), 0.0);
        assertEquals(5, ArraysEx.findMin(new double[]{12, 13, 12, 14, 5}), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_doubleArrEmpty() {
        ArraysEx.findMin(new double[0]);
    }


    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_GenericType_Comparator() {
        IntWrapper[] array;

        array = new IntWrapper[]{new IntWrapper(5), new IntWrapper(13), new IntWrapper(12), new IntWrapper(14), new IntWrapper(7)};
        assertEquals(5, ArraysEx.findMin(array, IntWrapperCmp.INSTANCE).value);

        array = new IntWrapper[]{new IntWrapper(12), new IntWrapper(13), new IntWrapper(12), new IntWrapper(5), new IntWrapper(7)};
        assertEquals(5, ArraysEx.findMin(array, IntWrapperCmp.INSTANCE).value);

        array = new IntWrapper[]{new IntWrapper(12), new IntWrapper(13), new IntWrapper(12), new IntWrapper(14), new IntWrapper(5)};
        assertEquals(5, ArraysEx.findMin(array, IntWrapperCmp.INSTANCE).value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_GenericType_ComparatorEmpty() {
        ArraysEx.findMin(new IntWrapper[0], IntWrapperCmp.INSTANCE);
    }

    /**
     * Test of findMax method, of class ArraysEx.
     */
    @Test
    public void testFindMin_GenericType() {
        assertEquals(5, (int)ArraysEx.findMin(new Integer[]{5, 13, 12, 14, 7}));
        assertEquals(5, (int)ArraysEx.findMin(new Integer[]{12, 13, 5, 14, 7}));
        assertEquals(5, (int)ArraysEx.findMin(new Integer[]{12, 13, 12, 14, 5}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindMin_GenericTypeEmpty() {
        ArraysEx.findMin(new String[0]);
    }

    private static final class IntWrapper {
        public final int value;

        public IntWrapper(int value) {
            this.value = value;
        }
    }

    private enum IntWrapperCmp implements Comparator<IntWrapper> {
        INSTANCE;

        @Override
        public int compare(IntWrapper o1, IntWrapper o2) {
            return Integer.compare(o1.value, o2.value);
        }
    }
}
