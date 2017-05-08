package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelegatedPropertySourceTest {
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

    @SuppressWarnings("unchecked")
    private static PropertySource<Object> mockProperty() {
        return mock(PropertySource.class);
    }

    /**
     * Test of getValue method, of class DelegatedPropertySource.
     */
    @Test
    public void testGetValue() {
        PropertySource<Object> wrapped = mockProperty();
        DelegatedPropertySource<Object> delegated = new DelegatedPropertySource<>(wrapped);

        Object value = new Object();
        stub(wrapped.getValue()).toReturn(value);

        assertSame(value, delegated.getValue());
        verify(wrapped).getValue();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of addChangeListener method, of class DelegatedPropertySource.
     */
    @Test
    public void testAddChangeListener() {
        PropertySource<Object> wrapped = mockProperty();
        DelegatedPropertySource<Object> delegated = new DelegatedPropertySource<>(wrapped);

        ListenerRef listenerRef = mock(ListenerRef.class);
        stub(wrapped.addChangeListener(any(Runnable.class))).toReturn(listenerRef);

        Runnable listener = mock(Runnable.class);
        assertSame(listenerRef, delegated.addChangeListener(listener));
        verify(wrapped).addChangeListener(same(listener));
        verifyNoMoreInteractions(wrapped);
    }
}
