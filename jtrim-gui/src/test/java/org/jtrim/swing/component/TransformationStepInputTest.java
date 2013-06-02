package org.jtrim.swing.component;

import java.util.Arrays;
import org.jtrim.image.ImageResult;
import org.jtrim.image.transform.TransformedImage;
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
public class TransformationStepInputTest {
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

    private static TransformationStepInput create(
            ImageResult source,
            int destinationWidth,
            int destinationHeight,
            TransformedImage inputImage) {
        return new TransformationStepInput(source, destinationWidth, destinationHeight, inputImage);
    }

    @Test
    public void testProperties() {
        TransformedImage inputImage = new TransformedImage(null, null);
        for (ImageResult source: Arrays.asList(null, new ImageResult(null, null))) {
            for (int width: Arrays.asList(0, 1, 100)) {
                for (int height: Arrays.asList(0, 1, 100)) {
                    TransformationStepInput stepInput = create(source, width, height, inputImage);

                    assertSame(source, stepInput.getSource());
                    assertEquals(width, stepInput.getDestinationWidth());
                    assertEquals(height, stepInput.getDestinationHeight());
                    assertSame(inputImage, stepInput.getInputImage());
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalWidth() {
        create(new ImageResult(null, null), -1, 100, new TransformedImage(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalHeight() {
        create(new ImageResult(null, null), 100, -1, new TransformedImage(null, null));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalInputImage() {
        create(new ImageResult(null, null), 100, 100, null);
    }
}
