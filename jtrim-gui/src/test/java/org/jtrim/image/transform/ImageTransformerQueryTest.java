package org.jtrim.image.transform;

import java.util.List;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.AsyncDataConverter;
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
public class ImageTransformerQueryTest {
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
    public void testConversion1() {
        for (int numberOfTransformers = 1; numberOfTransformers < 4; numberOfTransformers++) {
            ImageTransformerLinkTest.TestCase inputs = new ImageTransformerLinkTest.TestCase(numberOfTransformers);
            inputs.runTest1(new ImageTransformerLinkTest.LinkFactory1() {
                @Override
                public ImageTransformerLink createLink(
                        ImageTransformerData input,
                        TaskExecutorService executor,
                        ImageTransformer[] transformers) {
                    ImageTransformerQuery query = new ImageTransformerQuery(executor, transformers);
                    assertNotNull(query.toString());
                    return query.createDataLink(input);
                }
            });
        }
    }

    @Test
    public void testConversion2() {
        for (int numberOfTransformers = 1; numberOfTransformers < 4; numberOfTransformers++) {
            ImageTransformerLinkTest.TestCase inputs = new ImageTransformerLinkTest.TestCase(numberOfTransformers);
            inputs.runTest2(new ImageTransformerLinkTest.LinkFactory2() {
                @Override
                public ImageTransformerLink createLink(
                        ImageTransformerData input,
                        List<AsyncDataConverter<ImageTransformerData, TransformedImage>> asyncConverters) {
                    ImageTransformerQuery query = new ImageTransformerQuery(asyncConverters);
                    assertNotNull(query.toString());
                    return query.createDataLink(input);
                }
            });
        }
    }
}