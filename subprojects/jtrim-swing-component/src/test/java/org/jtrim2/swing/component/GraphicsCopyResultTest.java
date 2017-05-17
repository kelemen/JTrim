package org.jtrim2.swing.component;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphicsCopyResultTest {
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
