package org.jtrim2.cancel;

import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see Cancellation#anyToken(CancellationToken[])
 */
final class CombinedTokenAny implements CancellationToken {
    private final CancellationToken[] tokens;

    public CombinedTokenAny(CancellationToken... tokens) {
        this.tokens = tokens.clone();
        ExceptionHelper.checkNotNullElements(this.tokens, "tokens");
    }

    @Override
    public ListenerRef addCancellationListener(Runnable listener) {
        Runnable wrappedListener = Tasks.runOnceTask(listener);

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
            if (token.isCanceled()) {
                return true;
            }
        }
        return false;
    }
}
