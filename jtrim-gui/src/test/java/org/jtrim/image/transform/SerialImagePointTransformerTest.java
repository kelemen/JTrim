package org.jtrim.image.transform;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collections;
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
public class SerialImagePointTransformerTest {
    private static final double DOUBLE_TOLERANCE = 0.0000001;

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

    private static void checkEqualPointTransformersBackward(
            double x,
            double y,
            ImagePointTransformer expected,
            ImagePointTransformer actual) throws Exception {
        Point2D.Double origDest = new Point2D.Double(x, y);

        Point2D.Double dest = (Point2D.Double)origDest.clone();
        Point2D.Double srcActual = new Point2D.Double();
        Point2D.Double srcExpected = new Point2D.Double();

        actual.transformDestToSrc(dest, srcActual);
        expected.transformDestToSrc(dest, srcExpected);

        // Test that src was not modified
        assertEquals(origDest, dest);

        assertEquals(srcExpected.x, srcActual.x, DOUBLE_TOLERANCE);
        assertEquals(srcExpected.y, srcActual.y, DOUBLE_TOLERANCE);
    }

    private static void checkEqualPointTransformersForward(
            double x,
            double y,
            ImagePointTransformer expected,
            ImagePointTransformer actual) {

        Point2D.Double origSrc = new Point2D.Double(x, y);

        Point2D.Double src = (Point2D.Double)origSrc.clone();
        Point2D.Double destActual = new Point2D.Double();
        Point2D.Double destExpected = new Point2D.Double();

        actual.transformSrcToDest(src, destActual);
        expected.transformSrcToDest(src, destExpected);

        // Test that src was not modified
        assertEquals(origSrc, src);

        assertEquals(destExpected.x, destActual.x, DOUBLE_TOLERANCE);
        assertEquals(destExpected.y, destActual.y, DOUBLE_TOLERANCE);
    }

    private static void checkEqualPointTransformersBackward(
            ImagePointTransformer expected,
            ImagePointTransformer actual) throws Exception {
        checkEqualPointTransformersBackward(0.0, 0.0, expected, actual);
        checkEqualPointTransformersBackward(-1.0, -1.0, expected, actual);
        checkEqualPointTransformersBackward(1.0, 1.0, expected, actual);
        checkEqualPointTransformersBackward(647.0, 943.0, expected, actual);
        checkEqualPointTransformersBackward(-647.0, -943.0, expected, actual);
    }

    private static void checkEqualPointTransformersForward(
            ImagePointTransformer expected,
            ImagePointTransformer actual) {
        checkEqualPointTransformersForward(0.0, 0.0, expected, actual);
        checkEqualPointTransformersForward(-1.0, -1.0, expected, actual);
        checkEqualPointTransformersForward(1.0, 1.0, expected, actual);
        checkEqualPointTransformersForward(647.0, 943.0, expected, actual);
        checkEqualPointTransformersForward(-647.0, -943.0, expected, actual);
    }

    @Test
    public void testTransformSrcToDest() {
        AffineTransform transf1 = AffineTransform.getTranslateInstance(50.0, 40.0);
        AffineTransform transf2 = AffineTransform.getRotateInstance(Math.PI / 6);

        AffineTransform transf = new AffineTransform();
        transf.concatenate(transf2);
        transf.concatenate(transf1);

        ImagePointTransformer pointTransf1 = new AffineImagePointTransformer(transf1);
        ImagePointTransformer pointTransf2 = new AffineImagePointTransformer(transf2);
        ImagePointTransformer pointTransf = new AffineImagePointTransformer(transf);

        SerialImagePointTransformer serialPointTransf1 = new SerialImagePointTransformer(pointTransf1, pointTransf2);
        checkEqualPointTransformersForward(pointTransf, serialPointTransf1);

        SerialImagePointTransformer serialPointTransf2 = new SerialImagePointTransformer(Arrays.asList(pointTransf1, pointTransf2));
        checkEqualPointTransformersForward(pointTransf, serialPointTransf2);
    }

    @Test
    public void testTransformDestToSrc() throws Exception {
        AffineTransform transf1 = AffineTransform.getTranslateInstance(50.0, 40.0);
        AffineTransform transf2 = AffineTransform.getRotateInstance(Math.PI / 6);

        AffineTransform transf = new AffineTransform();
        transf.concatenate(transf2);
        transf.concatenate(transf1);

        ImagePointTransformer pointTransf1 = new AffineImagePointTransformer(transf1);
        ImagePointTransformer pointTransf2 = new AffineImagePointTransformer(transf2);
        ImagePointTransformer pointTransf = new AffineImagePointTransformer(transf);

        SerialImagePointTransformer serialPointTransf1 = new SerialImagePointTransformer(pointTransf1, pointTransf2);
        checkEqualPointTransformersBackward(pointTransf, serialPointTransf1);

        SerialImagePointTransformer serialPointTransf2 = new SerialImagePointTransformer(Arrays.asList(pointTransf1, pointTransf2));
        checkEqualPointTransformersBackward(pointTransf, serialPointTransf2);
    }

    @Test
    public void testIdentity() throws Exception {
        ImagePointTransformer identity = new AffineImagePointTransformer(new AffineTransform());

        SerialImagePointTransformer serialPointTransf1 = new SerialImagePointTransformer();
        checkEqualPointTransformersForward(identity, serialPointTransf1);
        checkEqualPointTransformersBackward(identity, serialPointTransf1);

        SerialImagePointTransformer serialPointTransf2 = new SerialImagePointTransformer(Collections.<ImagePointTransformer>emptyList());
        checkEqualPointTransformersForward(identity, serialPointTransf2);
        checkEqualPointTransformersBackward(identity, serialPointTransf2);
    }
}