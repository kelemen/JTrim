package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
@SuppressWarnings("deprecation")
public class AffineImageTransformerTest {
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
                return AffineImageTransformer.getTransformationMatrix(transformations);
            }

            @Override
            public AffineTransform getTransformationMatrix(
                    BasicImageTransformations transformations,
                    double srcWidth,
                    double srcHeight,
                    double destWidth,
                    double destHeight) {

                return AffineImageTransformer.getTransformationMatrix(
                        transformations, srcWidth, srcHeight, destWidth, destHeight);
            }

            @Override
            public boolean isSimpleTransformation(BasicImageTransformations transformation) {
                return AffineImageTransformer.isSimpleTransformation(transformation);
            }
        });
    }

    @Test
    public void testGetTransformationMatrix_BasicImageTransformations_ImageTransformerData() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(-Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);

        int srcWidth = 20;
        int srcHeight = 30;
        BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
        ImageTransformerData transformerData = new ImageTransformerData(
                srcImage, 110, 120, new ImageMetaData(srcWidth, srcHeight, true));

        AffineTransform transf = AffineImageTransformer.getTransformationMatrix(
                builder.create(), transformerData);

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

        CommonAffineTransformationsTests.checkCloseEnough(transf, withSourceOffset);
    }

    private static void testConvertData(
            AffineImageTransformer transformer,
            ImageTransformerData input,
            ImagePointTransformer expectedPointTransformer) {

        TransformedImage transformedImage = transformer.convertData(input);
        assertEquals(input.getDestWidth(), transformedImage.getImage().getWidth());
        assertEquals(input.getDestHeight(), transformedImage.getImage().getHeight());
        ImagePointTransformer actualPointTransformer = transformedImage.getPointTransformer();

        PointTransformerChecks.checkEqualPointTransformers(expectedPointTransformer, actualPointTransformer);
    }

    @Test
    public void testConvertDataNullSourceNullMetaData() {
        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineImageTransformer transformer = new AffineImageTransformer(
                    BasicImageTransformations.newZoomTransformation(100.0, 100.0),
                    Color.GRAY,
                    interpolation);

            ImageTransformerData transfomerData = new ImageTransformerData(null, 100, 200, null);
            TransformedImage transformed = transformer.convertData(transfomerData);
            assertNull(transformed.getImage());
            PointTransformerChecks.checkEqualPointTransformers(
                    new AffineImagePointTransformer(new AffineTransform()),
                    transformed.getPointTransformer());
        }
    }

    @Test
    public void testInstance() {
        BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
        builder.setFlipHorizontal(true);
        builder.setFlipVertical(true);
        builder.setOffset(5.0, 6.0);
        builder.setRotateInRadians(Math.PI / 6.0);
        builder.setZoomX(3.0);
        builder.setZoomY(4.0);
        BasicImageTransformations transf = builder.create();

        for (InterpolationType interpolation: InterpolationType.values()) {
            AffineTransform affinTransf = AffineImageTransformer.getTransformationMatrix(transf);

            AffineImageTransformer transformer1
                    = new AffineImageTransformer(transf, Color.GRAY, interpolation);
            AffineImageTransformer transformer2
                    = new AffineImageTransformer(affinTransf, Color.GRAY, interpolation);
            // Test that it does not affect transformer2
            affinTransf.translate(1000.0, 1000.0);

            int srcWidth = 20;
            int srcHeight = 30;
            int destWidth = 110;
            int destHeight = 120;
            BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
            ImageTransformerData transformerData = new ImageTransformerData(
                    srcImage, destWidth, destHeight, new ImageMetaData(srcWidth, srcHeight, true));
            ImageTransformerData transformerDataWithoutImage = new ImageTransformerData(
                    null, destWidth, destHeight, new ImageMetaData(srcWidth, srcHeight, true));

            AffineTransform expectedTransf
                    = AffineImageTransformer.getTransformationMatrix(transf, transformerData);
            ImagePointTransformer expectedPointTransf
                    = new AffineImagePointTransformer(expectedTransf);

            testConvertData(transformer1, transformerData, expectedPointTransf);
            testConvertData(transformer2, transformerData, expectedPointTransf);
            PointTransformerChecks.checkEqualPointTransformers(expectedPointTransf,
                    transformer1.convertData(transformerDataWithoutImage).getPointTransformer());
            PointTransformerChecks.checkEqualPointTransformers(expectedPointTransf,
                    transformer2.convertData(transformerDataWithoutImage).getPointTransformer());

            assertNotNull(transformer1.toString());
            assertNotNull(transformer2.toString());
        }
    }
}
