package org.jtrim2.image.transform;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class SerialImagePointTransformerTest {
    private static final double DOUBLE_TOLERANCE = 0.0000001;
    private static final ImagePointTransformer IDENTITY = AffineImagePointTransformer.IDENTITY;

    private static void checkEqualPointTransformersBackward(
            double x,
            double y,
            ImagePointTransformer expected,
            ImagePointTransformer actual) throws Exception {
        Point2D.Double origDest = new Point2D.Double(x, y);

        Point2D.Double dest = (Point2D.Double) origDest.clone();
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

        Point2D.Double src = (Point2D.Double) origSrc.clone();
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

    private static void checkEqualPointTransformers(
            ImagePointTransformer expected,
            ImagePointTransformer actual) throws Exception {
        checkEqualPointTransformersForward(expected, actual);
        checkEqualPointTransformersBackward(expected, actual);
    }

    private static TransformerFactory[] factories() {
        return new TransformerFactory[] {
            (transformers) -> SerialImagePointTransformer.combine(transformers),
            (transformers) -> SerialImagePointTransformer.combine(Arrays.asList(transformers))
        };
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

        for (TransformerFactory factory: factories()) {
            ImagePointTransformer checked = factory.create(pointTransf1, pointTransf2);
            checkEqualPointTransformersForward(pointTransf, checked);
        }
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

        for (TransformerFactory factory: factories()) {
            ImagePointTransformer checked = factory.create(pointTransf1, pointTransf2);
            checkEqualPointTransformersBackward(pointTransf, checked);
        }
    }

    @Test
    public void testNested() throws Exception {
        AffineTransform transf1 = AffineTransform.getTranslateInstance(50.0, 40.0);
        AffineTransform transf2 = AffineTransform.getRotateInstance(Math.PI / 6);
        AffineTransform transf3 = AffineTransform.getScaleInstance(1.5, 1.5);

        AffineTransform transf = new AffineTransform();
        transf.concatenate(transf3);
        transf.concatenate(transf2);
        transf.concatenate(transf1);

        ImagePointTransformer pointTransf1 = new AffineImagePointTransformer(transf1);
        ImagePointTransformer pointTransf2 = new AffineImagePointTransformer(transf2);
        ImagePointTransformer pointTransf3 = new AffineImagePointTransformer(transf3);
        ImagePointTransformer pointTransf = new AffineImagePointTransformer(transf);

        ImagePointTransformer nested = SerialImagePointTransformer.combine(pointTransf1, pointTransf2);

        for (TransformerFactory factory: factories()) {
            ImagePointTransformer checked = factory.create(nested, pointTransf3);
            checkEqualPointTransformers(pointTransf, checked);
        }
    }

    @Test
    public void testIdentity() throws Exception {
        AffineTransform transf1 = AffineTransform.getTranslateInstance(50.0, 40.0);
        AffineTransform transf2 = AffineTransform.getRotateInstance(Math.PI / 6);

        AffineTransform transf = new AffineTransform();
        transf.concatenate(transf2);
        transf.concatenate(transf1);

        ImagePointTransformer pointTransf1 = new AffineImagePointTransformer(transf1);
        ImagePointTransformer pointTransf2 = new AffineImagePointTransformer(transf2);
        ImagePointTransformer pointTransf = new AffineImagePointTransformer(transf);

        for (TransformerFactory factory: factories()) {
            ImagePointTransformer checked = factory.create(
                    IDENTITY, pointTransf1, IDENTITY, pointTransf2, IDENTITY);

            checkEqualPointTransformers(pointTransf, checked);
        }
    }

    @Test
    public void testFilterIdentity() throws Exception {
        AffineTransform transf = AffineTransform.getTranslateInstance(50.0, 40.0);
        ImagePointTransformer pointTransf = new AffineImagePointTransformer(transf);

        for (TransformerFactory factory: factories()) {
            assertSame(pointTransf, factory.create(pointTransf));
            assertSame(pointTransf, factory.create(pointTransf, IDENTITY));
            assertSame(pointTransf, factory.create(IDENTITY, pointTransf));
            assertSame(pointTransf, factory.create(IDENTITY, pointTransf, IDENTITY));
        }
    }

    @Test
    public void testEmpty() throws Exception {
        for (TransformerFactory factory: factories()) {
            ImagePointTransformer checked = factory.create();
            checkEqualPointTransformers(IDENTITY, checked);
        }
    }

    private interface TransformerFactory {
        public ImagePointTransformer create(ImagePointTransformer... transformers);
    }
}
