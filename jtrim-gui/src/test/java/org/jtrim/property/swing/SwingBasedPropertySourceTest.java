package org.jtrim.property.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class SwingBasedPropertySourceTest {
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

    private static PropertySource<Object> create(SwingPropertySource<Object, PropertyChangeListener> wrapped) {
        return new SwingBasedPropertySource<>(wrapped, RunnableForwarder.INSTANCE);
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
        assertTrue(listenerRef.isRegistered());

        verifyZeroInteractions(listener);

        wrapped.setValue(wrapped);
        verify(listener).run();

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());

        wrapped.setValue(wrapped);
        verifyNoMoreInteractions(listener);

        // Unregister again and verify that nothing bad happens
        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());

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
            addListenerTasks[i] = new Runnable() {
                @Override
                public void run() {
                    listenerRefs[index] = property.addChangeListener(listener);
                }
            };
        }

        Tasks.runConcurrently(addListenerTasks);
        verifyZeroInteractions((Object[])listeners);

        wrapped.setValue(new Object());

        for (int i = 0; i < listeners.length; i++) {
            verify(listeners[i]).run();
        }

        Runnable[] removeListenerTasks = new Runnable[threadCount];
        for (int i = 0; i < listeners.length; i++) {
            final ListenerRef listenerRef = listenerRefs[i];
            removeListenerTasks[i] = new Runnable() {
                @Override
                public void run() {
                    listenerRef.unregister();
                }
            };
        }

        Tasks.runConcurrently(removeListenerTasks);
        wrapped.setValue(new Object());
        verifyNoMoreInteractions((Object[])listeners);
    }

    @Test
    public void testConcurrentAddAndRemoveListeners() {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            doTestConcurrentAddAndRemoveListeners(threadCount);
        }
    }

    private enum RunnableForwarder implements SwingForwarderFactory<PropertyChangeListener> {
        INSTANCE;

        @Override
        public PropertyChangeListener createForwarder(final Runnable listener) {
            assert listener != null;

            return new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    listener.run();
                }
            };
        }
    }

    private static final class TestSwingProperty
    implements
            SwingPropertySource<Object, PropertyChangeListener> {

        private final PropertyChangeSupport listeners;
        private Object value;

        public TestSwingProperty(Object initialValue) {
            this.listeners = new PropertyChangeSupport(this);
            this.value = initialValue;
        }

        public void setValue(Object newValue) {
            this.value = newValue;
            listeners.firePropertyChange("value", null, null);
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void addChangeListener(PropertyChangeListener listener) {
            listeners.addPropertyChangeListener(listener);
        }

        @Override
        public void removeChangeListener(PropertyChangeListener listener) {
            listeners.removePropertyChangeListener(listener);
        }
    }
}
