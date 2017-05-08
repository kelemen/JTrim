package org.jtrim2.property;

import org.junit.Test;

import static org.junit.Assert.*;

public class NoOpVerifierTest {
    @Test
    public void testNotNull() {
        Object value = new Object();
        assertSame(value, NoOpVerifier.getInstance().storeValue(value));
    }

    @Test
    public void testNull() {
        assertNull(NoOpVerifier.getInstance().storeValue(null));
    }
}
