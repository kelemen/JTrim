package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ConstSourceTest {
    @SuppressWarnings("unchecked")
    private static PropertyPublisher<Object> mockPublisher() {
        return mock(PropertyPublisher.class);
    }

    @Test
    public void testGetValue() {
        Object stored = new Object();
        Object returned = new Object();
        PropertyPublisher<Object> publisher = mockPublisher();
        stub(publisher.returnValue(same(stored))).toReturn(returned);

        ConstSource<Object> source = new ConstSource<>(stored, publisher);
        assertSame(returned, source.getValue());
    }

    @Test
    public void testNullValue() {
        ConstSource<Object> source = new ConstSource<>(null, NoOpPublisher.getInstance());
        assertNull(source.getValue());
    }

    @Test
    public void testListener() {
        ConstSource<Object> source = new ConstSource<>(new Object(), NoOpPublisher.getInstance());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = source.addChangeListener(listener);
        assertFalse(listenerRef.isRegistered());
        listenerRef.unregister();

        verifyZeroInteractions(listener);
    }
}
