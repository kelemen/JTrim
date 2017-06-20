package org.jtrim2.event;

import java.util.Objects;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see ListenerRefs#combineListenerRefs(ListenerRef[])
 */
final class MultiListenerRef implements ListenerRef {
    private final ListenerRef[] refs;

    private MultiListenerRef(ListenerRef[] refs) {
        this.refs = refs;
        ExceptionHelper.checkNotNullElements(refs, "refs");
    }

    public static ListenerRef combine(ListenerRef... refs) {
        switch (refs.length) {
            case 0:
                return ListenerRefs.unregistered();
            case 1:
                return Objects.requireNonNull(refs[0], "refs[0]");
            default:
                return new MultiListenerRef(refs.clone());
        }
    }

    @Override
    public void unregister() {
        Throwable toThrow = null;

        for (ListenerRef ref: refs) {
            try {
                ref.unregister();
            } catch (Throwable ex) {
                if (toThrow != null) toThrow.addSuppressed(ex);
                else toThrow = ex;
            }
        }

        ExceptionHelper.rethrowIfNotNull(toThrow);
    }
}
