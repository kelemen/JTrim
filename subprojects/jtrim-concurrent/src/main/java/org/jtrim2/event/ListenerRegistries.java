package org.jtrim2.event;

import java.util.Collection;

/**
 * Defines static utility methods for {@link SimpleListenerRegistry listener registries}.
 */
public final class ListenerRegistries {
    /**
     * Returns a combination of multiple {@code ListenerRef} instances. That is,
     * the returned {@code ListenerRef} is registered, if, and only, if at
     * least one of the specified {@code ListenerRef} instances is registered.
     * Also, {@link ListenerRef#unregister() unregistering} the returned
     * {@code ListenerRef} will cause all the wrapped {@code ListenerRef}
     * instances to be unregistered.
     * <P>
     * {@link ListenerRef#unregister() Unregistering} the returned
     * {@code ListenerRef} will cause the {@code unregister} method of all
     * the specified {@code ListenerRef} instances to be called. In case some of
     * them throws an exception, the first exception to be thrown will be
     * propagated to the caller and others will be suppressed
     * (via {@link Throwable#addSuppressed(Throwable)}.
     * <P>
     * Note: Passing an empty array will return a {@code ListenerRef} which is
     * always considered to be unregistered.
     *
     * @param refs the {@code ListenerRef} instances to be combined. This
     *   argument cannot be {@code null} and none of the {@code ListenerRef}
     *   instances can be {@code null}.
     * @return a combination of multiple {@code ListenerRef} instances. This
     *   method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the {@code ListenerRef}
     *   instances (or the array itself) is {@code null}
     */
    public static ListenerRef combineListenerRefs(ListenerRef... refs) {
        return MultiListenerRef.combine(refs);
    }

    /**
     * Returns a combination of multiple {@code ListenerRef} instances. That is,
     * the returned {@code ListenerRef} is registered, if, and only, if at
     * least one of the specified {@code ListenerRef} instances is registered.
     * Also, {@link ListenerRef#unregister() unregistering} the returned
     * {@code ListenerRef} will cause all the wrapped {@code ListenerRef}
     * instances to be unregistered.
     * <P>
     * {@link ListenerRef#unregister() Unregistering} the returned
     * {@code ListenerRef} will cause the {@code unregister} method of all
     * the specified {@code ListenerRef} instances to be called. In case some of
     * them throws an exception, the first exception to be thrown will be
     * propagated to the caller and others will be suppressed
     * (via {@link Throwable#addSuppressed(Throwable)}.
     * <P>
     * Note: Passing an empty collection will return a {@code ListenerRef} which
     * is always considered to be unregistered.
     *
     * @param refs the {@code ListenerRef} instances to be combined. This
     *   argument cannot be {@code null} and none of the {@code ListenerRef}
     *   instances can be {@code null}. Note that the content of this collection
     *   is not allowed to be changed during this method call.
     * @return a combination of multiple {@code ListenerRef} instances. This
     *   method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the {@code ListenerRef}
     *   instances (or the collection itself) is {@code null}
     */
    public static ListenerRef combineListenerRefs(Collection<? extends ListenerRef> refs) {
        return MultiListenerRef.combine(refs.toArray(new ListenerRef[refs.size()]));
    }

    private ListenerRegistries() {
        throw new AssertionError();
    }
}
