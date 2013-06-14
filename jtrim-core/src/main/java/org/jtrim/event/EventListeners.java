package org.jtrim.event;

/**
 * Contains static utility methods for listener dispatching.
 *
 * @author Kelemen Attila
 */
public final class EventListeners {
    /**
     * Returns an {@code EventDispatcher} which will simply call {@code run()}
     * method of the listeners passed to the {@code EventDispatcher}.
     *
     * @return an {@code EventDispatcher} which will simply call {@code run()}
     *   method of the listeners passed to the {@code EventDispatcher}. This
     *   method never returns {@code null}.
     */
    public static EventDispatcher<Runnable, Void> runnableDispatcher() {
        return RunnableDispatcher.INSTANCE;
    }

    /**
     * Calls the {@code onEvent} method of the passed {@code ListenerManager},
     * causing the {@code run()} method of the registered listeners to be
     * called.
     *
     * @param listeners the {@code ListenerManager} whose listeners are to be
     *   notified. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public static void dispatchRunnable(ListenerManager<? extends Runnable> listeners) {
        listeners.onEvent(runnableDispatcher(), null);
    }

    /**
     * Calls the {@code onEvent} method of the passed
     * {@code OneShotListenerManager}, causing the {@code run()} method of the
     * registered listeners to be called.
     *
     * @param listeners the {@code OneShotListenerManager} whose listeners are
     *   to be notified. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public static void dispatchRunnable(OneShotListenerManager<? extends Runnable, Void> listeners) {
        listeners.onEvent(runnableDispatcher(), null);
    }

    private EventListeners() {
        throw new AssertionError();
    }
}
