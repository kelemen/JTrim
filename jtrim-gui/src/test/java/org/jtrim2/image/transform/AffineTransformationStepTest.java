package org.jtrim2.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.image.ImageTestUtils;
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
public class AffineTransformationStepTest {
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
    public void testCommonStaticMethods() {
        CommonAffineTransformationsTests.testCommonTransformations(new CommonAffineTransformations() {
            @Override
            public AffineTransform getTransformationMatrix(
                    BasicImageTransformations transformations) {
                return AffineTransformationStep.getTransformationMatrix(transformations);
            }

            @Override
            public AffineTransform getTransformationMatrix(
                    BasicImageTransformations transformations,
                    double srcWidth,
                    double srcHeight,
                    double destWidth,
                    double destHeight) {

                return AffineTransformationStep.getTransformationMatrix(
                        transformations, srcWidth, srcHeight, destWidth, destHeight);
            }

            @Override
            public boolean isSimpleTransformation(BasicImageTransformations transformation) {
                return AffineTransformationStep.isSimpleTransformation(transformation);
            }
        });
    }

    @Test
    public void testRenderNullInputProperties() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransformationStep transformer = new AffineTransformationStep(
                    BasicImageTransformations.newZoomTransformation(100.0, 100.0),
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

    private static TransformedImage blankTransformedImage(Color color, int width, int height, int type) {
        return new TransformedImage(blankImage(color, width, height, type),
                nonIdentityPointTransformer());
    }

    @Test
    public void testRenderWithZeroOutputDim() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransformationStep transformer = new AffineTransformationStep(
                    BasicImageTransformations.newZoomTransformation(100.0, 100.0),
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
            AffineTransformationStep transformer = new AffineTransformationStep(
                    BasicImageTransformations.identityTransformation(),
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

    public void testRenderNotUsingOffered(BufferedImage offered, int width, int height, int type) {
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransformationStep transformer = new AffineTransformationStep(
                    BasicImageTransformations.identityTransformation(),
                    Color.GRAY,
                    interpolation);

            TransformedImage inputImage = blankTransformedImage(Color.BLUE, width, height, type);

            TransformationStepInput input = new TransformationStepInput(
                    null, width, height, inputImage);
            TransformedImage result
                    = transformer.render(Cancellation.UNCANCELABLE_TOKEN, input, offered);
            assertNotSame(offered, result.getImage());

            ImageTestUtils.checkBlankImage(result.getImage(), Color.BLUE);

            PointTransformerChecks.checkEqualPointTransformers(AffineImagePointTransformer.IDENTITY,
                    result.getPointTransformer());
        }
    }

    private static void testConvertData(
            AffineTransformationStep transformer,
            TransformationStepInput input,
            ImagePointTransformer expectedPointTransformer) {

        TransformedImage transformedImage = transformer.render(
                Cancellation.UNCANCELABLE_TOKEN, input, null);

        assertEquals(input.getDestinationWidth(), transformedImage.getImage().getWidth());
        assertEquals(input.getDestinationHeight(), transformedImage.getImage().getHeight());

        ImagePointTransformer actualPointTransformer = transformedImage.getPointTransformer();
        PointTransformerChecks.checkEqualPointTransformers(expectedPointTransformer, actualPointTransformer);
    }

    @Test
    public void testRender() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);
        BasicImageTransformations transf = builder.create();

        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransform affinTransf = AffineTransformationStep.getTransformationMatrix(transf);

            AffineTransformationStep transformer1
                    = new AffineTransformationStep(transf, Color.GRAY, interpolation);
            AffineTransformationStep transformer2
                    = new AffineTransformationStep(affinTransf, Color.GRAY, interpolation);
            // Test that it does not affect transformer2
            affinTransf.translate(1000.0, 1000.0);

            int srcWidth = 20;
            int srcHeight = 30;
            int destWidth = 110;
            int destHeight = 120;
            BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
            TransformedImage inputImage = new TransformedImage(srcImage, null);
            TransformationStepInput input = new TransformationStepInput(
                    null, destWidth, destHeight, inputImage);

            AffineTransform expectedTransf = AffineTransformationStep.getTransformationMatrix(
                    transf, srcWidth, srcHeight, destWidth, destHeight);
            ImagePointTransformer expectedPointTransf
                    = new AffineImagePointTransformer(expectedTransf);

            testConvertData(transformer1, input, expectedPointTransf);
            testConvertData(transformer2, input, expectedPointTransf);

            assertNotNull(transformer1.toString());
            assertNotNull(transformer2.toString());
        }
    }

    @Test
    public void testProperties1() {
        Color bckgColor = new Color(1, 2, 3, 4);
        AffineTransform expectedAffine = nonIdentityAffineTransform();
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransform affine = new AffineTransform(expectedAffine);
            AffineTransformationStep transf = new AffineTransformationStep(affine, bckgColor, interpolation);

            assertSame(bckgColor, transf.getBackgroundColor());
            assertSame(interpolation, transf.getInterpolationType());
            PointTransformerChecks.checkEqualPointTransformers(
                    new AffineImagePointTransformer(expectedAffine),
                    new AffineImagePointTransformer(transf.getTransformations()));
        }
    }

    @Test
    public void testProperties2() {
        Color bckgColor = new Color(1, 2, 3, 4);
        BasicImageTransformations affine = BasicImageTransformations.newRotateTransformation(3.0);
        AffineTransform expectedAffine = AffineTransformationStep.getTransformationMatrix(affine);
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransformationStep transf = new AffineTransformationStep(affine, bckgColor, interpolation);

            assertSame(bckgColor, transf.getBackgroundColor());
            assertSame(interpolation, transf.getInterpolationType());
            PointTransformerChecks.checkEqualPointTransformers(
                    new AffineImagePointTransformer(expectedAffine),
                    new AffineImagePointTransformer(transf.getTransformations()));
        }
    }
}
