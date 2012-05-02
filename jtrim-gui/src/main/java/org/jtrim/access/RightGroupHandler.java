package org.jtrim.access;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@code AccessStateListener} which can keep track of the
 * availability of a group of rights. The {@code RightGroupHandler} must be
 * added to an {@code AccessManager} which notifies the
 * {@code RightGroupHandler} of changes in the availability of rights. Note
 * however, that this implementation can only handle {@link HierarchicalRight}
 * as rights.
 * <P>
 * To use this class add the instance of {@code RightGroupHandler} to an
 * {@code AccessManager} (e.g.: for a {@link HierarchicalAccessManager}, it must
 * be specified in the constructor) and then specify the right groups and
 * the action to take when the group of right becomes available or unavailable
 * by calling the {@link #addGroupListener(Collection, Collection, boolean, AccessChangeAction) addGroupListener}
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
 * @author Kelemen Attila
 */
public final class RightGroupHandler
implements
        AccessStateListener<HierarchicalRight> {

    private final ReentrantLock mainLock;

    // A tree where in the nodes are stored the RightGroup instances which might
    // have been afffected given the HierarchicalRight defined by the path to
    // the node (the children nodes might be affected as well).
    private final RightNode groups;

    /**
     * Creates an {@link AccessChangeAction} which when notified, notifies all
     * the {@code AccessChangeAction} instances specified in the arguments.
     * <P>
     * Even if an {@code AccessChangeAction} throws an exception, other
     * {@code AccessChangeAction} instances will still be notified. However, the
     * first exception thrown by the underlying {@code AccessChangeAction}
     * instances will be rethrown to the called of the {@code AccessChangeAction}
     * returned by this method. If more than one {@code AccessChangeAction}
     * throws an exception, others will
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
     * Creates a new {@code RightGroupHandler} which does not yet monitors the
     * state of any right group.
     *
     * @throws IllegalArgumentException thrown if the specified
     *   {@code expectedGroupCount} argument is a negative integer
     */
    public RightGroupHandler() {
        this.mainLock = new ReentrantLock();
        this.groups = new RightNode(null, null);
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
     * @param lazyNotify if this argument is {@code false}, the
     *   specified {@code AccessChangeAction} will be notified always when
     *   a state of any given right change regardless if this change resulted
     *   in a change in the availability of the right group. Otherwise, the
     *   {@code AccessChangeAction} will only be notified if the availability of
     *   the right group changes.
     * @param accessChangeAction the listener to be notified when the
     *   availability of the specified rights changes. This argument cannot be
     *   {@code null}.
     * @return the {@code ListenerRef} which can be used to stop listening
     *   for the currently specified group of rights. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code accessChangeAction} is {@code null}
     *
     * @see #multiAction(AccessChangeAction[])
     */
    public ListenerRef addGroupListener(
            Collection<HierarchicalRight> readRights,
            Collection<HierarchicalRight> writeRights,
            boolean lazyNotify,
            final AccessChangeAction accessChangeAction) {
        final RightGroup newGroup = new RightGroup(
                readRights, writeRights, lazyNotify, accessChangeAction);

        newGroup.addToGroups();

        return new ListenerRef() {
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
            AccessManager<?, HierarchicalRight> accessManager,
            HierarchicalRight right, AccessState state) {

        Set<RightGroup> affectedGroups;
        mainLock.lock();
        try {
            RightNode node = groups.tryGetNode(right);
            affectedGroups = groups.getAffectedGroups(right);
            if (node != null) {
                node.getAllGroups(affectedGroups);
            }
        } finally {
            mainLock.unlock();
        }

        if (affectedGroups != null && !affectedGroups.isEmpty()) {
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
        private final Set<HierarchicalRight> readRights;
        private final Set<HierarchicalRight> writeRights;
        private final boolean lazyNotify;
        private final AccessChangeAction action;

        public RightGroup(
                Collection<HierarchicalRight> readRights,
                Collection<HierarchicalRight> writeRights,
                boolean lazyNotify,
                AccessChangeAction accessChangeAction) {
            ExceptionHelper.checkNotNullArgument(accessChangeAction, "accessChangeAction");

            this.lastState = new AtomicReference<>(null);
            this.readRights = readRights != null
                    ? new HashSet<>(readRights)
                    : Collections.<HierarchicalRight>emptySet();
            this.writeRights = writeRights != null
                    ? new HashSet<>(writeRights)
                    : Collections.<HierarchicalRight>emptySet();
            this.lazyNotify = lazyNotify;
            this.action = accessChangeAction;
        }

        public void addToGroups() {
            mainLock.lock();
            try {
                for (HierarchicalRight right: readRights) {
                    groups.addGroup(right, this);
                }
                for (HierarchicalRight right: writeRights) {
                    groups.addGroup(right, this);
                }
            } finally {
                mainLock.unlock();
            }
        }

        private void removeFromGroups(HierarchicalRight right) {
            assert mainLock.isHeldByCurrentThread();

            RightNode node = groups.tryGetNode(right);
            if (node != null) {
                node.removeGroup(this);
            }
        }

        public void removeFromGroups() {
            mainLock.lock();
            try {
                for (HierarchicalRight right: readRights) {
                    removeFromGroups(right);
                }
                for (HierarchicalRight right: writeRights) {
                    removeFromGroups(right);
                }
            } finally {
                mainLock.unlock();
            }
        }

        public void checkChanges(AccessManager<?, HierarchicalRight> accessManager) {
            boolean currentState = accessManager.isAvailable(readRights, writeRights);
            Boolean prevState = lastState.getAndSet(currentState);
            if (!lazyNotify
                    || prevState == null
                    || currentState != prevState.booleanValue()) {
                action.onChangeAccess(accessManager, currentState);
            }
        }
    }

    private static class RightNode {
        private final Set<RightGroup> affectedGroups;
        private final RightNode parentNode;
        private final Object inputEdge;
        private final Map<Object, RightNode> edges;

        public RightNode(Object inputEdge, RightNode parentNode) {
            this.inputEdge = inputEdge;
            this.parentNode = parentNode;
            this.affectedGroups = new HashSet<>();
            this.edges = new HashMap<>();
        }

        private RightNode getNode(HierarchicalRight path) {
            RightNode currentNode = this;
            for (Object edge: path.getRights()) {
                RightNode prevNode = currentNode;
                currentNode = currentNode.edges.get(edge);
                if (currentNode == null) {
                    currentNode = new RightNode(edge, prevNode);
                    prevNode.edges.put(edge, currentNode);
                }
            }
            return currentNode;
        }

        public Set<RightGroup> getAffectedGroups(HierarchicalRight path) {
            Set<RightGroup> result = new HashSet<>();
            result.addAll(affectedGroups);

            RightNode currentNode = this;
            for (Object edge: path.getRights()) {
                currentNode = currentNode.edges.get(edge);
                if (currentNode == null) {
                    break;
                }
                else {
                    result.addAll(currentNode.affectedGroups);
                }
            }
            return result;
        }

        public RightNode tryGetNode(HierarchicalRight path) {
            RightNode currentNode = this;
            for (Object edge: path.getRights()) {
                currentNode = currentNode.edges.get(edge);
                if (currentNode == null) {
                    return null;
                }
            }
            return currentNode;
        }

        public void getAllGroups(Set<RightGroup> result) {
            result.addAll(affectedGroups);
            for (RightNode child: edges.values()) {
                child.getAllGroups(result);
            }
        }

        private boolean isEmptyNode() {
            return affectedGroups.isEmpty() && edges.isEmpty();
        }

        public void addGroup(HierarchicalRight path, RightGroup group) {
            getNode(path).affectedGroups.add(group);
        }

        public void removeGroup(RightGroup group) {
            affectedGroups.remove(group);

            // cleanup the edge map for performance
            RightNode prevNode = this;
            RightNode currentNode = parentNode;
            while (currentNode != null && prevNode.isEmptyNode()) {
                currentNode.edges.remove(prevNode.inputEdge);

                prevNode = currentNode;
                currentNode = currentNode.parentNode;
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
        public void onChangeAccess(
                AccessManager<?, HierarchicalRight> accessManager,
                boolean available) {
            Throwable toThrow = null;
            for (AccessChangeAction action: subActions) {
                try {
                    action.onChangeAccess(accessManager, available);
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
