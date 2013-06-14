package org.jtrim.event;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class EventListenersTest {
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

    /**
     * Test of runnableDispatcher method, of class EventListeners.
     */
    @Test
    public void testRunnableDispatcher() {
        Runnable listener = mock(Runnable.class);
        EventListeners.runnableDispatcher().onEvent(listener, null);
        verify(listener).run();
    }

    /**
     * Test of dispatchRunnable method, of class EventListeners.
     */
    @Test
    public void testDispatchRunnable1() {
        Runnable listener = mock(Runnable.class);
        ListenerManager<Runnable> listeners = new CopyOnTriggerListenerManager<>();
        listeners.registerListener(listener);

        EventListeners.dispatchRunnable(listeners);

        verify(listener).run();
    }

    @Test
    public void testDispatchRunnable2() {
        Runnable listener = mock(Runnable.class);
        OneShotListenerManager<Runnable, Void> listeners = new OneShotListenerManager<>();
        listeners.registerListener(listener);

        EventListeners.dispatchRunnable(listeners);

        verify(listener).run();
    }
}
