package org.jtrim.image.transform;

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
public class BasicImageTransformationsTest {
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
    public void testInitialDefaultBuilder() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        assertEquals(0.0, builder.getOffsetX(), 0.0);
        assertEquals(0.0, builder.getOffsetY(), 0.0);
        assertEquals(0.0, builder.getRotateInRadians(), 0.0);
        assertEquals(1.0, builder.getZoomX(), 0.0);
        assertEquals(1.0, builder.getZoomY(), 0.0);
        assertEquals(0, builder.getRotateInDegrees());
        assertFalse(builder.isFlipHorizontal());
        assertFalse(builder.isFlipVertical());

        BasicImageTransformations transf = builder.create();
        assertEquals(0.0, transf.getOffsetX(), 0.0);
        assertEquals(0.0, transf.getOffsetY(), 0.0);
        assertEquals(0.0, transf.getRotateInRadians(), 0.0);
        assertEquals(1.0, transf.getZoomX(), 0.0);
        assertEquals(1.0, transf.getZoomY(), 0.0);
        assertEquals(0, transf.getRotateInDegrees());
        assertFalse(transf.isFlipHorizontal());
        assertFalse(transf.isFlipVertical());
        assertTrue(transf.isIdentity());
    }

    @Test
    public void testInitialNonDefaultBuilder() {
        BasicImageTransformations.Builder parentBuilder = new BasicImageTransformations.Builder();
        parentBuilder.setFlipHorizontal(true);
        parentBuilder.setFlipVertical(true);
        parentBuilder.setOffset(2.0, 3.0);
        parentBuilder.setRotateInRadians(0.5);
        parentBuilder.setZoomX(4.0);
        parentBuilder.setZoomY(5.0);
        BasicImageTransformations parent = parentBuilder.create();

        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder(parent);
        assertEquals(parent.getOffsetX(), builder.getOffsetX(), 0.0);
        assertEquals(parent.getOffsetY(), builder.getOffsetY(), 0.0);
        assertEquals(parent.getRotateInRadians(), builder.getRotateInRadians(), 0.0);
        assertEquals(parent.getZoomX(), builder.getZoomX(), 0.0);
        assertEquals(parent.getZoomY(), builder.getZoomY(), 0.0);
        assertEquals(parent.getRotateInDegrees(), builder.getRotateInDegrees());
        assertTrue(builder.isFlipHorizontal());
        assertTrue(builder.isFlipVertical());

        BasicImageTransformations transf = builder.create();
        assertEquals(parent.getOffsetX(), transf.getOffsetX(), 0.0);
        assertEquals(parent.getOffsetY(), transf.getOffsetY(), 0.0);
        assertEquals(parent.getRotateInRadians(), transf.getRotateInRadians(), 0.0);
        assertEquals(parent.getZoomX(), transf.getZoomX(), 0.0);
        assertEquals(parent.getZoomY(), transf.getZoomY(), 0.0);
        assertEquals(parent.getRotateInDegrees(), transf.getRotateInDegrees());
        assertTrue(transf.isFlipHorizontal());
        assertTrue(transf.isFlipVertical());
        assertFalse(transf.isIdentity());
    }

    private static void testRotateDeg(int degrees) {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInDegrees(degrees);
        assertEquals(degrees, builder.getRotateInDegrees());
        assertEquals(Math.toRadians(degrees), builder.getRotateInRadians(), DOUBLE_TOLERANCE);

        BasicImageTransformations transf = builder.create();
        assertEquals(degrees, transf.getRotateInDegrees());
        assertEquals(Math.toRadians(degrees), transf.getRotateInRadians(), DOUBLE_TOLERANCE);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testRotateDeg() {
        for (int degrees = 0; degrees < 360; degrees += 45) {
            testRotateDeg(degrees);
        }
        testRotateDeg(1);
        testRotateDeg(359);
    }

    private static void testRotateRad(int degrees) {
        double rad = Math.toRadians(degrees);

        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInRadians(rad);
        assertEquals((int)Math.round(Math.toDegrees(rad)), builder.getRotateInDegrees());
        assertEquals(rad, builder.getRotateInRadians(), DOUBLE_TOLERANCE);

        BasicImageTransformations transf = builder.create();
        assertEquals((int)Math.round(Math.toDegrees(rad)), transf.getRotateInDegrees());
        assertEquals(rad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testRotateRad() {
        for (int degrees = 0; degrees < 360; degrees += 45) {
            testRotateRad(degrees);
        }
        testRotateRad(1);
        testRotateRad(359);
    }

    private static void testSpecialRotate(int degrees, double expectedRadExactly) {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInDegrees(degrees);
        assertEquals(expectedRadExactly, builder.getRotateInRadians(), 0.0);

        BasicImageTransformations transf = builder.create();
        assertEquals(expectedRadExactly, transf.getRotateInRadians(), 0.0);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testSpecialRotates() {
        testSpecialRotate(-360, BasicImageTransformations.RAD_0);
        testSpecialRotate(-270, BasicImageTransformations.RAD_90);
        testSpecialRotate(-180, BasicImageTransformations.RAD_180);
        testSpecialRotate(-90, BasicImageTransformations.RAD_270);
        testSpecialRotate(0, BasicImageTransformations.RAD_0);
        testSpecialRotate(90, BasicImageTransformations.RAD_90);
        testSpecialRotate(180, BasicImageTransformations.RAD_180);
        testSpecialRotate(270, BasicImageTransformations.RAD_270);
        testSpecialRotate(360, BasicImageTransformations.RAD_0);
        testSpecialRotate(450, BasicImageTransformations.RAD_90);
        testSpecialRotate(540, BasicImageTransformations.RAD_180);
        testSpecialRotate(630, BasicImageTransformations.RAD_270);
    }

    private static void testRadRotate(double rad, double expectedRad) {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInRadians(rad);
        assertEquals(expectedRad, builder.getRotateInRadians(), DOUBLE_TOLERANCE);

        BasicImageTransformations transf = builder.create();
        assertEquals(expectedRad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testRotateRadNonUniform() {
        for (int degrees = -360; degrees < 0; degrees += 45) {
            testRadRotate(Math.toRadians(degrees), Math.toRadians(degrees + 360));
        }
        for (int degrees = 360; degrees < 720; degrees += 45) {
            testRadRotate(Math.toRadians(degrees), Math.toRadians(degrees - 360));
        }
    }

    @Test
    public void testRotateRadNan() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInRadians(Double.NaN);
        assertTrue(Double.isNaN(builder.getRotateInRadians()));

        BasicImageTransformations transf = builder.create();
        assertTrue(Double.isNaN(transf.getRotateInRadians()));

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testRotateRadNegZero() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInRadians(-0.0);
        assertEquals(0.0, builder.getRotateInRadians(), 0.0);

        BasicImageTransformations transf = builder.create();
        assertEquals(0.0, transf.getRotateInRadians(), 0.0);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testRotateRadPosInf() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInRadians(Double.POSITIVE_INFINITY);
        assertTrue(Double.isInfinite(builder.getRotateInRadians()) && builder.getRotateInRadians() > 0.0);

        BasicImageTransformations transf = builder.create();
        assertTrue(Double.isInfinite(transf.getRotateInRadians()) && transf.getRotateInRadians() > 0.0);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testRotateRadNegInf() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setRotateInRadians(Double.NEGATIVE_INFINITY);
        assertTrue(Double.isInfinite(builder.getRotateInRadians()) && builder.getRotateInRadians() < 0.0);

        BasicImageTransformations transf = builder.create();
        assertTrue(Double.isInfinite(transf.getRotateInRadians()) && transf.getRotateInRadians() < 0.0);

        assertNotNull(transf.toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testZoomXY() {
        for (double zoomX: Arrays.asList(-3.5, -1.0, 0.0, 1.0, 5.6)) {
            for (double zoomY: Arrays.asList(-3.5, -1.0, 0.0, 1.0, 5.6)) {
                BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
                builder.setZoomX(zoomX);
                builder.setZoomY(zoomY);
                assertEquals(zoomX, builder.getZoomX(), 0.0);
                assertEquals(zoomY, builder.getZoomY(), 0.0);

                BasicImageTransformations transf = builder.create();
                assertEquals(zoomX, transf.getZoomX(), 0.0);
                assertEquals(zoomY, transf.getZoomY(), 0.0);

                assertNotNull(transf.toString());
                assertNotNull(builder.toString());
            }
        }
    }

    @Test
    public void testZoom() {
        for (double zoom: Arrays.asList(-3.5, -1.0, 0.0, 1.0, 5.6)) {
            BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
            builder.setZoom(zoom);
            assertEquals(zoom, builder.getZoomX(), 0.0);
            assertEquals(zoom, builder.getZoomY(), 0.0);

            BasicImageTransformations transf = builder.create();
            assertEquals(zoom, transf.getZoomX(), 0.0);
            assertEquals(zoom, transf.getZoomY(), 0.0);

            assertNotNull(transf.toString());
            assertNotNull(builder.toString());
        }
    }

    @Test
    public void testOffset() {
        for (double offsetX: Arrays.asList(-3.5, -1.0, 0.0, 1.0, 5.6)) {
            for (double offsetY: Arrays.asList(-3.5, -1.0, 0.0, 1.0, 5.6)) {
                BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
                builder.setOffset(offsetX, offsetY);
                assertEquals(offsetX, builder.getOffsetX(), 0.0);
                assertEquals(offsetY, builder.getOffsetY(), 0.0);

                BasicImageTransformations transf = builder.create();
                assertEquals(offsetX, transf.getOffsetX(), 0.0);
                assertEquals(offsetY, transf.getOffsetY(), 0.0);

                assertNotNull(transf.toString());
                assertNotNull(builder.toString());
            }
        }
    }

    @Test
    public void testSetFlip() {
        for (boolean flipH: Arrays.asList(false, true)) {
            for (boolean flipV: Arrays.asList(false, true)) {
                BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
                builder.setFlipHorizontal(flipH);
                builder.setFlipVertical(flipV);
                assertEquals(flipH, builder.isFlipHorizontal());
                assertEquals(flipV, builder.isFlipVertical());

                BasicImageTransformations transf = builder.create();
                assertEquals(flipH, transf.isFlipHorizontal());
                assertEquals(flipV, transf.isFlipVertical());

                assertNotNull(transf.toString());
                assertNotNull(builder.toString());
            }
        }
    }

    @Test
    public void testFlipHorizontal() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.flipHorizontal();
        assertTrue(builder.isFlipHorizontal());
        assertTrue(builder.create().isFlipHorizontal());

        builder.flipHorizontal();
        assertFalse(builder.isFlipHorizontal());
        assertFalse(builder.create().isFlipHorizontal());

        assertNotNull(builder.create().toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testFlipVertical() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.flipVertical();
        assertTrue(builder.isFlipVertical());
        assertTrue(builder.create().isFlipVertical());

        builder.flipVertical();
        assertFalse(builder.isFlipVertical());
        assertFalse(builder.create().isFlipVertical());

        assertNotNull(builder.create().toString());
        assertNotNull(builder.toString());
    }

    @Test
    public void testIdentity() {
        BasicImageTransformations transf = BasicImageTransformations.identityTransformation();
        assertEquals(0.0, transf.getOffsetX(), 0.0);
        assertEquals(0.0, transf.getOffsetY(), 0.0);
        assertEquals(0.0, transf.getRotateInRadians(), 0.0);
        assertEquals(1.0, transf.getZoomX(), 0.0);
        assertEquals(1.0, transf.getZoomY(), 0.0);
        assertEquals(0, transf.getRotateInDegrees());
        assertFalse(transf.isFlipHorizontal());
        assertFalse(transf.isFlipVertical());
        assertTrue(transf.isIdentity());

        assertNotNull(transf.toString());
    }

    @Test
    public void testNewZoom() {
        for (double zoomX: Arrays.asList(-3.5, -1.0, 0.0, 5.6)) {
            for (double zoomY: Arrays.asList(-3.5, -1.0, 0.0, 5.6)) {
                BasicImageTransformations transf = BasicImageTransformations.newZoomTransformation(zoomX, zoomY);
                assertEquals(0.0, transf.getOffsetX(), 0.0);
                assertEquals(0.0, transf.getOffsetY(), 0.0);
                assertEquals(0.0, transf.getRotateInRadians(), 0.0);
                assertEquals(zoomX, transf.getZoomX(), 0.0);
                assertEquals(zoomY, transf.getZoomY(), 0.0);
                assertEquals(0, transf.getRotateInDegrees());
                assertFalse(transf.isFlipHorizontal());
                assertFalse(transf.isFlipVertical());
                assertFalse(transf.isIdentity());

                assertNotNull(transf.toString());
            }
        }

        BasicImageTransformations transf = BasicImageTransformations.newZoomTransformation(1.0, 1.0);
        assertTrue(transf.isIdentity());
    }

    @Test
    public void testNewOffset() {
        for (double offsetX: Arrays.asList(-3.5, -1.0, 1.0, 5.6)) {
            for (double offsetY: Arrays.asList(-3.5, -1.0, 1.0, 5.6)) {
                BasicImageTransformations transf = BasicImageTransformations.newOffsetTransformation(offsetX, offsetY);
                assertEquals(offsetX, transf.getOffsetX(), 0.0);
                assertEquals(offsetY, transf.getOffsetY(), 0.0);
                assertEquals(0.0, transf.getRotateInRadians(), 0.0);
                assertEquals(1.0, transf.getZoomX(), 0.0);
                assertEquals(1.0, transf.getZoomY(), 0.0);
                assertEquals(0, transf.getRotateInDegrees());
                assertFalse(transf.isFlipHorizontal());
                assertFalse(transf.isFlipVertical());
                assertFalse(transf.isIdentity());

                assertNotNull(transf.toString());
            }
        }

        BasicImageTransformations transf = BasicImageTransformations.newOffsetTransformation(0.0, 0.0);
        assertTrue(transf.isIdentity());
    }

    @Test
    public void testNewRotate() {
        for (double rad: Arrays.asList(0.5, 1.0, 2.6)) {
            BasicImageTransformations transf = BasicImageTransformations.newRotateTransformation(rad);
            assertEquals(0.0, transf.getOffsetX(), 0.0);
            assertEquals(0.0, transf.getOffsetY(), 0.0);
            assertEquals(rad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);
            assertEquals(1.0, transf.getZoomX(), 0.0);
            assertEquals(1.0, transf.getZoomY(), 0.0);
            assertEquals((int)Math.round(Math.toDegrees(rad)), transf.getRotateInDegrees());
            assertFalse(transf.isFlipHorizontal());
            assertFalse(transf.isFlipVertical());
            assertFalse(transf.isIdentity());

            assertNotNull(transf.toString());
        }

        BasicImageTransformations transf = BasicImageTransformations.newRotateTransformation(0.0);
        assertTrue(transf.isIdentity());
    }

    @Test
    public void compareOffsetXDiffers() {
        BasicImageTransformations transf1 = BasicImageTransformations.newOffsetTransformation(1.0, 0.0);
        BasicImageTransformations transf2 = BasicImageTransformations.newOffsetTransformation(2.0, 0.0);
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(transf2.toString());
    }

    @Test
    public void compareOffsetYDiffers() {
        BasicImageTransformations transf1 = BasicImageTransformations.newOffsetTransformation(0.0, 1.0);
        BasicImageTransformations transf2 = BasicImageTransformations.newOffsetTransformation(0.0, 2.0);
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(transf2.toString());
    }

    @Test
    public void compareZoomXDiffers() {
        BasicImageTransformations transf1 = BasicImageTransformations.newZoomTransformation(2.0, 1.0);
        BasicImageTransformations transf2 = BasicImageTransformations.newZoomTransformation(3.0, 1.0);
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(transf2.toString());
    }

    @Test
    public void compareZoomYDiffers() {
        BasicImageTransformations transf1 = BasicImageTransformations.newZoomTransformation(1.0, 2.0);
        BasicImageTransformations transf2 = BasicImageTransformations.newZoomTransformation(1.0, 3.0);
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(transf2.toString());
    }

    @Test
    public void compareRotateRadDiffers() {
        BasicImageTransformations transf1 = BasicImageTransformations.newRotateTransformation(1.0);
        BasicImageTransformations transf2 = BasicImageTransformations.newRotateTransformation(2.0);
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(transf2.toString());
    }

    @Test
    public void compareRotateDefDiffers() {
        BasicImageTransformations.Builder builder1 = new BasicImageTransformations.Builder();
        BasicImageTransformations.Builder builder2 = new BasicImageTransformations.Builder();

        builder1.setRotateInDegrees(10);
        builder2.setRotateInDegrees(20);

        BasicImageTransformations transf1 = builder1.create();
        BasicImageTransformations transf2 = builder2.create();
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(builder1.toString());
        assertNotNull(transf2.toString());
        assertNotNull(builder2.toString());
    }

    @Test
    public void compareFlipHorizontalDiffers() {
        BasicImageTransformations.Builder builder1 = new BasicImageTransformations.Builder();
        BasicImageTransformations.Builder builder2 = new BasicImageTransformations.Builder();

        builder1.flipHorizontal();

        BasicImageTransformations transf1 = builder1.create();
        BasicImageTransformations transf2 = builder2.create();
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(builder1.toString());
        assertNotNull(transf2.toString());
        assertNotNull(builder2.toString());
    }

    @Test
    public void compareFlipVerticalDiffers() {
        BasicImageTransformations.Builder builder1 = new BasicImageTransformations.Builder();
        BasicImageTransformations.Builder builder2 = new BasicImageTransformations.Builder();

        builder1.flipVertical();

        BasicImageTransformations transf1 = builder1.create();
        BasicImageTransformations transf2 = builder2.create();
        assertFalse(transf1.equals(transf2));
        assertFalse(transf2.equals(transf1));

        assertNotNull(transf1.toString());
        assertNotNull(builder1.toString());
        assertNotNull(transf2.toString());
        assertNotNull(builder2.toString());
    }

    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void compareToNull() {
        assertFalse(BasicImageTransformations.identityTransformation().equals(null));
    }

    @Test
    public void compareToBuilder() {
        Object builder = new BasicImageTransformations.Builder();
        Object transf = BasicImageTransformations.identityTransformation();

        // Comparing different types.
        assertFalse(transf.equals(builder));
        assertFalse(builder.equals(transf));
    }

    @Test
    public void compareEquals1() {
        Object transf1 = BasicImageTransformations.identityTransformation();
        Object transf2 = BasicImageTransformations.identityTransformation();

        // Comparing different types.
        assertTrue(transf2.equals(transf1));
        assertTrue(transf1.equals(transf2));
        assertEquals(transf1.hashCode(), transf2.hashCode());
    }

    @Test
    public void compareEquals2() {
        Object transf1 = BasicImageTransformations.newZoomTransformation(2.0, 3.0);
        Object transf2 = BasicImageTransformations.newZoomTransformation(2.0, 3.0);

        // Comparing different types.
        assertTrue(transf2.equals(transf1));
        assertTrue(transf1.equals(transf2));
        assertEquals(transf1.hashCode(), transf2.hashCode());
    }

    @Test
    public void compareEquals3() {
        Object transf1 = BasicImageTransformations.newOffsetTransformation(2.0, 3.0);
        Object transf2 = BasicImageTransformations.newOffsetTransformation(2.0, 3.0);

        // Comparing different types.
        assertTrue(transf2.equals(transf1));
        assertTrue(transf1.equals(transf2));
        assertEquals(transf1.hashCode(), transf2.hashCode());
    }

    @Test
    public void compareEquals4() {
        Object transf1 = BasicImageTransformations.newRotateTransformation(2.0);
        Object transf2 = BasicImageTransformations.newRotateTransformation(2.0);

        // Comparing different types.
        assertTrue(transf2.equals(transf1));
        assertTrue(transf1.equals(transf2));
        assertEquals(transf1.hashCode(), transf2.hashCode());
    }

    @Test
    public void compareEqualsDifferentProperties1() {
        BasicImageTransformations.Builder builder1 = new BasicImageTransformations.Builder();
        BasicImageTransformations.Builder builder2 = new BasicImageTransformations.Builder();

        builder1.setZoomX(-3.0);
        builder2.setZoomX(3.0);

        builder1.setFlipHorizontal(true);

        Object transf1 = builder1.create();
        Object transf2 = builder2.create();

        // Comparing different types.
        assertTrue(transf2.equals(transf1));
        assertTrue(transf1.equals(transf2));
        assertEquals(transf1.hashCode(), transf2.hashCode());
    }

    @Test
    public void compareEqualsDifferentProperties2() {
        BasicImageTransformations.Builder builder1 = new BasicImageTransformations.Builder();
        BasicImageTransformations.Builder builder2 = new BasicImageTransformations.Builder();

        builder1.setZoomY(-3.0);
        builder2.setZoomY(3.0);

        builder1.setFlipVertical(true);

        Object transf1 = builder1.create();
        Object transf2 = builder2.create();

        // Comparing different types.
        assertTrue(transf2.equals(transf1));
        assertTrue(transf1.equals(transf2));
    }
}
