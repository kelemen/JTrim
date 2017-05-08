package org.jtrim2.image.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class IIOReadProgressAdapterTest {
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
     * Test of sequenceStarted method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testSequenceStarted() {
        new IIOReadProgressAdapter().sequenceStarted(null, 0);
    }

    /**
     * Test of sequenceComplete method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testSequenceComplete() {
        new IIOReadProgressAdapter().sequenceComplete(null);
    }

    /**
     * Test of imageStarted method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testImageStarted() {
        new IIOReadProgressAdapter().imageStarted(null, 0);
    }

    /**
     * Test of imageProgress method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testImageProgress() {
        new IIOReadProgressAdapter().imageProgress(null, 0.0f);
    }

    /**
     * Test of imageComplete method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testImageComplete() {
        new IIOReadProgressAdapter().imageComplete(null);
    }

    /**
     * Test of thumbnailStarted method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testThumbnailStarted() {
        new IIOReadProgressAdapter().thumbnailStarted(null, 0, 1);
    }

    /**
     * Test of thumbnailProgress method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testThumbnailProgress() {
        new IIOReadProgressAdapter().thumbnailProgress(null, 0.0f);
    }

    /**
     * Test of thumbnailComplete method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testThumbnailComplete() {
        new IIOReadProgressAdapter().thumbnailComplete(null);
    }

    /**
     * Test of readAborted method, of class IIOReadProgressAdapter.
     */
    @Test
    public void testReadAborted() {
        new IIOReadProgressAdapter().readAborted(null);
    }
}
