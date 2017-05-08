package org.jtrim2.property.swing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.EventListeners;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StandardBasedSwingPropertySourceTest {
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

    private static SwingPropertySource<Object, Runnable> create(PropertySource<?> wrapped) {
        return new StandardBasedSwingPropertySource<>(wrapped, EventListeners.runnableDispatcher());
    }

    @Test
    public void testGetValue() {
        Object initialValue = new Object();
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(initialValue);
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        assertSame(initialValue, property.getValue());

        Object newValue = new Object();
        wrapped.setValue(newValue);
        assertSame(newValue, property.getValue());
    }

    @Test
    public void testAddListenerOnce() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);

        verifyZeroInteractions(listener);
        wrapped.setValue(new Object());
        verify(listener).run();

        property.removeChangeListener(listener);
        wrapped.setValue(new Object());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAddAndRemoveNullListener() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        // Just add one normal to verify that the property did not become broken.
        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        property.addChangeListener(null);

        wrapped.setValue(new Object());
        verify(listener).run();

        property.removeChangeListener(null);
        wrapped.setValue(new Object());
        verify(listener, times(2)).run();
    }

    @Test
    public void testAddListenerMultipleTimes() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        property.addChangeListener(listener);

        verifyZeroInteractions(listener);
        wrapped.setValue(new Object());
        verify(listener, times(2)).run();

        property.removeChangeListener(listener);
        wrapped.setValue(new Object());
        verify(listener, times(3)).run();

        property.removeChangeListener(listener);
        wrapped.setValue(new Object());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAddMultipleListeners() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);
        property.addChangeListener(listener1);
        property.addChangeListener(listener2);

        verifyZeroInteractions(listener1, listener2);
        wrapped.setValue(new Object());
        verify(listener1).run();
        verify(listener2).run();

        property.removeChangeListener(listener1);
        wrapped.setValue(new Object());
        verifyNoMoreInteractions(listener1);
        verify(listener2, times(2)).run();

        property.removeChangeListener(listener2);
        wrapped.setValue(new Object());
        verifyNoMoreInteractions(listener1, listener2);
    }

    @Test
    public void testRemoveListenerMoreThanItWasAddedAndReAdd() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        property.addChangeListener(listener);

        property.removeChangeListener(listener);
        property.removeChangeListener(listener);
        property.removeChangeListener(listener);

        wrapped.setValue(new Object());
        verifyZeroInteractions(listener);

        property.addChangeListener(listener);
        wrapped.setValue(new Object());
        verify(listener).run();
    }

    private Set<Throwable> toSet(Throwable... errors) {
        Set<Throwable> result = new HashSet<>();
        result.addAll(Arrays.asList(errors));
        return result;
    }

    @Test
    public void testBuggyListeners() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        SwingPropertySource<Object, Runnable> property = create(wrapped);

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);
        Runnable listener3 = mock(Runnable.class);

        property.addChangeListener(listener1);
        property.addChangeListener(listener2);
        property.addChangeListener(listener3);

        TestException exception1 = new TestException();
        TestException exception2 = new TestException();

        doThrow(exception1).when(listener1).run();
        doThrow(exception2).when(listener2).run();

        String loggerName = "org.jtrim2.property.swing.StandardBasedSwingPropertySource";
        try (LogCollector logs = LogCollector.startCollecting(loggerName)) {
            wrapped.setValue(new Object());

            Throwable[] exceptions = logs.getExceptions(Level.SEVERE);
            assertEquals(toSet(exception1, exception2), toSet(exceptions));
        }
    }

    private void doTestConcurrentAddAndRemoveListeners(int threadCount) {
        final MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        final SwingPropertySource<Object, Runnable> property = create(wrapped);

        Runnable[] listeners = new Runnable[threadCount];
        Runnable[] addListenerTasks = new Runnable[threadCount];
        for (int i = 0; i < listeners.length; i++) {
            final Runnable listener = mock(Runnable.class);
            listeners[i] = listener;

            addListenerTasks[i] = () -> {
                property.addChangeListener(listener);
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
            final Runnable listener = listeners[i];

            removeListenerTasks[i] = () -> {
                property.removeChangeListener(listener);
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

    @SuppressWarnings("serial")
    private static class TestException extends RuntimeException {
    }
}
