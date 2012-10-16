package org.jtrim.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jtrim.collections.RefList.ElementRef;
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
public class CollectionsExTest {

    public CollectionsExTest() {
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
     * Test of newHashMap method, of class CollectionsEx.
     */
    @Test
    public void testNewHashMap1() {
        // We can only test that it does not fail miserably.
        CollectionsEx.newHashMap(0);
        CollectionsEx.newHashMap(1);
        CollectionsEx.newHashMap(26);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashMap1Error() {
        CollectionsEx.newHashMap(-1);
    }

    /**
     * Test of newHashMap method, of class CollectionsEx.
     */
    @Test
    public void testNewHashMap2() {
        // We can only test that it does not fail miserably.
        CollectionsEx.newHashMap(0, 0.50f);
        CollectionsEx.newHashMap(1, 0.75f);
        CollectionsEx.newHashMap(26, 100.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashMap2Error1() {
        CollectionsEx.newHashMap(1, 0.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashMap2Error2() {
        CollectionsEx.newHashMap(1, -0.25f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashMap2Error3() {
        CollectionsEx.newHashMap(1, Float.NaN);
    }

    /**
     * Test of newHashSet method, of class CollectionsEx.
     */
    @Test
    public void testNewHashSet1() {
        // We can only test that it does not fail miserably.
        CollectionsEx.newHashSet(0);
        CollectionsEx.newHashSet(1);
        CollectionsEx.newHashSet(26);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashSet1Error() {
        CollectionsEx.newHashSet(-1);
    }

    /**
     * Test of newHashSet method, of class CollectionsEx.
     */
    @Test
    public void testNewHashSet2() {
        // We can only test that it does not fail miserably.
        CollectionsEx.newHashSet(0, 0.50f);
        CollectionsEx.newHashSet(1, 0.75f);
        CollectionsEx.newHashSet(26, 100.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashSet2Error1() {
        CollectionsEx.newHashSet(1, 0.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashSet2Error2() {
        CollectionsEx.newHashSet(1, -0.25f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHashSet2Error3() {
        CollectionsEx.newHashSet(1, Float.NaN);
    }

    /**
     * Test of newIdentityHashSet method, of class CollectionsEx.
     */
    @Test
    public void testNewIdentityHashSet() {
        // We can only test that it does not fail miserably.
        CollectionsEx.newIdentityHashSet(0);
        CollectionsEx.newIdentityHashSet(1);
        CollectionsEx.newIdentityHashSet(26);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewIdentityHashSetError() {
        CollectionsEx.newIdentityHashSet(-1);
    }

    private static void expect(Class<? extends Throwable> exception, Runnable task) {
        assert exception != null;
        assert task != null;

        try {
            task.run();
        } catch (Throwable thrown) {
            assertTrue("Expected exception: " + exception.getName() + " but received: " + thrown.getClass().getName(),
                    exception.isAssignableFrom(thrown.getClass()));
            return;
        }
        fail("Expected exception: " + exception.getName());
    }

    private void checkIfReadOnly(final List<Integer> list) {
        expect(UnsupportedOperationException.class, new Runnable() {
            @Override
            public void run() {
                list.add(5);
            }
        });
        expect(UnsupportedOperationException.class, new Runnable() {
            @Override
            public void run() {
                list.add(0, 5);
            }
        });
        expect(UnsupportedOperationException.class, new Runnable() {
            @Override
            public void run() {
                list.addAll(Arrays.asList(5));
            }
        });
        expect(UnsupportedOperationException.class, new Runnable() {
            @Override
            public void run() {
                list.addAll(0, Arrays.asList(5));
            }
        });
        if (!list.isEmpty()) {
            // Collections.emptyList().retainAll(?) does not throw an exception
            expect(UnsupportedOperationException.class, new Runnable() {
                @Override
                public void run() {
                    list.retainAll(Arrays.asList(5));
                }
            });
            // Collections.emptyList().removeAll(?) does not throw an exception
            expect(UnsupportedOperationException.class, new Runnable() {
                @Override
                public void run() {
                    list.removeAll(Arrays.asList(5));
                }
            });
            // Collections.emptyList().remove(?) does not throw an exception
            expect(UnsupportedOperationException.class, new Runnable() {
                @Override
                public void run() {
                    list.remove(Integer.valueOf(10));
                }
            });
            // Collections.emptyList().clear() does not throw an exception
            expect(UnsupportedOperationException.class, new Runnable() {
                @Override
                public void run() {
                    list.clear();
                }
            });
            expect(UnsupportedOperationException.class, new Runnable() {
                @Override
                public void run() {
                    list.set(0, 5);
                }
            });
            expect(UnsupportedOperationException.class, new Runnable() {
                @Override
                public void run() {
                    list.remove(0);
                }
            });
        }
    }

    /**
     * Test of readOnlyCopy method, of class CollectionsEx.
     */
    @Test
    public void testReadOnlyCopy() {
        int[] expected = new int[]{10, 11, 12};
        final List<Integer> list = CollectionsEx.readOnlyCopy(createLinearList(expected));
        checkListContent(list, expected);
        checkIfReadOnly(list);

        checkIfReadOnly(CollectionsEx.readOnlyCopy(new LinkedList<Integer>()));
    }

    private static List<Integer> createLinearList(int... content) {
        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < content.length; i++) {
            result.add(content[i]);
        }
        return result;
    }

    private static List<Integer> createRandomList(int... content) {
        List<Integer> result = new ArrayList<>(content.length);
        for (int i = 0; i < content.length; i++) {
            result.add(content[i]);
        }
        return result;
    }

    private static void checkListContent(List<Integer> list, int... content) {
        Iterator<Integer> itr = list.iterator();
        for (int i = 0; i < content.length; i++) {
            assertTrue(itr.hasNext());
            assertEquals(content[i], itr.next().intValue());
        }
        assertFalse(itr.hasNext());
    }

    /**
     * Test of viewConcatList method, of class CollectionsEx.
     */
    @Test
    public void testViewConcatList() {
        List<Integer> randomList = CollectionsEx.viewConcatList(createRandomList(11, 12, 13), createRandomList(14, 15));
        assertTrue(randomList instanceof RandomAccessConcatListView);
        checkListContent(randomList, 11, 12, 13, 14, 15);

        List<Integer> list = CollectionsEx.viewConcatList(createLinearList(11, 12, 13), createLinearList(14, 15));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15);

        list = CollectionsEx.viewConcatList(createLinearList(11, 12, 13), createRandomList(14, 15));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15);

        list = CollectionsEx.viewConcatList(createRandomList(11, 12, 13), createLinearList(14, 15));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15);

        list = CollectionsEx.viewConcatList(randomList, createRandomList(16, 17));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15, 16, 17);

        list = CollectionsEx.viewConcatList(createRandomList(10), randomList);
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 10, 11, 12, 13, 14, 15);
    }

    /**
     * Test of naturalOrder method, of class CollectionsEx.
     */
    @Test
    public void testNaturalOrder() {
        assertSame(NaturalComparator.INSTANCE, CollectionsEx.naturalOrder());
    }

    /**
     * Test of getDetachedListRef method, of class CollectionsEx.
     */
    @Test
    public void testGetDetachedListRef() {
        ElementRef<Integer> ref = CollectionsEx.getDetachedListRef(5);
        assertTrue(ref instanceof DetachedListRef);
        assertEquals(5, ref.getElement().intValue());
    }
}
