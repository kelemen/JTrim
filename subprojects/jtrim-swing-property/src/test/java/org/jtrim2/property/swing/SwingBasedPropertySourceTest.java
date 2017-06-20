package org.jtrim2.property.swing;

import java.beans.PropertyChangeListener;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SwingBasedPropertySourceTest {
    private static PropertySource<Object> create(SwingPropertySource<Object, PropertyChangeListener> wrapped) {
        return new SwingBasedPropertySource<>(wrapped, TestSwingProperty.listenerForwarder());
    }

    /**
     * Test of getValue method, of class SwingBasedPropertySource.
     */
    @Test
    public void testGetValue() {
        Object initialValue = new Object();
        TestSwingProperty wrapped = new TestSwingProperty(initialValue);
        PropertySource<Object> property = create(wrapped);

        assertSame(initialValue, property.getValue());

        Object value1 = new Object();
        wrapped.setValue(value1);
        assertSame(value1, property.getValue());
    }

    @Test
    public void testSingleListener() {
        TestSwingProperty wrapped = new TestSwingProperty(new Object());
        PropertySource<Object> property = create(wrapped);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyZeroInteractions(listener);

        wrapped.setValue(wrapped);
        verify(listener).run();

        listenerRef.unregister();

        wrapped.setValue(wrapped);
        verifyNoMoreInteractions(listener);

        // Unregister again and verify that nothing bad happens
        listenerRef.unregister();

        wrapped.setValue(wrapped);
        verifyNoMoreInteractions(listener);
    }

    private void doTestConcurrentAddAndRemoveListeners(int threadCount) {
        TestSwingProperty wrapped = new TestSwingProperty(new Object());
        final PropertySource<Object> property = create(wrapped);

        Runnable[] listeners = new Runnable[threadCount];
        Runnable[] addListenerTasks = new Runnable[threadCount];
        final ListenerRef[] listenerRefs = new ListenerRef[threadCount];
        for (int i = 0; i < listeners.length; i++) {
            final Runnable listener = mock(Runnable.class);
            listeners[i] = listener;

            final int index = i;
            addListenerTasks[i] = () -> {
                listenerRefs[index] = property.addChangeListener(listener);
            };
        }

        Tasks.runConcurrently(addListenerTasks);
        verifyZeroInteractions((Object[]) listeners);

        wrapped.setValue(new Object());

        for (Runnable listener: listeners) {
            verify(listener).run();
        }

        Runnable[] removeListenerTasks = new Runnable[threadCount];
        for (int i = 0; i < listeners.length; i++) {
            final ListenerRef listenerRef = listenerRefs[i];
            removeListenerTasks[i] = listenerRef::unregister;
        }

        Tasks.runConcurrently(removeListenerTasks);
        wrapped.setValue(new Object());
        verifyNoMoreInteractions((Object[]) listeners);
    }

    @Test
    public void testConcurrentAddAndRemoveListeners() {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            doTestConcurrentAddAndRemoveListeners(threadCount);
        }
    }
}
