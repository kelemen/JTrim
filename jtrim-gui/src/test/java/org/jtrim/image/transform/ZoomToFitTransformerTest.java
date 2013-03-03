package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
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
public class ZoomToFitTransformerTest {
    private static final double DEFAULT_DOUBLE_TOLERANCE = 0.00000001;

    private static final Set<ZoomToFitOption> TYPICAL_OPTIONS_MAY_MAGNIFY
            = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.KEEP_ASPECT_RATIO);
    private static final Set<ZoomToFitOption> TYPICAL_OPTIONS
            = mayNotMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.KEEP_ASPECT_RATIO);

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

    private static Set<ZoomToFitOption> mayMagnify(ZoomToFitOption... options) {
        Set<ZoomToFitOption> result = EnumSet.of(ZoomToFitOption.MAY_MAGNIFY);
        result.addAll(Arrays.asList(options));
        return result;
    }

    private static Set<ZoomToFitOption> mayNotMagnify(ZoomToFitOption... options) {
        Set<ZoomToFitOption> result = EnumSet.copyOf(Arrays.asList(options));
        assertFalse(result.contains(ZoomToFitOption.MAY_MAGNIFY));
        return result;
    }

    private static ZoomToFitTransformer create(
            BasicImageTransformations transBase,
            Set<ZoomToFitOption> options,
            Color bckgColor,
            InterpolationType interpolationType) {
        return new ZoomToFitTransformer(transBase, options, bckgColor, interpolationType);
    }

    private static boolean cmpDoubles(double value1, double value2, double epsilon) {
        assert !Double.isNaN(epsilon);

        if (Double.isNaN(value1)) {
            return Double.isNaN(value2);
        }
        if (Double.isInfinite(value1)) {
            if (Double.isInfinite(value2)) {
                return value1 < 0.0 ? (value2 < 0.0) : (value2 > 0.0);
            }
            else {
                return false;
            }
        }
        return Math.abs(value1 - value2) <= epsilon;
    }

    private static BasicImageTransformations normalize(BasicImageTransformations transf) {
        BasicImageTransformations.Builder result;
        if (transf.getZoomX() == 0.0 || transf.getZoomY() == 0.0) {
            result = new BasicImageTransformations.Builder();
            result.setZoom(0.0);
        }
        else {
            result = new BasicImageTransformations.Builder(transf);
            if (result.isFlipHorizontal()) {
                result.setZoomX(-1.0 * result.getZoomX());
            }
            if (result.isFlipVertical()) {
                result.setZoomY(-1.0 * result.getZoomY());
            }
            if (result.getZoomX() < 0.0 && result.getZoomY() < 0.0) {
                result.setZoomX(-1.0 * result.getZoomX());
                result.setZoomY(-1.0 * result.getZoomY());
                result.setRotateInRadians(result.getRotateInRadians() + Math.PI);
            }
        }
        return result.create();
    }

    private static boolean cmpTransformations(
            BasicImageTransformations transf1,
            BasicImageTransformations transf2) {
        return cmpTransformations(transf1, transf2, DEFAULT_DOUBLE_TOLERANCE);
    }

    private static boolean cmpTransformations(
            BasicImageTransformations transf1,
            BasicImageTransformations transf2,
            double epsilon) {

        BasicImageTransformations normTransf1 = normalize(transf1);
        BasicImageTransformations normTransf2 = normalize(transf2);

        if (!cmpDoubles(normTransf1.getOffsetX(), normTransf2.getOffsetX(), epsilon)) {
            return false;
        }
        if (!cmpDoubles(normTransf1.getOffsetY(), normTransf2.getOffsetY(), epsilon)) {
            return false;
        }
        if (!cmpDoubles(normTransf1.getRotateInRadians(), normTransf2.getRotateInRadians(), epsilon)) {
            return false;
        }
        if (!cmpDoubles(normTransf1.getZoomX(), normTransf2.getZoomX(), epsilon)) {
            return false;
        }
        if (!cmpDoubles(normTransf1.getZoomY(), normTransf2.getZoomY(), epsilon)) {
            return false;
        }
        // Normalization always sets flipping to false.
        return true;
    }

    private static void assertCloseEnough(
            BasicImageTransformations expected,
            BasicImageTransformations transf) {
        assertTrue("Expected: " + expected + ". Received: " + transf, cmpTransformations(expected, transf));
    }

    private static void assertCloseEnough(
            String addtionalInfo,
            BasicImageTransformations expected,
            BasicImageTransformations transf) {
        assertTrue("Expected: " + expected + ". Received: " + transf + ". " + addtionalInfo,
                cmpTransformations(expected, transf));
    }

    private static void assertConversion(
            int srcWidth,
            int srcHeight,
            int destWidth,
            int destHeight,
            Set<ZoomToFitOption> options,
            BasicImageTransformations transBase,
            BasicImageTransformations expected) {

        AffineTransform expectedMatrix = AffineImageTransformer.getTransformationMatrix(expected);
        AffineImagePointTransformer expectedTransform = new AffineImagePointTransformer(expectedMatrix);

        for (InterpolationType interpolation : InterpolationType.values()) {
            ZoomToFitTransformer transformer = new ZoomToFitTransformer(
                    transBase, options, Color.GRAY, interpolation);

            BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
            ImageMetaData metaData = new ImageMetaData(srcWidth, srcHeight, true);
            ImageTransformerData input = new ImageTransformerData(srcImage, destWidth, destHeight, metaData);
            TransformedImage converted = transformer.convertData(input);

            for (double x: Arrays.asList(0.0)) {
                for (double y: Arrays.asList(0.0)) {
                    Point2D.Double src = new Point2D.Double(x, y);

                    Point2D.Double dest = new Point2D.Double();
                    converted.transformSrcToDest(src, dest);

                    src.x = src.x - srcWidth / 2.0;
                    src.y = src.y - srcHeight / 2.0;
                    Point2D.Double expectedDest = new Point2D.Double();
                    expectedTransform.transformSrcToDest(src, expectedDest);

                    assertEquals(expectedDest.x + destWidth / 2.0, dest.x, DEFAULT_DOUBLE_TOLERANCE);
                    assertEquals(expectedDest.y + destHeight / 2.0, dest.y, DEFAULT_DOUBLE_TOLERANCE);
                }
            }
        }
    }

    @Test
    public void testTransformationsZeroOrNegativeSourceWidth() {
        for (int srcWidth: Arrays.asList(Integer.MIN_VALUE, -1, 0)) {
            BasicImageTransformations expect = BasicImageTransformations.identityTransformation();
            BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                    srcWidth, 10, 5, 5, TYPICAL_OPTIONS,
                    BasicImageTransformations.identityTransformation());
            assertCloseEnough(expect, transformations);
        }
    }

    @Test
    public void testTransformationsZeroOrNegativeSourceHeight() {
        for (int srcHeight: Arrays.asList(Integer.MIN_VALUE, -1, 0)) {
            BasicImageTransformations expect = BasicImageTransformations.identityTransformation();
            BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                    10, srcHeight, 5, 5, TYPICAL_OPTIONS,
                    BasicImageTransformations.identityTransformation());
            assertCloseEnough(expect, transformations);
        }
    }

    @Test
    public void testTransformationsWidthZoomSmallerIdentityBased1() {
        BasicImageTransformations expect = BasicImageTransformations.identityTransformation();

        for (boolean fitWidth: Arrays.asList(false, true)) {
            for (boolean fitHeight: Arrays.asList(false, true)) {
                for (boolean keepAspect: Arrays.asList(false, true)) {
                    Set<ZoomToFitOption> options = EnumSet.noneOf(ZoomToFitOption.class);
                    if (fitWidth) options.add(ZoomToFitOption.FIT_WIDTH);
                    if (fitHeight) options.add(ZoomToFitOption.FIT_HEIGHT);
                    if (keepAspect) options.add(ZoomToFitOption.KEEP_ASPECT_RATIO);

                    BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                            10, 15, 20, 45, options,
                            BasicImageTransformations.identityTransformation());
                    assertCloseEnough("Error for ZoomToFit: " + options, expect, transformations);
                    assertConversion(10, 15, 20, 45, options,
                            BasicImageTransformations.identityTransformation(), expect);
                }
            }
        }
    }

    @Test
    public void testTransformationsWidthZoomSmallerIdentityBased2() {
        Set<ZoomToFitOption> options = TYPICAL_OPTIONS_MAY_MAGNIFY;
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(2.0, 2.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, TYPICAL_OPTIONS_MAY_MAGNIFY,
                BasicImageTransformations.identityTransformation(), expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerIdentityBased3() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.FIT_HEIGHT);
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(2.0, 3.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
    }

    @Test
    public void testTransformationsWidthZoomSmallerIdentityBased4() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.KEEP_ASPECT_RATIO);
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(3.0, 3.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, BasicImageTransformations.identityTransformation(), expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerIdentityBased5() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT);
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(3.0, 3.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, BasicImageTransformations.identityTransformation(), expect);
    }

    @Test
    public void testTransformationsHeightZoomSmallerIdentityBased1() {
        BasicImageTransformations expect = BasicImageTransformations.identityTransformation();

        for (boolean fitWidth: Arrays.asList(false, true)) {
            for (boolean fitHeight: Arrays.asList(false, true)) {
                for (boolean keepAspect: Arrays.asList(false, true)) {
                    Set<ZoomToFitOption> options = EnumSet.noneOf(ZoomToFitOption.class);
                    if (fitWidth) options.add(ZoomToFitOption.FIT_WIDTH);
                    if (fitHeight) options.add(ZoomToFitOption.FIT_HEIGHT);
                    if (keepAspect) options.add(ZoomToFitOption.KEEP_ASPECT_RATIO);

                    BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                            10, 15, 40, 30, options,
                            BasicImageTransformations.identityTransformation());
                    assertCloseEnough("Error for ZoomToFit: " + options, expect, transformations);
                    assertConversion(10, 15, 40, 30, options,
                            BasicImageTransformations.identityTransformation(), expect);
                }
            }
        }
    }

    @Test
    public void testTransformationsHeightZoomSmallerIdentityBased2() {
        Set<ZoomToFitOption> options = TYPICAL_OPTIONS_MAY_MAGNIFY;
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(2.0, 2.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 40, 30, TYPICAL_OPTIONS_MAY_MAGNIFY,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 40, 30, options, BasicImageTransformations.identityTransformation(), expect);
    }

    @Test
    public void testTransformationsHeightZoomSmallerIdentityBased3() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.FIT_HEIGHT);
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(4.0, 2.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 40, 30, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 40, 30, options, BasicImageTransformations.identityTransformation(), expect);
    }

    @Test
    public void testTransformationsHeightZoomSmallerIdentityBased4() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.KEEP_ASPECT_RATIO);
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(4.0, 4.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 40, 30, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 40, 30, options, BasicImageTransformations.identityTransformation(), expect);
    }

    @Test
    public void testTransformationsHeightZoomSmallerIdentityBased5() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations expect = BasicImageTransformations.newZoomTransformation(4.0, 4.0);
        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 40, 30, options,
                BasicImageTransformations.identityTransformation());
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 40, 30, options, BasicImageTransformations.identityTransformation(), expect);
    }

    private static double getRotatedWidth(double rotate, double width, double height) {
        double sPhi = Math.sin(rotate);
        double cPhi = Math.cos(rotate);

        double widthHalf = width / 2.0;
        double heightHalf = height / 2.0;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (double mulC: new double[]{-1.0, 1.0}) {
            for (double mulS: new double[]{-1.0, 1.0}) {
                double x = mulC * widthHalf * cPhi + mulS * heightHalf * sPhi;
                if (x > maxX) maxX = x;
                if (x < minX) minX = x;
            }
        }
        return maxX - minX;
    }

    private static double getRotatedHeight(double rotate, double width, double height) {
        double sPhi = Math.sin(rotate);
        double cPhi = Math.cos(rotate);

        double widthHalf = width / 2.0;
        double heightHalf = height / 2.0;

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (double mulC: new double[]{-1.0, 1.0}) {
            for (double mulS: new double[]{-1.0, 1.0}) {
                double y = mulS * widthHalf * sPhi + mulC * heightHalf * cPhi;
                if (y > maxY) maxY = y;
                if (y < minY) minY = y;
            }
        }
        return maxY - minY;
    }

    private static BasicImageTransformations newRotateDegTransformation(int degrees) {
        BasicImageTransformations.Builder result = new BasicImageTransformations.Builder();
        result.setRotateInDegrees(degrees);
        return result.create();
    }

    private static BasicImageTransformations newRotateTransformation(double radians, boolean flipH, boolean flipV) {
        BasicImageTransformations.Builder result = new BasicImageTransformations.Builder();
        result.setFlipHorizontal(flipH);
        result.setFlipVertical(flipV);
        result.setRotateInRadians(radians);
        return result.create();
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate90Based1() {
        Set<ZoomToFitOption> options = TYPICAL_OPTIONS_MAY_MAGNIFY;
        BasicImageTransformations rotateTransf = newRotateDegTransformation(90);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / 15.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate90Based2() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(90);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / 15.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate90Based3() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(90);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(45.0 / 10.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate90Based4() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.KEEP_ASPECT_RATIO);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(90);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / 15.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate90Based5() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.KEEP_ASPECT_RATIO);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(90);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(45.0 / 10.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate90Based6() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(90);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoomX(45.0 / 10.0);
        expectBuilder.setZoomY(20.0 / 15.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate180() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(180);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoomX(20.0 / 10.0);
        expectBuilder.setZoomY(45.0 / 15.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate270() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(270);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoomX(45.0 / 10.0);
        expectBuilder.setZoomY(20.0 / 15.0);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate135() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(135);
        double rotate = rotateTransf.getRotateInRadians();
        double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / rotatedWidth);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate315() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(315);
        double rotate = rotateTransf.getRotateInRadians();
        double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / rotatedWidth);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotate225() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.FIT_WIDTH);
        BasicImageTransformations rotateTransf = newRotateDegTransformation(225);
        double rotate = rotateTransf.getRotateInRadians();
        double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / rotatedWidth);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotateBased1() {
        double rotate = Math.PI / 8.0;
        BasicImageTransformations expect = BasicImageTransformations.newRotateTransformation(rotate);
        BasicImageTransformations rotateTransf = BasicImageTransformations.newRotateTransformation(rotate);

        for (boolean fitWidth: Arrays.asList(false, true)) {
            for (boolean fitHeight: Arrays.asList(false, true)) {
                for (boolean keepAspect: Arrays.asList(false, true)) {
                    Set<ZoomToFitOption> options = EnumSet.noneOf(ZoomToFitOption.class);
                    if (fitWidth) options.add(ZoomToFitOption.FIT_WIDTH);
                    if (fitHeight) options.add(ZoomToFitOption.FIT_HEIGHT);
                    if (keepAspect) options.add(ZoomToFitOption.KEEP_ASPECT_RATIO);

                    BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                            10, 15, 20, 45, options,
                            BasicImageTransformations.newRotateTransformation(rotate));
                    assertCloseEnough("Error for ZoomToFit: " + options, expect, transformations);
                    assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
                }
            }
        }
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotateBased2() {
        Set<ZoomToFitOption> options = TYPICAL_OPTIONS_MAY_MAGNIFY;
        double rotate = Math.PI / 8.0;
        BasicImageTransformations rotateTransf = BasicImageTransformations.newRotateTransformation(rotate);
        double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / rotatedWidth);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotateBased3() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.FIT_HEIGHT);
        double rotate = Math.PI / 8.0;
        BasicImageTransformations rotateTransf = BasicImageTransformations.newRotateTransformation(rotate);
        double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(20.0 / rotatedWidth);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotateBased4() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.KEEP_ASPECT_RATIO);
        double rotate = Math.PI / 8.0;
        BasicImageTransformations rotateTransf = BasicImageTransformations.newRotateTransformation(rotate);
        double rotateHeight = getRotatedHeight(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(45.0 / rotateHeight);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsWidthZoomSmallerRotateBased5() {
        Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_HEIGHT);
        double rotate = Math.PI / 8.0;
        BasicImageTransformations rotateTransf = BasicImageTransformations.newRotateTransformation(rotate);
        double rotateHeight = getRotatedHeight(rotate, 10.0, 15.0);

        BasicImageTransformations.Builder expectBuilder = new BasicImageTransformations.Builder(rotateTransf);
        expectBuilder.setZoom(45.0 / rotateHeight);
        BasicImageTransformations expect = expectBuilder.create();

        BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                10, 15, 20, 45, options, rotateTransf);
        assertCloseEnough(expect, transformations);
        assertConversion(10, 15, 20, 45, options, rotateTransf, expect);
    }

    @Test
    public void testTransformationsHeightZoomSmallerRotateBased1() {
        double rotate = Math.PI / 8.0;
        BasicImageTransformations expect = BasicImageTransformations.newRotateTransformation(rotate);
        BasicImageTransformations rotateTransf = BasicImageTransformations.newRotateTransformation(rotate);

        for (boolean fitWidth: Arrays.asList(false, true)) {
            for (boolean fitHeight: Arrays.asList(false, true)) {
                for (boolean keepAspect: Arrays.asList(false, true)) {
                    Set<ZoomToFitOption> options = EnumSet.noneOf(ZoomToFitOption.class);
                    if (fitWidth) options.add(ZoomToFitOption.FIT_WIDTH);
                    if (fitHeight) options.add(ZoomToFitOption.FIT_HEIGHT);
                    if (keepAspect) options.add(ZoomToFitOption.KEEP_ASPECT_RATIO);

                    BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                            10, 15, 40, 30, options,
                            rotateTransf);
                    assertCloseEnough("Error for ZoomToFit: " + options, expect, transformations);
                    assertConversion(10, 15, 40, 30, options, rotateTransf, expect);
                }
            }
        }
    }

    @Test
    public void testTransformationsHeightZoomRotateBased2() {
        for (boolean flipH: Arrays.asList(false, true)) {
            for (boolean flipV: Arrays.asList(false, true)) {
                Set<ZoomToFitOption> options = TYPICAL_OPTIONS_MAY_MAGNIFY;
                double rotate = Math.PI / 8.0;
                BasicImageTransformations rotateTransf = newRotateTransformation(rotate, flipH, flipV);
                double rotatedHeight = getRotatedHeight(rotate, 10.0, 15.0);

                BasicImageTransformations.Builder expectBuilder
                        = new BasicImageTransformations.Builder(rotateTransf);
                expectBuilder.setZoom(30.0 / rotatedHeight);
                BasicImageTransformations expect = expectBuilder.create();

                BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                        10, 15, 40, 30, options, rotateTransf);
                assertCloseEnough(expect, transformations);
                assertConversion(10, 15, 40, 30, options, rotateTransf, expect);
            }
        }
    }

    @Test
    public void testTransformationsHeightZoomSmallerRotateBased3() {
        for (boolean flipH: Arrays.asList(false, true)) {
            for (boolean flipV: Arrays.asList(false, true)) {
                Set<ZoomToFitOption> options
                        = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.FIT_HEIGHT);
                double rotate = Math.PI / 8.0;
                BasicImageTransformations rotateTransf = newRotateTransformation(rotate, flipH, flipV);
                double rotatedHeight = getRotatedHeight(rotate, 10.0, 15.0);

                BasicImageTransformations.Builder expectBuilder
                        = new BasicImageTransformations.Builder(rotateTransf);
                expectBuilder.setZoom(30.0 / rotatedHeight);
                BasicImageTransformations expect = expectBuilder.create();

                BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                        10, 15, 40, 30, options, rotateTransf);
                assertCloseEnough(expect, transformations);
                assertConversion(10, 15, 40, 30, options, rotateTransf, expect);
            }
        }
    }

    @Test
    public void testTransformationsHeightZoomSmallerRotateBased4() {
        for (boolean flipH: Arrays.asList(false, true)) {
            for (boolean flipV: Arrays.asList(false, true)) {
                Set<ZoomToFitOption> options
                        = mayMagnify(ZoomToFitOption.FIT_WIDTH, ZoomToFitOption.KEEP_ASPECT_RATIO);
                double rotate = Math.PI / 8.0;
                BasicImageTransformations rotateTransf = newRotateTransformation(rotate, flipH, flipV);
                double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

                BasicImageTransformations.Builder expectBuilder
                        = new BasicImageTransformations.Builder(rotateTransf);
                expectBuilder.setZoom(40.0 / rotatedWidth);
                BasicImageTransformations expect = expectBuilder.create();

                BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                        10, 15, 40, 30, options, rotateTransf);
                assertCloseEnough(expect, transformations);
                assertConversion(10, 15, 40, 30, options, rotateTransf, expect);
            }
        }
    }

    @Test
    public void testTransformationsHeightZoomSmallerRotateBased5() {
        for (boolean flipH: Arrays.asList(false, true)) {
            for (boolean flipV: Arrays.asList(false, true)) {
                Set<ZoomToFitOption> options = mayMagnify(ZoomToFitOption.FIT_WIDTH);
                double rotate = Math.PI / 8.0;
                BasicImageTransformations rotateTransf = newRotateTransformation(rotate, flipH, flipV);
                double rotatedWidth = getRotatedWidth(rotate, 10.0, 15.0);

                BasicImageTransformations.Builder expectBuilder
                        = new BasicImageTransformations.Builder(rotateTransf);
                expectBuilder.setZoom(40.0 / rotatedWidth);
                BasicImageTransformations expect = expectBuilder.create();

                BasicImageTransformations transformations = ZoomToFitTransformer.getBasicTransformations(
                        10, 15, 40, 30, options, rotateTransf);
                assertCloseEnough(expect, transformations);
                assertConversion(10, 15, 40, 30, options, rotateTransf, expect);
            }
        }
    }

    /**
     * Test of toString method, of class ZoomToFitTransformer.
     */
    @Test
    public void testToString() {
        ZoomToFitTransformer transformer = create(
                BasicImageTransformations.newRotateTransformation(0.5),
                EnumSet.of(ZoomToFitOption.FIT_HEIGHT),
                Color.BLACK,
                InterpolationType.BICUBIC);
        assertNotNull(transformer.toString());
    }
}
