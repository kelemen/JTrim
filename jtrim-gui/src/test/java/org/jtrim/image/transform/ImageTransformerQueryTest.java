package org.jtrim.image.transform;

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
            inputs.runTest1((input, executor, transformers) -> {
                ImageTransformerQuery query = new ImageTransformerQuery(executor, transformers);
                assertNotNull(query.toString());
                return query.createDataLink(input);
            });
        }
    }

    @Test
    public void testConversion2() {
        for (int numberOfTransformers = 1; numberOfTransformers < 4; numberOfTransformers++) {
            ImageTransformerLinkTest.TestCase inputs = new ImageTransformerLinkTest.TestCase(numberOfTransformers);
            inputs.runTest2((input, asyncConverters) -> {
                ImageTransformerQuery query = new ImageTransformerQuery(asyncConverters);
                assertNotNull(query.toString());
                return query.createDataLink(input);
            });
        }
    }
}
