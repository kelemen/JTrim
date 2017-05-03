package org.jtrim2.image.transform;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Set;
import org.jtrim2.image.ImageMetaData;
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
public class ZoomToFitTransformerTest {
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
                return ZoomToFitTransformer.getBasicTransformations(
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
                ZoomToFitTransformer transfomer = new ZoomToFitTransformer(
                        transBase, options, Color.GRAY, interpolationType);

                ImageMetaData metaData = new ImageMetaData(
                        srcImage.getWidth(), srcImage.getHeight(), true);
                ImageTransformerData input = new ImageTransformerData(
                        srcImage, destWidth, destHeight, metaData);

                return transfomer.convertData(input);
            }
        });
        tests.doAllTests();
    }

    /**
     * Test of toString method, of class ZoomToFitTransformer.
     */
    @Test
    public void testToString() {
        ZoomToFitTransformer transformer = new ZoomToFitTransformer(
                BasicImageTransformations.newRotateTransformation(0.5),
                EnumSet.of(ZoomToFitOption.FIT_HEIGHT),
                Color.BLACK,
                InterpolationType.BICUBIC);
        assertNotNull(transformer.toString());
    }
}
