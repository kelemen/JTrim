package org.jtrim2.swing.component;

import org.junit.Test;

import static org.junit.Assert.*;

public class RenderingTypeTest {
    @Test
    public void testAutoGenerated() {
        for (RenderingType state: RenderingType.values()) {
            assertSame(state, RenderingType.valueOf(state.toString()));
        }
    }
}
