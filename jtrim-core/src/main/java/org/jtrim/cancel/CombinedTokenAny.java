package org.jtrim.cancel;

import org.jtrim.concurrent.Tasks;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class CombinedTokenAny implements CancellationToken {
    private final CancellationToken[] tokens;

    public CombinedTokenAny(CancellationToken... tokens) {
        this.tokens = tokens.clone();
        ExceptionHelper.checkNotNullElements(this.tokens, "tokens");
    }

    @Override
    public ListenerRef addCancellationListener(Runnable listener) {
        Runnable wrappedListener = Tasks.runOnceTask(listener, false);

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
            if (token.isCanceled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkCanceled() {
        for (CancellationToken token: tokens) {
            token.checkCanceled();
        }
    }
}
