package org.jtrim.swing.component;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageResult;
import org.jtrim.image.transform.TransformedImage;
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
public class CachingImageTransformationStepTest {
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

    private static ImageResult createSource() {
        return new ImageResult(
                new BufferedImage(2, 3, BufferedImage.TYPE_INT_ARGB),
                new ImageMetaData(2, 3, true));
    }

    private static ImageResult createEmptySource() {
        return new ImageResult(null, null);
    }

    private static TransformedImage createTransformedImage() {
        return new TransformedImage(new BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB), null);
    }

    private static TransformedImage createEmptyTransformedImage() {
        return new TransformedImage(null, null);
    }

    private static void testHardRefCache(TransformationStepInput input) {
        TransformedImage output = createEmptyTransformedImage();
        ImageTransformationStep wrappedTransformation = mock(ImageTransformationStep.class);
        stub(wrappedTransformation.render(
                any(CancellationToken.class),
                any(TransformationStepInput.class),
                any(BufferedImage.class))).toReturn(output);

        TransformationStepInput.Cmp cmp = mock(TransformationStepInput.Cmp.class);
        stub(cmp.isSameInput(any(TransformationStepInput.class), any(TransformationStepInput.class)))
                .toReturn(true)
                .toReturn(false);

        CachingImageTransformationStep step
                = new CachingImageTransformationStep(ReferenceType.HardRefType, wrappedTransformation, cmp);

        CancellationToken cancelToken = mock(CancellationToken.class);
        BufferedImage offered = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        TransformedImage result1 = step.render(cancelToken, input, offered);
        assertSame(output, result1);
        verify(wrappedTransformation).render(cancelToken, input, offered);

        TransformedImage result2 = step.render(cancelToken, input, offered);
        assertSame(output, result2);
        verify(wrappedTransformation).render(cancelToken, input, offered);

        TransformedImage result3 = step.render(cancelToken, input, offered);
        assertSame(output, result3);
        verify(wrappedTransformation, times(2)).render(cancelToken, input, offered);
    }

    @Test
    public void testHardRefCache() {
        for (ImageResult result: Arrays.asList(null, createEmptySource(), createSource())) {
            for (TransformedImage image: Arrays.asList(createEmptyTransformedImage(), createTransformedImage())) {
                for (int width: Arrays.asList(0, 10)) {
                    for (int height: Arrays.asList(0, 5)) {
                        TransformationStepInput input =
                                new TransformationStepInput(result, width, height, image);
                        testHardRefCache(input);
                    }
                }
            }
        }
    }

    private static void testNoRefCache(TransformationStepInput input) {
        TransformedImage output = createEmptyTransformedImage();
        ImageTransformationStep wrappedTransformation = mock(ImageTransformationStep.class);
        stub(wrappedTransformation.render(
                any(CancellationToken.class),
                any(TransformationStepInput.class),
                any(BufferedImage.class))).toReturn(output);

        TransformationStepInput.Cmp cmp = mock(TransformationStepInput.Cmp.class);
        stub(cmp.isSameInput(any(TransformationStepInput.class), any(TransformationStepInput.class)))
                .toReturn(true)
                .toReturn(false);

        CachingImageTransformationStep step
                = new CachingImageTransformationStep(ReferenceType.NoRefType, wrappedTransformation, cmp);

        CancellationToken cancelToken = mock(CancellationToken.class);
        BufferedImage offered = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        TransformedImage result1 = step.render(cancelToken, input, offered);
        assertSame(output, result1);
        verify(wrappedTransformation).render(cancelToken, input, offered);

        TransformedImage result2 = step.render(cancelToken, input, offered);
        assertSame(output, result2);
        verify(wrappedTransformation,times(2)).render(cancelToken, input, offered);

        TransformedImage result3 = step.render(cancelToken, input, offered);
        assertSame(output, result3);
        verify(wrappedTransformation, times(3)).render(cancelToken, input, offered);
    }

    @Test
    public void testNoRefCache() {
        for (ImageResult result: Arrays.asList(null, createEmptySource(), createSource())) {
            for (TransformedImage image: Arrays.asList(createEmptyTransformedImage(), createTransformedImage())) {
                for (int width: Arrays.asList(0, 10)) {
                    for (int height: Arrays.asList(0, 5)) {
                        TransformationStepInput input =
                                new TransformationStepInput(result, width, height, image);
                        testNoRefCache(input);
                    }
                }
            }
        }
    }
}
