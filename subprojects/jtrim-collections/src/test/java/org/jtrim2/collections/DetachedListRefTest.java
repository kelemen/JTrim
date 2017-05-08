package org.jtrim2.collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DetachedListRefTest {

    public DetachedListRefTest() {
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
     * Test of getIndex method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetIndex() {
        new DetachedListRef<>(null).getIndex();
    }

    /**
     * Test of getIterator method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetIterator() {
        new DetachedListRef<>(null).getIterator();
    }

    /**
     * Test of getNext method, of class DetachedListRef.
     */
    @Test
    public void testGetNext() {
        DetachedListRef<Object> ref = new DetachedListRef<>(null);
        assertSame(ref, ref.getNext(0));
        assertNull(ref.getNext(1));
        assertNull(ref.getNext(100));
    }

    /**
     * Test of getPrevious method, of class DetachedListRef.
     */
    @Test
    public void testGetPrevious() {
        DetachedListRef<Object> ref = new DetachedListRef<>(null);
        assertSame(ref, ref.getPrevious(0));
        assertNull(ref.getPrevious(1));
        assertNull(ref.getPrevious(100));
    }

    /**
     * Test of moveLast method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testMoveLast() {
        new DetachedListRef<>(null).moveLast();
    }

    /**
     * Test of moveFirst method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testMoveFirst() {
        new DetachedListRef<>(null).moveFirst();
    }

    /**
     * Test of moveBackward method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testMoveBackward() {
        new DetachedListRef<>(null).moveBackward(0);
    }

    /**
     * Test of moveForward method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testMoveForward() {
        new DetachedListRef<>(null).moveForward(0);
    }

    /**
     * Test of addAfter method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testAddAfter() {
        new DetachedListRef<>(null).addAfter(null);
    }

    /**
     * Test of addBefore method, of class DetachedListRef.
     */
    @Test(expected = IllegalStateException.class)
    public void testAddBefore() {
        new DetachedListRef<>(null).addBefore(null);
    }

    /**
     * Test of setElement and getElement methods, of class DetachedListRef.
     */
    @Test
    public void testSetAndGetElement() {
        DetachedListRef<Boolean> ref = new DetachedListRef<>(false);

        assertFalse(ref.getElement());
        assertFalse(ref.setElement(true));
        assertTrue(ref.getElement());
    }

    /**
     * Test of isRemoved method, of class DetachedListRef.
     */
    @Test
    public void testIsRemoved() {
        assertTrue(new DetachedListRef<>(null).isRemoved());
    }

    /**
     * Test of remove method, of class DetachedListRef.
     */
    @Test
    public void testRemove() {
        new DetachedListRef<>(null).remove();
    }
}
