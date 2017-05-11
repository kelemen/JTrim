package org.jtrim2.event;

import java.util.Objects;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see ListenerRegistries#combineListenerRefs(ListenerRef[])
 */
final class MultiListenerRef implements ListenerRef {
    private final ListenerRef[] refs;

    private MultiListenerRef(ListenerRef[] refs) {
        this.refs = refs;
        ExceptionHelper.checkNotNullElements(refs, "refs");
    }

    public static ListenerRef combine(ListenerRef... refs) {
        ListenerRef result;

        switch (refs.length) {
            case 0:
                result = UnregisteredListenerRef.INSTANCE;
                break;
            case 1:
                result = refs[0];
                Objects.requireNonNull(result, "refs[0]");
                break;
            default:
                result = new MultiListenerRef(refs.clone());
                break;
        }
        return result;
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
