package org.jtrim2.testutils.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import org.jtrim2.image.transform.ImagePointTransformer;

import static org.junit.Assert.*;

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

    public static void checkEqualPointTransformersBackward(
            double x,
            double y,
            ImagePointTransformer expected,
            ImagePointTransformer actual) throws NoninvertibleTransformException {

        Point2D.Double dest = new Point2D.Double(x, y);
        Point2D.Double srcActual = new Point2D.Double();
        Point2D.Double srcExpected = new Point2D.Double();

        actual.transformDestToSrc(dest, srcActual);
        expected.transformDestToSrc(dest, srcExpected);

        assertEquals(srcExpected.x, srcActual.x, DOUBLE_TOLERANCE);
        assertEquals(srcExpected.y, srcActual.y, DOUBLE_TOLERANCE);
    }

    public static void checkEqualPointTransformersForward(
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

    public static void checkEqualPointTransformers(
            double x,
            double y,
            ImagePointTransformer expected,
            ImagePointTransformer actual) {

        checkEqualPointTransformersForward(x, y, expected, actual);
        try {
            checkEqualPointTransformersBackward(x, y, expected, actual);
        } catch (NoninvertibleTransformException ex) {
            throw new RuntimeException(ex);
        }
    }

    private PointTransformerChecks() {
        throw new AssertionError();
    }
}
