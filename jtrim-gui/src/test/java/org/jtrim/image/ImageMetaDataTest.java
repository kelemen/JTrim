package org.jtrim.image;

import java.util.Arrays;
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
public class ImageMetaDataTest {
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

    private static ImageMetaData create(int width, int height, boolean complete) {
        return new ImageMetaData(width, height, complete);
    }

    @Test
    public void testProperties() {
        for (int width: Arrays.asList(0, 1, 56)) {
            for (int height: Arrays.asList(0, 1, 37)) {
                for (boolean complete: Arrays.asList(false, true)) {
                    ImageMetaData metaData = create(width, height, complete);
                    assertEquals(width, metaData.getWidth());
                    assertEquals(height, metaData.getHeight());
                    assertEquals(complete, metaData.isComplete());
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor1() {
        create(-1, 10, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor2() {
        create(10, -1, true);
    }
}