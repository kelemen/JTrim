package org.jtrim.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * An {@code AccessAvailabilityNotifier} can keep track of the availability of
 * group of rights and notify listeners when a change occurs. The
 * {@code AccessAvailabilityNotifier} must be create by the static
 * {@link #attach(AccessManager) attach} method and the newly created instance
 * will then keep track of the changes of the rights of the passed
 * {@code AccessManager}.
 * <P>
 * Once you have created an instance of {@code AccessAvailabilityNotifier}, you
 * may then {@link #addGroupListener(Collection, Collection, AccessChangeAction) specify group of rights}
 * and action to executed when the availability of these rights changes. That
 * is, if they all become available, or if they cannot be acquired due to one or
 * more rights being unavailable.
 *
 * <h3>Thread safety</h3>
 * Instances of {@code AccessAvailabilityNotifier} are safe to be accessed from
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not <I>synchronization transparent</I> but
 * they do not wait for any external event. That is, they return relatively
 * quickly.
 *
 * @param <RightType>
 *
 * @see #attach(AccessManager)
 *
 * @author Kelemen Attila
 */
public final class AccessAvailabilityNotifier<RightType> {
    private final Impl<?, RightType> impl;

    private <IDType> AccessAvailabilityNotifier(AccessManager<IDType, RightType> manager) {
        ExceptionHelper.checkNotNullArgument(manager, "manager");
        this.impl = new Impl<>(manager);
    }

    /**
     * Creates a new instance of {@code AccessAvailabilityNotifier} which will
     * keep track of changes in the availability of the subsequently added right
     * groups.
     * <P>
     * This method will register a listener with the specified
     * {@code AccessManager} and you may cause the returned
     * {@code AccessAvailabilityNotifier} to unregister this added listener.
     * Note however, that doing so will stop the {@code AccessAvailabilityNotifier}
     * from keeping track of the changes of the availability right groups. That
     * is, all the added right groups will be unregistered (even if they are
     * added later).
     *
     * @param <IDType> the type of the request ID (see
     *   {@link AccessRequest#getRequestID()})
     * @param <RightType> the type of the rights that can be managed by the
     *   given {@code AccessManager}
     * @param manager the {@code AccessManager} managing the rights and
     *   determining if the rights are available or not. This argument cannot be
     *   {@code null}
     * @return a new instance of {@code AccessAvailabilityNotifier} which will
     *   keep track of changes in the availability of the subsequently added
     *   right groups. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AccessManager} is {@code null}
     *
     * @see #detach()
     */
    public static <IDType, RightType> AccessAvailabilityNotifier<RightType> attach(
            AccessManager<IDType, RightType> manager) {

        AccessAvailabilityNotifier<RightType> result = new AccessAvailabilityNotifier<>(manager);
        result.attach();
        return result;
    }

    private void attach() {
        impl.attach();
    }

    /**
     * Creates an {@link AccessChangeAction} which when notified, notifies all
     * the {@code AccessChangeAction} instances specified in the arguments.
     * <P>
     * Even if an {@code AccessChangeAction} throws an exception, other
     * {@code AccessChangeAction} instances will still be notified. However, the
     * first exception thrown by the underlying {@code AccessChangeAction}
     * instances will be rethrown to the caller of the
     * {@code AccessChangeAction} returned by this method. If more than one
     * {@code AccessChangeAction} throws an exception others will be
     * {@link Throwable#addSuppressed(Throwable) suppressed}.
     *
     * @param actions the {@code AccessChangeAction} instance to be called by
     *   the returned {@code AccessChangeAction}. This argument and its elements
     *   cannot be {@code null}.
     * @return the {@link AccessChangeAction} which when notified, notifies all
     *   the {@code AccessChangeAction} instances specified in the arguments.
     *   This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of its element
     *   is {@code null}
     */
    public static AccessChangeAction multiAction(AccessChangeAction... actions) {
        return new MultiAction(actions);
    }

    /**
     * Adds a group of rights which this {@code AccessAvailabilityNotifier} must
     * monitor for availability. Whenever the availability of the group of
     * rights changes, the specified {@code AccessChangeAction} is invoked to
     * take the appropriate action.
     *
     * @param readRights the {@code Collection} of rights for which read access
     *   is required. Read access is granted to a particular right if there is
     *   no conflicting write access. This argument can be {@code null} which is
     *   equivalent to passing an empty {@code Collection}. The content of this
     *   passed {@code Collection} is copied by this method and modifying the
     *   {@code Collection} after this method has no effect on this
     *   {@code AccessAvailabilityNotifier}.
     * @param writeRights the {@code Collection} of rights for which write
     *   access is required. Read access is granted to a particular right if
     *   there is no conflicting write or read access. This argument can be
     *   {@code null} which is equivalent to passing an empty
     *   {@code Collection}. The content of this passed {@code Collection} is
     *   copied by this method and modifying the {@code Collection} after this
     *   method has no effect on this {@code AccessAvailabilityNotifier}.
     * @param accessChangeAction the listener to be notified when the
     *   availability of the specified rights changes. This argument cannot be
     *   {@code null}.
     * @return the {@code ListenerRef} which can be used to stop listening for
     *   the currently specified group of rights. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     * {@code accessChangeAction} is {@code null}
     *
     * @see #multiAction(AccessChangeAction[])
     */
    public ListenerRef addGroupListener(
            Collection<? extends RightType> readRights,
            Collection<? extends RightType> writeRights,
            AccessChangeAction accessChangeAction) {

        return impl.addGroupListener(readRights, writeRights, accessChangeAction);
    }

    /**
     * Stops keeping track of the changes in the availability of rights. That
     * is, unregisters all the
     * {@link #addGroupListener(Collection, Collection, AccessChangeAction) added right groups}.
     * <P>
     * This method will make this {@code AccessAvailabilityNotifier} useless but
     * this method might be useful if you need to unregister the listener added
     * to the underlying {@code AccessManager}.
     */
    public void detach() {
        impl.detach();
    }

    private enum NotificationState {
        NEVER_NOTIFIED,
        AVAILABLE,
        UNAVAILABLE
    }

    private static final class RightGroup<RightType> {
        private final AccessChangeAction action;
        private final List<RightType> readRights;
        private final List<RightType> writeRights;
        private final AtomicReference<NotificationState> lastState;
        private volatile boolean enabled;

        public RightGroup(
                AccessChangeAction action,
                Collection<? extends RightType> readRights,
                Collection<? extends RightType> writeRights) {
            ExceptionHelper.checkNotNullArgument(action, "action");

            this.action = action;
            this.readRights = readRights != null
                    ? new ArrayList<>(readRights)
                    : Collections.<RightType>emptyList();
            this.writeRights = writeRights != null
                    ? new ArrayList<>(writeRights)
                    : Collections.<RightType>emptyList();
            this.lastState = new AtomicReference<>(NotificationState.NEVER_NOTIFIED);
            this.enabled = true;

            ExceptionHelper.checkNotNullElements(this.readRights, "readRights");
            ExceptionHelper.checkNotNullElements(this.writeRights, "writeRights");
        }

        private boolean setState(boolean available) {
            NotificationState nextState = available
                    ? NotificationState.AVAILABLE
                    : NotificationState.UNAVAILABLE;
            NotificationState prevState = lastState.getAndSet(nextState);
            return prevState != nextState;
        }

        public void updateAvailability(AccessManager<?, RightType> manager) {
            if (enabled) {
                boolean available = manager.isAvailable(readRights, writeRights);
                if (setState(available)) {
                    action.onChangeAccess(available);
                }
            }
        }

        public void disable() {
            enabled = false;
        }
    }

    private static final class Impl<IDType, RightType> {
        private final AccessManager<IDType, RightType> manager;
        private volatile ListenerRef listenerRef;
        private final Lock mainLock;
        private final RefList<RightGroup<RightType>> groups;
        private boolean attached;

        public Impl(AccessManager<IDType, RightType> manager) {
            ExceptionHelper.checkNotNullArgument(manager, "manager");
            this.manager = manager;
            this.mainLock = new ReentrantLock();
            this.groups = new RefLinkedList<>();
            this.attached = true;
        }

        private void attach() {
            listenerRef = manager.addAccessChangeListener(new AccessChangeListener<IDType, RightType>() {
                @Override
                public void onChangeAccess(AccessRequest<? extends IDType, ? extends RightType> request, boolean acquired) {
                    onAcquireOrRelease();
                }
            });
        }

        public ListenerRef addGroupListener(
                Collection<? extends RightType> readRights,
                Collection<? extends RightType> writeRights,
                final AccessChangeAction accessChangeAction) {
            final RightGroup<RightType> group = new RightGroup<>(accessChangeAction, readRights, writeRights);

            final RefList.ElementRef<?> elementRef;
            mainLock.lock();
            try {
                if (!attached) {
                    return UnregisteredListenerRef.INSTANCE;
                }
                elementRef = groups.addLastGetReference(group);
            } finally {
                mainLock.unlock();
            }

            return new ListenerRef() {
                private volatile boolean registered = true;

                @Override
                public boolean isRegistered() {
                    return registered;
                }

                @Override
                public void unregister() {
                    group.disable();
                    mainLock.lock();
                    try {
                        elementRef.remove();
                    } finally {
                        mainLock.unlock();
                    }
                    registered = false;
                }
            };
        }

        public void detach() {
            // Each method which might be called from this method are
            // idempotent, so it is no problem calling them multiple times.

            ListenerRef currentRef = listenerRef;
            listenerRef = null;
            if (currentRef != null) {
                currentRef.unregister();
            }

            mainLock.lock();
            try {
                for (RightGroup<?> group: groups) {
                    group.disable();
                }
                groups.clear();
                attached = false;
            } finally {
                mainLock.unlock();
            }
        }

        private void onAcquireOrRelease() {
            List<RightGroup<RightType>> currentGroups;
            mainLock.lock();
            try {
                currentGroups = new ArrayList<>(groups);
            } finally {
                mainLock.unlock();
            }

            for (RightGroup<RightType> group : currentGroups) {
                group.updateAvailability(manager);
            }
        }
    }

    private static class MultiAction implements AccessChangeAction {
        private final AccessChangeAction[] subActions;

        public MultiAction(AccessChangeAction[] subActions) {
            this.subActions = subActions.clone();
            ExceptionHelper.checkNotNullElements(this.subActions, "subActions");
        }

        @Override
        public void onChangeAccess(boolean available) {
            Throwable toThrow = null;
            for (AccessChangeAction action: subActions) {
                try {
                    action.onChangeAccess(available);
                } catch (Throwable ex) {
                    if (toThrow == null) {
                        toThrow = ex;
                    }
                    else {
                        toThrow.addSuppressed(ex);
                    }
                }
            }

            if (toThrow != null) {
                ExceptionHelper.rethrow(toThrow);
            }
        }
    }
}
