package org.jtrim2.property;

import org.junit.Test;

import static org.junit.Assert.*;

public class NoOpPublisherTest {
    @Test
    public void testNotNull() {
        Object value = new Object();
        assertSame(value, NoOpPublisher.getInstance().returnValue(value));
    }

    @Test
    public void testNull() {
        assertNull(NoOpPublisher.getInstance().returnValue(null));
    }
}
