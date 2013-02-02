package org.jtrim.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
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
public class HierarchicalRightTest {
    private static final AtomicLong CURRENT_RIGHT_INDEX = new AtomicLong(0);

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

    private static HierarchicalRight create(int rightCount) {
        return HierarchicalRight.create(createRightList(rightCount));
    }

    private static Object newRightObject() {
        final String name = "RIGHT-" + CURRENT_RIGHT_INDEX.getAndIncrement();
        return new Object() {
            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static Object[] createRightList(int rightCount) {
        Object[] result = new Object[rightCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = newRightObject();
        }
        return result;
    }

    private static void checkRightList(HierarchicalRight right, Object[] expected) {
        assertArrayEquals(expected, right.getRights().toArray());
    }

    /**
     * Test of create method, of class HierarchicalRight.
     */
    @Test
    public void testCreate() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);

            HierarchicalRight right = HierarchicalRight.create(rightList);
            checkRightList(right, rightList);
        }
    }

    /**
     * Test of createFromList method, of class HierarchicalRight.
     */
    @Test
    public void testCreateFromList() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);

            HierarchicalRight right = HierarchicalRight.createFromList(Arrays.asList(rightList));
            checkRightList(right, rightList);
        }
    }

    /**
     * Test of isUniversal method, of class HierarchicalRight.
     */
    @Test
    public void testIsUniversal() {
        assertTrue(HierarchicalRight.create().isUniversal());
        assertTrue(HierarchicalRight.createFromList(Collections.emptyList()).isUniversal());
        for (int rightCount = 1; rightCount < 5; rightCount++) {
            assertFalse(HierarchicalRight.create(createRightList(rightCount)).isUniversal());
            assertFalse(HierarchicalRight.createFromList(Arrays.asList(createRightList(rightCount))).isUniversal());
        }
    }

    @Test
    public void testIsChildRight() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);
            HierarchicalRight child = HierarchicalRight.create(rightList);

            for (int parentIndex = 0; parentIndex < rightCount; parentIndex++) {
                HierarchicalRight parent = HierarchicalRight.createFromList(Arrays.asList(rightList).subList(0, parentIndex + 1));
                assertTrue(child.isChildRightOf(parent));
                if (parentIndex + 1 != rightCount) {
                    assertFalse(parent.isChildRightOf(child));
                }
            }
        }
    }

    @Test
    public void testIsChildRightWithUnrelated() {
        HierarchicalRight right1 = create(1);
        HierarchicalRight right2 = create(1);
        assertFalse(right1.isChildRightOf(right2));
    }

    @Test
    public void testGetParentRight() {
        for (int rightCount = 1; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);
            HierarchicalRight child = HierarchicalRight.create(rightList);

            for (int parentIndex = 0; parentIndex < rightCount; parentIndex++) {
                Object[] parentList = Arrays.copyOfRange(rightList, 0, rightCount - parentIndex - 1);

                checkRightList(child.getParentRight(parentIndex), parentList);
                if (parentIndex == 0) {
                    checkRightList(child.getParentRight(), parentList);
                }
            }
        }
    }

    @Test
    public void testGetParentRightLargeIndex() {
        for (int rightCount = 1; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);
            HierarchicalRight child = HierarchicalRight.create(rightList);

            assertTrue(child.getParentRight(rightCount).isUniversal());
            assertTrue(child.getParentRight(rightCount + 1).isUniversal());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetParentRightIllegalIndex() {
        create(3).getParentRight(-1);
    }

    /**
     * Test of getChildRight method, of class HierarchicalRight.
     */
    @Test
    public void testGetChildRight() {
        for (int rightCount = 1; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);
            HierarchicalRight right = HierarchicalRight.create(rightList);
            assertSame(rightList[rightList.length - 1], right.getChildRight());
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetChildRightWithUniversal() {
        HierarchicalRight universal = HierarchicalRight.create();
        universal.getChildRight();
    }

    private static Object[] concat(Object[] array1, Object[] array2) {
        Object[] result = new Object[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    /**
     * Test of createSubRight method, of class HierarchicalRight.
     */
    @Test
    public void testCreateSubRight() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);
            HierarchicalRight parent = HierarchicalRight.create(rightList);

            for (int childOffset = 0; childOffset < rightCount; childOffset++) {
                Object[] childAppendList = createRightList(childOffset);
                Object[] childList = concat(rightList, childAppendList);

                HierarchicalRight child = parent.createSubRight(childAppendList);
                checkRightList(child, childList);
            }
        }
    }

    @Test
    public void testCreateSubRightWithGetParent() {
        Object[] childList = createRightList(5);

        HierarchicalRight child = HierarchicalRight.create(childList);
        HierarchicalRight parent = child.getParentRight();
        HierarchicalRight child2 = parent.createSubRight(childList[childList.length - 1]);

        checkRightList(child2, childList);

        Object[] child3List = childList.clone();
        child3List[child3List.length - 1] = newRightObject();
        HierarchicalRight child3 = parent.createSubRight(child3List[child3List.length - 1]);
        checkRightList(child3, child3List);
    }

    @Test
    public void testComparisonEqualList() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            Object[] rightList = createRightList(rightCount);
            HierarchicalRight right1 = HierarchicalRight.create(rightList);
            HierarchicalRight right2 = HierarchicalRight.create(rightList);

            assertEquals(right1, right2);
            assertEquals(right1.hashCode(), right2.hashCode());
        }
    }

    @Test
    public void testComparisonDifferentLengthLists() {
        for (int rightCount = 1; rightCount < 5; rightCount++) {
            Object[] rightList1 = createRightList(rightCount);
            HierarchicalRight right1 = HierarchicalRight.create(rightList1);

            for (int difference = rightCount; difference > 0; difference--) {
                Object[] rightList2 = Arrays.copyOfRange(rightList1, 0, rightCount - difference);
                HierarchicalRight right2 = HierarchicalRight.create(rightList2);

                assertFalse(right1.equals(right2));
                assertFalse(right2.equals(right1));
            }
        }
    }

    @Test
    public void testComparisonDifferentLists() {
        for (int rightCount = 1; rightCount < 5; rightCount++) {
            Object[] rightList1 = createRightList(rightCount);
            HierarchicalRight right1 = HierarchicalRight.create(rightList1);

            for (int diffIndex = 0; diffIndex < rightCount; diffIndex++) {
                Object[] rightList2 = rightList1.clone();
                rightList2[diffIndex] = newRightObject();

                HierarchicalRight right2 = HierarchicalRight.create(rightList2);
                assertFalse(right1.equals(right2));
                assertFalse(right2.equals(right1));
            }
        }
    }

    @Test
    public void testComparisonDifferentClass() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            HierarchicalRight right = create(rightCount);
            assertFalse(right.equals(new Object()));
        }
    }

    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testComparisonToNull() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            HierarchicalRight right = create(rightCount);
            assertFalse(right.equals(null));
        }
    }

    @Test
    public void testComparisonToSelf() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            HierarchicalRight right = create(rightCount);
            assertEquals(right, right);
        }
    }

    /**
     * Test of toString method, of class HierarchicalRight.
     */
    @Test
    public void testToString() {
        for (int rightCount = 0; rightCount < 5; rightCount++) {
            assertNotNull(create(rightCount).toString());
        }
    }
}