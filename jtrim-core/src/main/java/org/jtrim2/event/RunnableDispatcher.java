package org.jtrim2.event;

/**
 * @see EventListeners#runnableDispatcher()
 *
 * @author Kelemen Attila
 */
enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
    INSTANCE;

    @Override
    public void onEvent(Runnable eventListener, Void arg) {
        eventListener.run();
    }
}
