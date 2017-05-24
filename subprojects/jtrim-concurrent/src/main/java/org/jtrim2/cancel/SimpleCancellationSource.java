package org.jtrim2.cancel;

import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.OneShotListenerManager;

/**
 * @see Cancellation#createCancellationSource()
 */
final class SimpleCancellationSource implements CancellationSource {
    private final CancellationControllerImpl impl;

    public SimpleCancellationSource() {
        this.impl = new CancellationControllerImpl();
    }

    @Override
    public CancellationController getController() {
        return impl;

    }

    @Override
    public CancellationToken getToken() {
        return impl.getToken();
    }

    private static class CancellationControllerImpl
    implements
            CancellationController {

        private final CancellationTokenImpl impl;

        public CancellationControllerImpl() {
            this.impl = new CancellationTokenImpl();
        }

        public CancellationToken getToken() {
            return impl;
        }

        @Override
        public void cancel() {
            impl.cancel();
        }
    }

    private static class CancellationTokenImpl
    implements
            CancellationToken {

        private volatile boolean canceled;
        private final OneShotListenerManager<Runnable, Void> listeners;

        public CancellationTokenImpl() {
            this.listeners = new OneShotListenerManager<>();
            this.canceled = false;
        }

        public void cancel() {
            canceled = true;
            EventListeners.dispatchRunnable(listeners);
        }

        @Override
        public ListenerRef addCancellationListener(final Runnable task) {
            return listeners.registerOrNotifyListener(task);
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }
    }
}
