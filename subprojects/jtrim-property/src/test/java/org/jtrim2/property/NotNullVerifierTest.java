package org.jtrim2.property;

import org.junit.Test;

import static org.junit.Assert.*;

public class NotNullVerifierTest {
    @Test
    public void testNotNull() {
        Object value = new Object();
        assertSame(value, NotNullVerifier.getInstance().storeValue(value));
    }

    @Test(expected = NullPointerException.class)
    public void testNull() {
        NotNullVerifier.getInstance().storeValue(null);
    }
}
