package org.jtrim2.image.transform;

import org.junit.Test;

import static org.junit.Assert.*;

public class InterpolationTypeTest {
    @Test
    public void testAutoGenerated() {
        for (InterpolationType state: InterpolationType.values()) {
            assertSame(state, InterpolationType.valueOf(state.toString()));
        }
    }
}
