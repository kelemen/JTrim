package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see Cancellation#createCancellationSource()
 *
 * @author Kelemen Attila
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
        private final ListenerManager<Runnable, Void> listeners;

        public CancellationTokenImpl() {
            this.listeners = new CopyOnTriggerListenerManager<>();
            this.canceled = false;
        }

        public void cancel() {
            canceled = true;
            listeners.onEvent(RunnableDispatcher.INSTANCE, null);
        }

        @Override
        public ListenerRef addCancellationListener(final Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");

            SingleShotEvent idempotentTask = new SingleShotEvent(task);
            final ListenerRef result = listeners.registerListener(idempotentTask);
            idempotentTask.setListenerRef(result);

            if (isCanceled()) {
                result.unregister();
                idempotentTask.run();
                return UnregisteredListenerRef.INSTANCE;
            }
            else {
                return result;
            }
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void checkCanceled() {
            if (isCanceled()) {
                throw new OperationCanceledException();
            }
        }

        private static class SingleShotEvent implements Runnable {
            private final AtomicReference<Runnable> taskRef;
            private volatile ListenerRef listenerRef;

            public SingleShotEvent(Runnable task) {
                this.taskRef = new AtomicReference<>(task);
                this.listenerRef = null;
            }

            public void setListenerRef(ListenerRef listenerRef) {
                this.listenerRef = listenerRef;
                if (taskRef.get() == null) {
                    listenerRef.unregister();
                    this.listenerRef = null;
                }
            }

            @Override
            public void run() {
                Runnable task = taskRef.getAndSet(null);
                if (task != null) {
                    try {
                        task.run();
                    } finally {
                        ListenerRef currentListenerRef = listenerRef;
                        if (currentListenerRef != null) {
                            currentListenerRef.unregister();
                            listenerRef = null;
                        }
                    }
                }
            }
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }
}
