package org.jtrim2.event;

/**
 * @see EventListeners#runnableDispatcher()
 */
enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
    INSTANCE;

    @Override
    public void onEvent(Runnable eventListener, Void arg) {
        eventListener.run();
    }
}
