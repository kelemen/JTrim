package org.jtrim.event;

import org.jtrim.utils.ExceptionHelper;

/**
 * @see ListenerRegistries#combineListenerRefs(ListenerRef[])
 *
 * @author Kelemen Attila
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
                ExceptionHelper.checkNotNullArgument(result, "refs[0]");
                break;
            default:
                result = new MultiListenerRef(refs);
                break;
        }
        return result;
    }

    @Override
    public boolean isRegistered() {
        for (ListenerRef ref: refs) {
            if (ref.isRegistered()) {
                return true;
            }
        }
        return false;
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

        if (toThrow != null) {
            ExceptionHelper.rethrow(toThrow);
        }
    }
}
