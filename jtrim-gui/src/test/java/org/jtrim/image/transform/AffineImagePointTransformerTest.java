package org.jtrim.image.transform;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AffineImagePointTransformerTest {
    private static final double DOUBLE_TOLERANCE = 0.00000001;

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

    @Test
    public void testTransformSrcToDest() {
        AffineTransform transf = AffineTransform.getTranslateInstance(100.0, 200.0);
        AffineImagePointTransformer transformer = new AffineImagePointTransformer(transf);

        Point2D.Double origSrc = new Point2D.Double(52.0, 37.0);

        Point2D.Double src = (Point2D.Double)origSrc.clone();
        Point2D.Double dest = new Point2D.Double();
        transformer.transformSrcToDest(src, dest);

        Point2D.Double expectedDest = new Point2D.Double();
        transf.transform(origSrc, expectedDest);

        assertEquals(origSrc.x, src.x, DOUBLE_TOLERANCE);
        assertEquals(origSrc.y, src.y, DOUBLE_TOLERANCE);

        assertEquals(expectedDest.x, dest.x, DOUBLE_TOLERANCE);
        assertEquals(expectedDest.y, dest.y, DOUBLE_TOLERANCE);
    }

    @Test
    public void testTransformDestToSrc() throws Exception {
        AffineTransform transf = AffineTransform.getTranslateInstance(100.0, 200.0);
        AffineImagePointTransformer transformer = new AffineImagePointTransformer(transf);

        Point2D.Double origDest = new Point2D.Double(52.0, 37.0);

        Point2D.Double dest = (Point2D.Double)origDest.clone();
        Point2D.Double src = new Point2D.Double();
        transformer.transformDestToSrc(dest, src);

        Point2D.Double expectedSrc = new Point2D.Double();
        transf.inverseTransform(origDest, expectedSrc);

        assertEquals(origDest.x, dest.x, DOUBLE_TOLERANCE);
        assertEquals(origDest.y, dest.y, DOUBLE_TOLERANCE);

        assertEquals(expectedSrc.x, src.x, DOUBLE_TOLERANCE);
        assertEquals(expectedSrc.y, src.y, DOUBLE_TOLERANCE);
    }
}