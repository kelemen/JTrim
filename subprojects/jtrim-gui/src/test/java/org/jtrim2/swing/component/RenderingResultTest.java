package org.jtrim2.swing.component;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class RenderingResultTest {
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

    private static RenderingResult<Object> create(RenderingType type, Object result) {
        return new RenderingResult<>(type, result);
    }

    @Test
    public void testPropertiesSignificant() {
        for (Object result: Arrays.asList(null, new Object())) {
            RenderingResult<Object> renderingResult = create(
                    RenderingType.SIGNIFICANT_RENDERING, result);

            assertSame(result, renderingResult.getResult());
            assertEquals(RenderingType.SIGNIFICANT_RENDERING, renderingResult.getType());
            assertTrue(renderingResult.hasRendered());
            assertTrue(renderingResult.isSignificant());
        }
    }

    @Test
    public void testPropertiesInsignificant() {
        for (Object result: Arrays.asList(null, new Object())) {
            RenderingResult<Object> renderingResult = create(
                    RenderingType.INSIGNIFICANT_RENDERING, result);

            assertSame(result, renderingResult.getResult());
            assertEquals(RenderingType.INSIGNIFICANT_RENDERING, renderingResult.getType());
            assertTrue(renderingResult.hasRendered());
            assertFalse(renderingResult.isSignificant());
        }
    }

    @Test
    public void testPropertiesNoRendering() {
        RenderingResult<Object> renderingResult = create(
                RenderingType.NO_RENDERING, null);

        assertNull(renderingResult.getResult());
        assertEquals(RenderingType.NO_RENDERING, renderingResult.getType());
        assertFalse(renderingResult.hasRendered());
        assertFalse(renderingResult.isSignificant());
    }

    @Test
    public void testFactorySignificant() {
        for (Object result: Arrays.asList(null, new Object())) {
            RenderingResult<Object> renderingResult = RenderingResult.significant(result);

            assertSame(result, renderingResult.getResult());
            assertEquals(RenderingType.SIGNIFICANT_RENDERING, renderingResult.getType());
            assertTrue(renderingResult.hasRendered());
            assertTrue(renderingResult.isSignificant());
        }
    }

    @Test
    public void testFactoryInsignificant() {
        for (Object result: Arrays.asList(null, new Object())) {
            RenderingResult<Object> renderingResult = RenderingResult.insignificant(result);

            assertSame(result, renderingResult.getResult());
            assertEquals(RenderingType.INSIGNIFICANT_RENDERING, renderingResult.getType());
            assertTrue(renderingResult.hasRendered());
            assertFalse(renderingResult.isSignificant());
        }
    }

    @Test
    public void testFactoryNoRendering() {
        RenderingResult<Object> renderingResult = RenderingResult.noRendering();

        assertNull(renderingResult.getResult());
        assertEquals(RenderingType.NO_RENDERING, renderingResult.getType());
        assertFalse(renderingResult.hasRendered());
        assertFalse(renderingResult.isSignificant());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoRenderingWithResult() {
        create(RenderingType.NO_RENDERING, new Object());
    }

    @Test(expected = NullPointerException.class)
    public void testNullType() {
        create(null, new Object());
    }

    @Test(expected = NullPointerException.class)
    public void testNullTypeNullResult() {
        create(null, null);
    }
}
