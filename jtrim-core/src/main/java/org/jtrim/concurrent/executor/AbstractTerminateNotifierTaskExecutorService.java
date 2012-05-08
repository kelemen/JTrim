package org.jtrim.concurrent.executor;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public abstract class AbstractTerminateNotifierTaskExecutorService
extends
        AbstractTaskExecutorService {

    private final ListenerManager<Runnable, Void> listeners;

    public AbstractTerminateNotifierTaskExecutorService() {
        this.listeners = new CopyOnTriggerListenerManager<>();
    }

    protected final void notifyTerminateListeners() {
        listeners.onEvent(RunnableDispatcher.INSTANCE, null);
    }

    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        // A quick check for the already terminate case.
        if (isTerminated()) {
            listener.run();
            return UnregisteredListenerRef.INSTANCE;
        }

        AutoUnregisterListener autoListener = new AutoUnregisterListener(listener);
        ListenerRef result = autoListener.registerWith(listeners);
        if (isTerminated()) {
            autoListener.run();
        }
        return result;
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }

    // Note that apart from automatic unregistering, this class
    // takes care that the listener may not be run multiple times.
    private static class AutoUnregisterListener implements Runnable {
        private final AtomicReference<Runnable> listener;
        private volatile ListenerRef listenerRef;

        public AutoUnregisterListener(Runnable listener) {
            this.listener = new AtomicReference<>(listener);
            this.listenerRef = null;
        }

        public ListenerRef registerWith(ListenerManager<Runnable, ?> manager) {
            ListenerRef currentRef = manager.registerListener(this);
            this.listenerRef = currentRef;
            if (listener.get() == null) {
                this.listenerRef = null;
                currentRef.unregister();
            }
            return currentRef;
        }

        @Override
        public void run() {
            Runnable currentListener = listener.getAndSet(null);
            ListenerRef currentRef = listenerRef;
            if (currentRef != null) {
                currentRef.unregister();
            }
            if (currentListener != null) {
                currentListener.run();
            }
        }
    }
}
