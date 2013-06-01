package org.jtrim.property;

import java.util.Arrays;
import org.jtrim.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class MemPropertyTest {
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
    private static PropertyVerifier<Object> mockVerifier() {
        return mock(PropertyVerifier.class);
    }

    @SuppressWarnings("unchecked")
    private static PropertyPublisher<Object> mockPublisher() {
        return mock(PropertyPublisher.class);
    }

    private static MemProperty<Object> createSimple(Object initialValue) {
        return new MemProperty<>(initialValue, NoOpVerifier.getInstance(), NoOpPublisher.getInstance());
    }

    @Test
    public void testInitialValue() {
        Object initialValue = new Object();
        MemProperty<Object> property = createSimple(initialValue);
        assertSame(initialValue, property.getValue());
    }

    @Test
    public void testSetValue() {
        MemProperty<Object> property = createSimple(new Object());

        Object newValue = new Object();
        property.setValue(newValue);
        assertSame(newValue, property.getValue());
    }

    @Test
    public void testListener() {
        MemProperty<Object> property = createSimple(new Object());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);
        assertTrue(listenerRef.isRegistered());
        verifyZeroInteractions(listener);

        property.setValue(new Object());
        verify(listener).run();

        property.setValue(new Object());
        verify(listener, times(2)).run();

        listenerRef.unregister();
        property.setValue(new Object());
        verify(listener, times(2)).run();
    }

    @Test
    public void testVerifier() {
        Object verified1 = new Object();
        Object verified2 = new Object();

        PropertyVerifier<Object> verifier = mockVerifier();
        stub(verifier.storeValue(any()))
                .toReturn(verified1)
                .toReturn(verified2)
                .toReturn(new Object());

        Object value1 = new Object();
        Object value2 = new Object();

        MemProperty<Object> property = new MemProperty<>(value1, verifier, NoOpPublisher.getInstance());
        verify(verifier).storeValue(any());
        assertSame(verified1, property.getValue());

        property.setValue(value2);
        assertSame(verified2, property.getValue());

        ArgumentCaptor<Object> verifierArgs = ArgumentCaptor.forClass(Object.class);
        verify(verifier, times(2)).storeValue(verifierArgs.capture());

        assertEquals(Arrays.asList(value1, value2), verifierArgs.getAllValues());
    }

    @Test
    public void testPublisher() {
        Object published1 = new Object();
        Object published2 = new Object();

        PropertyPublisher<Object> publisher = mockPublisher();
        stub(publisher.returnValue(any()))
                .toReturn(published1)
                .toReturn(published2)
                .toReturn(new Object());

        Object value1 = new Object();
        Object value2 = new Object();

        MemProperty<Object> property = new MemProperty<>(value1, NoOpVerifier.getInstance(), publisher);
        verifyZeroInteractions(publisher);

        assertSame(published1, property.getValue());
        verify(publisher).returnValue(any());

        property.setValue(value2);
        assertSame(published2, property.getValue());

        ArgumentCaptor<Object> publisherArgs = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).returnValue(publisherArgs.capture());

        assertEquals(Arrays.asList(value1, value2), publisherArgs.getAllValues());
    }
}