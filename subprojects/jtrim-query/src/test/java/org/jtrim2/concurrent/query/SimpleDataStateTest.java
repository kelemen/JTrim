package org.jtrim2.concurrent.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleDataStateTest {
    @Test
    public void testProperties() {
        String stateStr = "SimpleDataStateTest - STATE";
        double progress = 6547.43654;

        SimpleDataState state = new SimpleDataState(stateStr, progress);

        assertEquals(progress, state.getProgress(), 0.0);
        assertEquals(stateStr, state.getState());
        assertNotNull(state.toString());
    }
}
