package org.jtrim2.concurrent.async;

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
public class DataWithUidTest {
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
    public void testHashCode() {
        Object dataID = new Object();

        DataWithUid<Object> dataWithUid1 = new DataWithUid<>(new Object(), dataID);
        DataWithUid<Object> dataWithUid2 = new DataWithUid<>(new Object(), dataID);

        assertEquals(dataWithUid1.hashCode(), dataWithUid2.hashCode());
    }

    @Test
    public void testEquals1() {
        Object dataID = new Object();

        DataWithUid<Object> dataWithUid1 = new DataWithUid<>(new Object(), dataID);
        DataWithUid<Object> dataWithUid2 = new DataWithUid<>(new Object(), dataID);

        assertTrue(dataWithUid1.equals(dataWithUid2));
        assertTrue(dataWithUid2.equals(dataWithUid1));
    }

    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals2() {
        DataWithUid<Object> dataWithUid = new DataWithUid<>(new Object(), new Object());

        assertTrue(dataWithUid.equals(dataWithUid));
        assertFalse(dataWithUid.equals(null));
        assertFalse(dataWithUid.equals(new Object()));
    }

    @Test
    public void testEquals3() {
        Object data = new Object();

        DataWithUid<Object> dataWithUid1 = new DataWithUid<>(data, new Object());
        DataWithUid<Object> dataWithUid2 = new DataWithUid<>(data, new Object());

        assertFalse(dataWithUid1.equals(dataWithUid2));
        assertFalse(dataWithUid2.equals(dataWithUid1));
    }

    @Test
    public void testContructorWithoutID() {
        Object data = new Object();

        DataWithUid<Object> dataWithUid = new DataWithUid<>(data);
        assertSame(data, dataWithUid.getData());
        assertSame(data, dataWithUid.getID());
        assertNotNull(dataWithUid.toString());
    }

    @Test
    public void testContructorWithID() {
        Object data = new Object();
        Object dataID = new Object();

        DataWithUid<Object> dataWithUid = new DataWithUid<>(data, dataID);
        assertSame(data, dataWithUid.getData());
        assertSame(dataID, dataWithUid.getID());
        assertNotNull(dataWithUid.toString());
    }
}
