package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class CombinedTokenAll implements CancellationToken {
    private final CancellationToken[] tokens;

    public CombinedTokenAll(CancellationToken... tokens) {
        this.tokens = tokens.clone();
        ExceptionHelper.checkNotNullElements(this.tokens, "tokens");
    }

    @Override
    public ListenerRef addCancellationListener(Runnable listener) {
        if (tokens.length == 0) {
            listener.run();
            return UnregisteredListenerRef.INSTANCE;
        }

        Runnable wrappedListener = Tasks.runOnceTask(listener, false);
        wrappedListener = new ListenerForwarder(wrappedListener, tokens.length);

        final ListenerRef[] listenerRefs = new ListenerRef[tokens.length];
        try {
            for (int i = 0; i < tokens.length; i++) {
                listenerRefs[i] = tokens[i].addCancellationListener(wrappedListener);
            }
        } catch (Throwable ex) {
            for (ListenerRef listenerRef: listenerRefs) {
                try {
                    if (listenerRef != null) {
                        listenerRef.unregister();
                    }
                } catch (Throwable subEx) {
                    ex.addSuppressed(subEx);
                }
            }
            throw ex;
        }

        return new ListenerRef() {
            @Override
            public boolean isRegistered() {
                for (ListenerRef listenerRef: listenerRefs) {
                    if (listenerRef.isRegistered()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void unregister() {
                for (ListenerRef listenerRef: listenerRefs) {
                    listenerRef.unregister();
                }
            }
        };
    }

    @Override
    public boolean isCanceled() {
        for (CancellationToken token: tokens) {
            if (!token.isCanceled()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void checkCanceled() {
        if (isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    private static class ListenerForwarder implements Runnable {
        private final Runnable task;
        private final AtomicInteger runRequired;

        public ListenerForwarder(Runnable task, int runRequired) {
            this.task = task;
            this.runRequired = new AtomicInteger(runRequired);
        }

        @Override
        public void run() {
            if (runRequired.decrementAndGet() == 0) {
                task.run();
            }
        }
    }
}
