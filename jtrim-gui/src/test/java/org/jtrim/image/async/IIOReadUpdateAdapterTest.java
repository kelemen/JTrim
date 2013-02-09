package org.jtrim.image.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class IIOReadUpdateAdapterTest {
    // Just for the sake of completness and to please code coverage.

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
     * Test of passStarted method, of class IIOReadUpdateAdapter.
     */
    @Test
    public void testPassStarted() {
        new IIOReadUpdateAdapter().passComplete(null, null);
    }

    /**
     * Test of imageUpdate method, of class IIOReadUpdateAdapter.
     */
    @Test
    public void testImageUpdate() {
        new IIOReadUpdateAdapter().imageUpdate(null, null, 0, 0, 0, 0, 0, 0, null);
    }

    /**
     * Test of passComplete method, of class IIOReadUpdateAdapter.
     */
    @Test
    public void testPassComplete() {
        new IIOReadUpdateAdapter().passComplete(null, null);
    }

    /**
     * Test of thumbnailPassStarted method, of class IIOReadUpdateAdapter.
     */
    @Test
    public void testThumbnailPassStarted() {
        new IIOReadUpdateAdapter().thumbnailPassStarted(null, null, 0, 0, 0, 0, 0, 0, 0, null);
    }

    /**
     * Test of thumbnailUpdate method, of class IIOReadUpdateAdapter.
     */
    @Test
    public void testThumbnailUpdate() {
        new IIOReadUpdateAdapter().thumbnailUpdate(null, null, 0, 0, 0, 0, 0, 0, null);
    }

    /**
     * Test of thumbnailPassComplete method, of class IIOReadUpdateAdapter.
     */
    @Test
    public void testThumbnailPassComplete() {
        new IIOReadUpdateAdapter().thumbnailPassComplete(null, null);
    }
}