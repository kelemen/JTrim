package org.jtrim.image.transform;

import java.awt.geom.AffineTransform;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * These tests test the common static methods of {@link AffineTransformationStep}
 * and {@link AffineImageTransformer}. The common methods are needed because
 * {@code AffineImageTransformer} is deprecated but static methods must be kept
 * for backward compatibility.
 *
 * @author Kelemen Attila
 */
final class CommonAffineTransformationsTests {
    private static final double DOUBLE_TOLERANCE = 0.000001;

    public static void testCommonTransformations(CommonAffineTransformations testedCode) {
        testGetTransformationMatrix_5args(testedCode);
        testGetTransformationMatrix_BasicImageTransformations1(testedCode);
        testGetTransformationMatrix_BasicImageTransformations2(testedCode);
        testGetTransformationMatrix_BasicImageTransformations3(testedCode);
        testIsSimpleTransformation(testedCode);
    }

    public static void checkCloseEnough(AffineTransform received, double[] expected) {
        double[] actual = new double[6];
        received.getMatrix(actual);
        assertArrayEquals("Expected: " + Arrays.toString(expected) + ". Actual: " + Arrays.toString(actual),
                expected, actual, DOUBLE_TOLERANCE);
    }

    private static void testGetTransformationMatrix_BasicImageTransformations1(
            CommonAffineTransformations testedCode) {

        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(-Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = testedCode.getTransformationMatrix(builder.create());

        double sqrt3 = Math.sqrt(3);
        checkCloseEnough(transf, new double[]{
            -1.5 * sqrt3, 1.5,
            -2.0, -2.0 * sqrt3,
            5.0, 6.0
        });
    }

    private static void testGetTransformationMatrix_BasicImageTransformations2(
            CommonAffineTransformations testedCode) {

        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(false);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(-Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = testedCode.getTransformationMatrix(builder.create());

        double sqrt3 = Math.sqrt(3);
        checkCloseEnough(transf, new double[]{
            1.5 * sqrt3, -1.5,
            -2.0, -2.0 * sqrt3,
            5.0, 6.0
        });
    }

    private static void testGetTransformationMatrix_BasicImageTransformations3(
            CommonAffineTransformations testedCode) {

        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(false);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(-Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = testedCode.getTransformationMatrix(builder.create());

        double sqrt3 = Math.sqrt(3);
        checkCloseEnough(transf, new double[]{
            -1.5 * sqrt3, 1.5,
            2.0, 2.0 * sqrt3,
            5.0, 6.0
        });
    }

    private static void testGetTransformationMatrix_5args(CommonAffineTransformations testedCode) {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(-Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        AffineTransform transf = testedCode.getTransformationMatrix(
                builder.create(), 20.0, 30.0, 110.0, 120.0);

        double sqrt3 = Math.sqrt(3);
        double[] withoutSourceOffset = new double[]{
            -1.5 * sqrt3, 1.5,
            -2.0, -2.0 * sqrt3,
            (5.0 + 110.0 / 2.0), (6.0 + 120.0 / 2.0)
        };

        double srcOffsetX = -20.0 / 2.0;
        double srcOffsetY = -30.0 / 2.0;

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

    private static void testIsSimpleTransformation(CommonAffineTransformations testedCode) {
        assertFalse(testedCode.isSimpleTransformation(
                BasicImageTransformations.newZoomTransformation(2.0, 1.0)));
        assertTrue(testedCode.isSimpleTransformation(
                BasicImageTransformations.newZoomTransformation(-1.0, -1.0)));
        assertTrue(testedCode.isSimpleTransformation(
                BasicImageTransformations.newOffsetTransformation(10.0, 100.0)));
        assertTrue(testedCode.isSimpleTransformation(
                newRotateDegTransformation(0)));
        assertTrue(testedCode.isSimpleTransformation(
                newRotateDegTransformation(90)));
        assertTrue(testedCode.isSimpleTransformation(
                newRotateDegTransformation(180)));
        assertTrue(testedCode.isSimpleTransformation(
                newRotateDegTransformation(270)));
    }

    private CommonAffineTransformationsTests() {
        throw new AssertionError();
    }
}
