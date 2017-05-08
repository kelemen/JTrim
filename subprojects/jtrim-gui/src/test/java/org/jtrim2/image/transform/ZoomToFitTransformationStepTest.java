package org.jtrim2.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Set;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.image.ImageTestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ZoomToFitTransformationStepTest {
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
    public void doCommonTests() {
        CommonZoomToFitTransformationsTests tests;
        tests = new CommonZoomToFitTransformationsTests(new CommonZoomToFitTransformations() {
            @Override
            public BasicImageTransformations getBasicTransformations(
                    int inputWidth,
                    int inputHeight,
                    int destWidth,
                    int destHeight,
                    Set<ZoomToFitOption> options,
                    BasicImageTransformations transBase) {
                return ZoomToFitTransformationStep.getBasicTransformations(
                        inputWidth, inputHeight, destWidth, destHeight, options, transBase);
            }

            @Override
            public TransformedImage doTransform(
                    BufferedImage srcImage,
                    int destWidth,
                    int destHeight,
                    Set<ZoomToFitOption> options,
                    BasicImageTransformations transBase,
                    InterpolationType interpolationType) {
                ZoomToFitTransformationStep transfomer = new ZoomToFitTransformationStep(
                        transBase, options, Color.GRAY, interpolationType);

                TransformationStepInput input;
                input = new TransformationStepInput(
                        null, destWidth, destHeight, new TransformedImage(srcImage, null));
                return transfomer.render(Cancellation.UNCANCELABLE_TOKEN, input, null);
            }
        });
        tests.doAllTests();
    }

    private static Set<ZoomToFitOption> fullZoomOptions() {
        return EnumSet.of(
                ZoomToFitOption.MAY_MAGNIFY,
                ZoomToFitOption.FIT_HEIGHT,
                ZoomToFitOption.FIT_WIDTH);
    }

    @Test
    public void testRenderNullInputProperties() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            ZoomToFitTransformationStep transformer = new ZoomToFitTransformationStep(
                    BasicImageTransformations.identityTransformation(),
                    fullZoomOptions(),
                    Color.GRAY,
                    interpolation);


            TransformationStepInput input = new TransformationStepInput(
                    null, 100, 100, TransformedImage.NULL_IMAGE);
            TransformedImage result = transformer.render(Cancellation.UNCANCELABLE_TOKEN, input, null);

            assertNull(result.getImage());
            PointTransformerChecks.checkEqualPointTransformers(AffineImagePointTransformer.IDENTITY,
                    result.getPointTransformer());
        }
    }

    private static BufferedImage blankImage(Color color, int width, int height, int type) {
        BufferedImage result = new BufferedImage(width, height, type);
        ImageTestUtils.fillImage(result, color);
        return result;
    }

    private static BufferedImage blankImage(Color color) {
        return blankImage(color, 3, 4, BufferedImage.TYPE_INT_ARGB);
    }

    private static AffineTransform nonIdentityAffineTransform() {
        AffineTransform transf = new AffineTransform();
        transf.translate(3.0, 5.0);
        transf.rotate(2.0);
        transf.scale(6.0, 7.0);
        transf.shear(3.5, 4.5);
        return transf;
    }

    private static ImagePointTransformer nonIdentityPointTransformer() {
        return new AffineImagePointTransformer(nonIdentityAffineTransform());
    }

    private static TransformedImage blankTransformedImage(Color color) {
        return new TransformedImage(blankImage(color), nonIdentityPointTransformer());
    }

    @Test
    public void testRenderWithZeroOutputDim() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            ZoomToFitTransformationStep transformer = new ZoomToFitTransformationStep(
                    BasicImageTransformations.identityTransformation(),
                    fullZoomOptions(),
                    Color.GRAY,
                    interpolation);

            TransformationStepInput input = new TransformationStepInput(
                    null, 0, 0, blankTransformedImage(Color.BLUE));
            TransformedImage result
                    = transformer.render(Cancellation.UNCANCELABLE_TOKEN, input, null);

            assertNull(result.getImage());
            PointTransformerChecks.checkEqualPointTransformers(AffineImagePointTransformer.IDENTITY,
                    result.getPointTransformer());
        }
    }

    @Test
    public void testRenderUsingOffered() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            ZoomToFitTransformationStep transformer = new ZoomToFitTransformationStep(
                    BasicImageTransformations.identityTransformation(),
                    fullZoomOptions(),
                    Color.GRAY,
                    interpolation);

            TransformedImage inputImage = blankTransformedImage(Color.BLUE);
            int width = inputImage.getImage().getWidth();
            int height = inputImage.getImage().getHeight();
            BufferedImage offered = blankImage(Color.GREEN);

            TransformationStepInput input = new TransformationStepInput(
                    null, width, height, inputImage);
            TransformedImage result
                    = transformer.render(Cancellation.UNCANCELABLE_TOKEN, input, offered);
            assertSame(offered, result.getImage());

            ImageTestUtils.checkBlankImage(offered, Color.BLUE);

            PointTransformerChecks.checkEqualPointTransformers(AffineImagePointTransformer.IDENTITY,
                    result.getPointTransformer());
        }
    }

    /**
     * Test of toString method, of class ZoomToFitTransformationStep.
     */
    @Test
    public void testToString() {
        ZoomToFitTransformationStep transformer = new ZoomToFitTransformationStep(
                BasicImageTransformations.newRotateTransformation(0.5),
                EnumSet.of(ZoomToFitOption.FIT_HEIGHT),
                Color.BLACK,
                InterpolationType.BICUBIC);
        assertNotNull(transformer.toString());
    }
}
