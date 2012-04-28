package org.jtrim.access;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@code AccessStateListener} which can keep track of the
 * availability of a group of rights. The {@code RightGroupHandler} must be
 * added to an {@code AccessManager} which notifies the
 * {@code RightGroupHandler} of changes in the availability of rights.
 * <P>
 * To use this class add the instance of {@code RightGroupHandler} to an
 * {@code AccessManager} (e.g.: for a {@link HierarchicalAccessManager}, it must
 * be specified in the constructor) and then specify the right groups and
 * the action to take when the group of right becomes available or unavailable
 * by calling the {@link #addGroupListener(Collection, Collection, AccessChangeAction) addGroupListener}
 * method.
 *
 * <h3>Thread safety</h3>
 * Instances of {@code RightGroupHandler} are safe to be accessed from multiple
 * threads concurrently but not however, that if {@code onEnterState} is called
 * concurrently with another {@code onEnterState} of the same
 * {@code RightGroupHandler} may cause the state of right availability to be
 * impossible.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not <I>synchronization transparent</I> but
 * they do not wait for any external event and return relatively quickly.
 *
 * @param <RightType> the type of the rights which can be handled by the
 *   {@code RightGroupHandler}
 *
 * @author Kelemen Attila
 */
public final class RightGroupHandler<RightType>
implements
        AccessStateListener<RightType> {

    private final ReentrantLock mainLock;

    // This maps rights to RightGroup instances which might be affected
    // when the AccessState of the right (the key of the map) changes.
    private final Map<RightType, Set<RightGroup>> groups;

    /**
     * Creates a new {@code RightGroupHandler} which does not yet monitors the
     * state of any right group.
     *
     * @param expectedGroupCount the maximum expected number of right groups to
     *   be added to this {@code RightGroupHandler} concurrently. The value of
     *   this argument does not affect correctness but may improve performance
     *   of adding right groups. This argument must be greater than or equal to
     *   zero.
     *
     * @throws IllegalArgumentException thrown if the specified
     *   {@code expectedGroupCount} argument is a negative integer
     */
    public RightGroupHandler(int expectedGroupCount) {
        this.mainLock = new ReentrantLock();
        this.groups = CollectionsEx.newHashMap(expectedGroupCount);
    }

    /**
     * Adds a group of rights which this {@code RightGroupHandler} must monitor
     * for availability. Whenever the availability of the group of rights
     * changes, the specified {@code AccessChangeAction} is invoked to take the
     * appropriate action.
     *
     * @param readRights the {@code Collection} of rights for which read access
     *   is required. Read access is granted to a particular right if there
     *   is no conflicting write access. This argument can be {@code null} which
     *   is equivalent to passing an empty {@code Collection}. The content of
     *   this passed {@code Collection} is copied by this method and modifying
     *   the {@code Collection} after this method has no effect on this
     *   {@code RightGroupHandler}.
     * @param writeRights the {@code Collection} of rights for which write
     *   access is required. Read access is granted to a particular right if
     *   there is no conflicting write or read access. This argument can be
     *   {@code null} which is equivalent to passing an empty
     *   {@code Collection}. The content of this passed {@code Collection} is
     *   copied by this method and modifying the {@code Collection} after this
     *   method has no effect on this {@code RightGroupHandler}.
     * @param accessChangeAction the listener to be notified when the
     *   availability of the specified rights changes. This argument cannot be
     *   {@code null}.
     * @return the {@code ListenerRef} which can be used to stop listening
     *   for the currently specified group of rights. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code accessChangeAction} is {@code null}
     */
    public ListenerRef<AccessChangeAction> addGroupListener(
            Collection<? extends RightType> readRights,
            Collection<? extends RightType> writeRights,
            final AccessChangeAction accessChangeAction) {
        final RightGroup newGroup = new RightGroup(
                readRights, writeRights, accessChangeAction);

        newGroup.addToGroups();

        return new ListenerRef<AccessChangeAction>() {
            private volatile boolean registered = true;
            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                newGroup.removeFromGroups();
                registered = false;
            }

            @Override
            public AccessChangeAction getListener() {
                return accessChangeAction;
            }
        };
    }

    /**
     * Checks whether any right group became available or unavailable and
     * notifies the registered {@code AccessChangeAction} instances if so.
     * <P>
     * If an invoked {@link AccessChangeAction#onChangeAccess(AccessManager, boolean)}
     * method throws an unchecked exception, it will be rethrown to the caller
     * after all the {@code AccessChangeAction} listeners were notified. Only
     * the first thrown exception will be rethrown, subsequent exceptions will
     * be {@link Throwable#addSuppressed(Throwable) suppressed}.
     *
     * @param accessManager the {@code AccessManager} in which the given right
     *   is managed. This argument cannot be {@code null}.
     * @param right the right which has changed state. This argument can only be
     *   {@code null} if the {@code AccessManager} supports {@code null} rights.
     * @param state the new state which is true for the specified right
     *   (or rights). This argument cannot be {@code null}.
     */
    @Override
    public void onEnterState(
            AccessManager<?, RightType> accessManager,
            RightType right, AccessState state) {

        Collection<RightGroup> affectedGroups;
        mainLock.lock();
        try {
            affectedGroups = groups.get(right);
            if (affectedGroups != null) {
                affectedGroups = new ArrayList<>(affectedGroups);
            }
        } finally {
            mainLock.unlock();
        }

        if (affectedGroups != null) {
            Throwable toThrow = null;
            for (RightGroup group: affectedGroups) {
                try {
                    group.checkChanges(accessManager);
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

    private class RightGroup {
        private final AtomicReference<Boolean> lastState;
        private final Set<RightType> readRights;
        private final Set<RightType> writeRights;
        private final AccessChangeAction action;

        public RightGroup(
                Collection<? extends RightType> readRights,
                Collection<? extends RightType> writeRights,
                AccessChangeAction accessChangeAction) {
            ExceptionHelper.checkNotNullArgument(accessChangeAction, "accessChangeAction");

            this.lastState = new AtomicReference<>(null);
            this.readRights = readRights != null
                    ? new HashSet<>(readRights)
                    : Collections.<RightType>emptySet();
            this.writeRights = writeRights != null
                    ? new HashSet<>(writeRights)
                    : Collections.<RightType>emptySet();
            this.action = accessChangeAction;
        }

        private void addToGroups(RightType right) {
            assert mainLock.isHeldByCurrentThread();

            Set<RightGroup> currentSet = groups.get(right);
            if (currentSet == null) {
                currentSet = new HashSet<>();
                groups.put(right, currentSet);
            }
            currentSet.add(this);
        }

        public void addToGroups() {
            mainLock.lock();
            try {
                for (RightType right: readRights) {
                    addToGroups(right);
                }
                for (RightType right: writeRights) {
                    addToGroups(right);
                }
            } finally {
                mainLock.unlock();
            }
        }

        private void removeFromGroups(RightType right) {
            assert mainLock.isHeldByCurrentThread();

            Set<RightGroup> currentSet = groups.get(right);
            if (currentSet != null) {
                currentSet.remove(this);
                if (currentSet.isEmpty()) {
                    groups.remove(right);
                }
            }
        }

        public void removeFromGroups() {
            mainLock.lock();
            try {
                for (RightType right: readRights) {
                    removeFromGroups(right);
                }
                for (RightType right: writeRights) {
                    removeFromGroups(right);
                }
            } finally {
                mainLock.unlock();
            }
        }

        public void checkChanges(AccessManager<?, RightType> accessManager) {
            boolean currentState = accessManager.isAvailable(readRights, writeRights);
            Boolean prevState = lastState.getAndSet(currentState);
            if (prevState == null || currentState != prevState.booleanValue()) {
                action.onChangeAccess(accessManager, currentState);
            }
        }
    }
}
