package org.jtrim.image.transform;

import java.awt.geom.Point2D;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class PointTransformerChecks {
    private static final double DOUBLE_TOLERANCE = 0.000001;

    public static void checkEqualPointTransformers(
            ImagePointTransformer expected,
            ImagePointTransformer actual) {
        checkEqualPointTransformers(0.0, 0.0, expected, actual);
        checkEqualPointTransformers(-1.0, -1.0, expected, actual);
        checkEqualPointTransformers(1.0, 1.0, expected, actual);
        checkEqualPointTransformers(647.0, 943.0, expected, actual);
        checkEqualPointTransformers(-647.0, -943.0, expected, actual);
    }

    public static void checkEqualPointTransformers(
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

    private PointTransformerChecks() {
        throw new AssertionError();
    }
}
