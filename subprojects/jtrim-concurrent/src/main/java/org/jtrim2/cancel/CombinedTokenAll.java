package org.jtrim2.cancel;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.utils.ExceptionHelper;

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
            return ListenerRefs.unregistered();
        }

        Runnable wrappedListener = Tasks.runOnceTask(listener);
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

        return () -> {
            for (ListenerRef listenerRef: listenerRefs) {
                listenerRef.unregister();
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
