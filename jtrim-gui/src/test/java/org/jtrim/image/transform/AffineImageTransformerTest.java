package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.jtrim.image.ImageMetaData;
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
public class AffineImageTransformerTest {
    private static final double DOUBLE_TOLERANCE = 0.000001;

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

    private static void checkCloseEnough(AffineTransform received, double[] expected) {
        double[] actual = new double[6];
        received.getMatrix(actual);
        assertArrayEquals("Expected: " + Arrays.toString(expected) + ". Actual: " + Arrays.toString(actual),
                expected, actual, DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetTransformationMatrix_BasicImageTransformations1() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = AffineImageTransformer.getTransformationMatrix(builder.create());

        double sqrt3 = Math.sqrt(3);
        checkCloseEnough(transf, new double[]{
            -1.5 * sqrt3, 1.5,
            -2.0, -2.0 * sqrt3,
            5.0, 6.0
        });
    }

    @Test
    public void testGetTransformationMatrix_BasicImageTransformations2() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(false);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = AffineImageTransformer.getTransformationMatrix(builder.create());

        double sqrt3 = Math.sqrt(3);
        checkCloseEnough(transf, new double[]{
            1.5 * sqrt3, -1.5,
            -2.0, -2.0 * sqrt3,
            5.0, 6.0
        });
    }

    @Test
    public void testGetTransformationMatrix_BasicImageTransformations3() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(false);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = AffineImageTransformer.getTransformationMatrix(builder.create());

        double sqrt3 = Math.sqrt(3);
        checkCloseEnough(transf, new double[]{
            -1.5 * sqrt3, 1.5,
            2.0, 2.0 * sqrt3,
            5.0, 6.0
        });
    }

    @Test
    public void testGetTransformationMatrix_5args() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = AffineImageTransformer.getTransformationMatrix(
                builder.create(), 20.0, 30.0, 110.0, 120.0);

        double sqrt3 = Math.sqrt(3);
        double[] withoutSourceOffset = new double[]{
            -1.5 * sqrt3, 1.5,
            -2.0, -2.0 * sqrt3,
            (5.0 + (110.0 - 1.0) / 2.0), (6.0 + (120.0 - 1.0) / 2.0)
        };

        double srcOffsetX = -(20.0 - 1.0) / 2.0;
        double srcOffsetY = -(30.0 - 1.0) / 2.0;

        double[] withSourceOffset = withoutSourceOffset.clone();
        withSourceOffset[4] += withoutSourceOffset[0] * srcOffsetX + withoutSourceOffset[2] * srcOffsetY;
        withSourceOffset[5] += withoutSourceOffset[1] * srcOffsetX + withoutSourceOffset[3] * srcOffsetY;

        checkCloseEnough(transf, withSourceOffset);
    }

    @Test
    public void testGetTransformationMatrix_BasicImageTransformations_ImageTransformerData() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        int srcWidth = 20;
        int srcHeight = 30;
        BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
        ImageTransformerData transformerData = new ImageTransformerData(
                srcImage, 110, 120, new ImageMetaData(srcWidth, srcHeight, true));

        AffineTransform transf = AffineImageTransformer.getTransformationMatrix(
                builder.create(), transformerData);

        double sqrt3 = Math.sqrt(3);
        double[] withoutSourceOffset = new double[]{
            -1.5 * sqrt3, 1.5,
            -2.0, -2.0 * sqrt3,
            (5.0 + (110.0 - 1.0) / 2.0), (6.0 + (120.0 - 1.0) / 2.0)
        };

        double srcOffsetX = -(20.0 - 1.0) / 2.0;
        double srcOffsetY = -(30.0 - 1.0) / 2.0;

        double[] withSourceOffset = withoutSourceOffset.clone();
        withSourceOffset[4] += withoutSourceOffset[0] * srcOffsetX + withoutSourceOffset[2] * srcOffsetY;
        withSourceOffset[5] += withoutSourceOffset[1] * srcOffsetX + withoutSourceOffset[3] * srcOffsetY;

        checkCloseEnough(transf, withSourceOffset);
    }

    private static BasicImageTransformations newRotateDegTransformation(int degrees) {
        BasicImageTransformations.Builder result = new BasicImageTransformations.Builder();
        result.setRotateInDegrees(degrees);
        return result.create();
    }

    /**
     * Test of isSimpleTransformation method, of class AffineImageTransformer.
     */
    @Test
    public void testIsSimpleTransformation() {
        assertFalse(AffineImageTransformer.isSimpleTransformation(BasicImageTransformations.newZoomTransformation(2.0, 1.0)));
        assertTrue(AffineImageTransformer.isSimpleTransformation(BasicImageTransformations.newZoomTransformation(-1.0, -1.0)));
        assertTrue(AffineImageTransformer.isSimpleTransformation(BasicImageTransformations.newOffsetTransformation(10.0, 100.0)));
        assertTrue(AffineImageTransformer.isSimpleTransformation(newRotateDegTransformation(0)));
        assertTrue(AffineImageTransformer.isSimpleTransformation(newRotateDegTransformation(90)));
        assertTrue(AffineImageTransformer.isSimpleTransformation(newRotateDegTransformation(180)));
        assertTrue(AffineImageTransformer.isSimpleTransformation(newRotateDegTransformation(270)));
    }

    private static void checkEqualPointTransformers(
            double x,
            double y,
            ImagePointTransformer expected,
            ImagePointTransformer actual) {

        Point2D.Double src = new Point2D.Double(x, y);
        Point2D.Double destActual = new Point2D.Double();
        Point2D.Double destExpected = new Point2D.Double();

        actual.transformSrcToDest(src, destActual);
        expected.transformSrcToDest(src, destExpected);

        assertEquals(destExpected.x, destActual.x, DOUBLE_TOLERANCE);
        assertEquals(destExpected.y, destActual.y, DOUBLE_TOLERANCE);
    }

    private static void checkEqualPointTransformers(
            ImagePointTransformer expected,
            ImagePointTransformer actual) {
        checkEqualPointTransformers(0.0, 0.0, expected, actual);
        checkEqualPointTransformers(-1.0, -1.0, expected, actual);
        checkEqualPointTransformers(1.0, 1.0, expected, actual);
        checkEqualPointTransformers(647.0, 943.0, expected, actual);
        checkEqualPointTransformers(-647.0, -943.0, expected, actual);
    }

    private static void testConvertData(
            AffineImageTransformer transformer,
            ImageTransformerData input,
            ImagePointTransformer expectedPointTransformer) {

        TransformedImage transformedImage = transformer.convertData(input);
        assertEquals(input.getDestWidth(), transformedImage.getImage().getWidth());
        assertEquals(input.getDestHeight(), transformedImage.getImage().getHeight());
        ImagePointTransformer actualPointTransformer = transformedImage.getPointTransformer();

        checkEqualPointTransformers(expectedPointTransformer, actualPointTransformer);
    }

    @Test
    public void testConvertDataNullSourceNullMetaData() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineImageTransformer transformer = new AffineImageTransformer(
                    BasicImageTransformations.newZoomTransformation(100.0, 100.0),
                    Color.GRAY,
                    interpolation);

            ImageTransformerData transfomerData = new ImageTransformerData(null, 100, 200, null);
            TransformedImage transformed = transformer.convertData(transfomerData);
            assertNull(transformed.getImage());
            checkEqualPointTransformers(
                    new AffineImagePointTransformer(new AffineTransform()),
                    transformed.getPointTransformer());
        }
    }

    @Test
    public void testInstance() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);
        BasicImageTransformations transf = builder.create();

        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransform affinTransf = AffineImageTransformer.getTransformationMatrix(transf);

            AffineImageTransformer transformer1 = new AffineImageTransformer(transf, Color.GRAY, interpolation);
            AffineImageTransformer transformer2 = new AffineImageTransformer(affinTransf, Color.GRAY, interpolation);
            // Test that it does not affect transformer2
            affinTransf.translate(1000.0, 1000.0);

            int srcWidth = 20;
            int srcHeight = 30;
            int destWidth = 110;
            int destHeight = 120;
            BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
            ImageTransformerData transformerData = new ImageTransformerData(
                    srcImage, destWidth, destHeight, new ImageMetaData(srcWidth, srcHeight, true));
            ImageTransformerData transformerDataWithoutImage = new ImageTransformerData(
                    null, destWidth, destHeight, new ImageMetaData(srcWidth, srcHeight, true));

            AffineTransform expectedTransf = AffineImageTransformer.getTransformationMatrix(transf, transformerData);
            ImagePointTransformer expectedPointTransf = new AffineImagePointTransformer(expectedTransf);

            testConvertData(transformer1, transformerData, expectedPointTransf);
            testConvertData(transformer2, transformerData, expectedPointTransf);
            checkEqualPointTransformers(expectedPointTransf, transformer1.convertData(transformerDataWithoutImage).getPointTransformer());
            checkEqualPointTransformers(expectedPointTransf, transformer2.convertData(transformerDataWithoutImage).getPointTransformer());

            assertNotNull(transformer1.toString());
            assertNotNull(transformer2.toString());
        }
    }
}