package org.jtrim2.event;

import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class EventListenersTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(EventListeners.class);
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
