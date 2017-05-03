package org.jtrim2.swing.concurrent.async;

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
public class GraphicsCopyResultTest {
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

    /**
     * Test of getInstance method, of class GraphicsCopyResult.
     */
    @Test
    public void testProperties() {
        for (Object result: Arrays.asList(null, new Object())) {
            for (boolean painted: Arrays.asList(false, true)) {
                GraphicsCopyResult<Object> instance = GraphicsCopyResult.getInstance(painted, result);
                assertEquals(painted, instance.isPainted());
                assertSame(result, instance.getPaintResult());
            }
        }
    }
}
