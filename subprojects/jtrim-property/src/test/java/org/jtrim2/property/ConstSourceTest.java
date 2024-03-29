package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.junit.Assert.*;
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
        when(publisher.returnValue(same(stored))).thenReturn(returned);

        ConstSource<Object> source = new ConstSource<>(stored, publisher);
        assertSame(returned, source.getValue());
    }

    @Test
    public void testNullValue() {
        ConstSource<Object> source = new ConstSource<>(null, PropertyFactory.noOpPublisher());
        assertNull(source.getValue());
    }

    @Test
    public void testListener() {
        ConstSource<Object> source = new ConstSource<>(new Object(), PropertyFactory.noOpPublisher());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = source.addChangeListener(listener);
        listenerRef.unregister();

        verifyNoInteractions(listener);
    }
}
