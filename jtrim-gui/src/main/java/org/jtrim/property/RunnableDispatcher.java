package org.jtrim.property;

import org.jtrim.event.EventDispatcher;

/**
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
