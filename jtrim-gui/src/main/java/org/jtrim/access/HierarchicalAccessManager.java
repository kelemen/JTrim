package org.jtrim.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskScheduler;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * An implementation of {@code AccessManager} which can manage
 * {@link HierarchicalRight hierarchical rights}.
 *
 * <h3>Rights</h3>
 * This implementation uses hierarchical rights so requesting a right will
 * need that subrights are also available for the requested use. The value
 * {@code null} is not considered to be a valid hierarchical right.
 * See {@link HierarchicalRight} for further details on hierarchical rights.
 *
 * <h3>Events</h3>
 * This implementation can notify clients if an access token is acquired or
 * released. These notification events are submitted to a user specified
 * {@link TaskExecutor TaskExecutor}, so clients can define
 * where the events execute but cannot define on what thread these events
 * are submitted to this {@code TaskExecutor}. They maybe scheduled on the
 * thread used by the {@code TaskExecutor} of the
 * {@link AccessToken AccessTokens} or in the call stack of caller of a methods
 * of this class. Although is not possible to determine which event is submitted
 * on which thread, these events will be submitted in the order they occurred.
 *
 * <h3>Thread safety</h3>
 * This class is thread-safe without any further synchronization. Note however
 * that although thread-safe, this implementation  will not scale well with
 * multiple processors.
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>
 * because they may notify the specified
 * {@link AccessChangeListener AccessChangeListener}. In case
 * scheduling the events of the changes in right states is
 * <I>synchronization transparent</I> then the methods of this class are also
 * <I>synchronization transparent</I>. Note that in most case you cannot
 * assume this and can only rely on that they do not block indefinitely.
 *
 * @param <IDType> the type of the request ID (see
 *   {@link AccessRequest#getRequestID()})
 *
 * @see HierarchicalRight
 * @author Kelemen Attila
 */
public final class HierarchicalAccessManager<IDType>
implements
        AccessManager<IDType, HierarchicalRight> {

    private final ReentrantLock mainLock;
    private final AccessTree<AccessTokenImpl<IDType>> readTree;
    private final AccessTree<AccessTokenImpl<IDType>> writeTree;

    private final TaskScheduler eventScheduler;
    private final ListenerManager<AccessChangeListener<IDType, HierarchicalRight>> listeners;

    /**
     * Creates a new {@code HierarchicalAccessManager} executing events in the
     * context of the specified executor.
     *
     * @param eventExecutor the {@code Executor} to which events are submitted
     *   to. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public HierarchicalAccessManager(TaskExecutor eventExecutor) {
        ExceptionHelper.checkNotNullArgument(eventExecutor, "eventExecutor");

        this.mainLock = new ReentrantLock();
        this.eventScheduler = new TaskScheduler(eventExecutor);
        this.readTree = new AccessTree<>();
        this.writeTree = new AccessTree<>();
        this.listeners = new CopyOnTriggerListenerManager<>();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef addAccessChangeListener(AccessChangeListener<IDType, HierarchicalRight> listener) {
        return listeners.registerListener(listener);
    }

    /**
     * Returns the shared tokens of the specified tokens without returning
     * the same token twice (using "==" for comparison).
     *
     * @param <V> the type of the request ID
     * @param tokens the tokens of which the shared tokens are to be returned.
     *   This argument cannot be {@code null}.
     */
    private static <V> Set<AccessToken<V>> getUniqueSharedTokens(
            Collection<AccessTokenImpl<V>> tokens) {

        Set<AccessToken<V>> result = CollectionsEx.newIdentityHashSet(tokens.size());
        for (AccessTokenImpl<V> token: tokens) {
            AccessToken<V> sharedToken = token.getSharedToken();
            if (sharedToken != null) {
                result.add(sharedToken);
            }
        }
        return result;
    }

    private static <V> Set<V> createSet(Collection<? extends V> collection) {
        Set<V> result = CollectionsEx.newIdentityHashSet(collection.size());
        result.addAll(collection);
        return result;
    }

    /**
     * Submits state change events if the client requested to do so.
     */
    private void dispatchEvents() {
        assert !mainLock.isHeldByCurrentThread();
        eventScheduler.dispatchTasks();
    }

    private void scheduleEvent(
            final AccessRequest<? extends IDType, ? extends HierarchicalRight> request,
            final boolean acquire) {
        eventScheduler.scheduleTask(() -> {
            listeners.onEvent((AccessChangeListener<IDType, HierarchicalRight> eventListener, Void arg) -> {
                eventListener.onChangeAccess(request, acquire);
            }, null);
        });
    }

    /**
     * Removes the rights associated with the specified token and eventually
     * will notify the listener. This method is called right after the specified
     * token has terminated.
     *
     * @param token the token which terminated
     */
    private void removeToken(AccessTokenImpl<IDType> token) {
        AccessRequest<? extends IDType, ? extends HierarchicalRight> request;
        Collection<? extends HierarchicalRight> readRights;
        Collection<? extends HierarchicalRight> writeRights;

        request = token.getRequest();
        readRights = request.getReadRights();
        writeRights = request.getWriteRights();

        Collection<HierarchicalRight> removedReadRights = new LinkedList<>();
        Collection<HierarchicalRight> removedWriteRights = new LinkedList<>();

        mainLock.lock();
        try {
            for (RefList.ElementRef<AccessTokenImpl<IDType>> index: token.getTokenIndexes()) {
                index.remove();
            }

            readTree.cleanupRights(readRights);
            writeTree.cleanupRights(writeRights);

            scheduleEvent(request, false);
        } finally {
            mainLock.unlock();
        }

        assert !mainLock.isHeldByCurrentThread();
        dispatchEvents();
    }

    /**
     * Add the rights associated with the specified token to {@code readTree}
     * and {@code writeTree}. This method will eventually notify the listener
     * if a right has changed state.
     *
     * @param token the token which rights must be added to the internal right
     *   trees. This argument is used only so that rights can be associated with
     *   with the token.
     * @param request the right request which was requested by the client
     *   these rights are associated with the specified token
     * @return the references of the rights in the internal right trees of
     *   the specified tokens. These references must be removed after
     *   the token terminates.
     */
    private Collection<RefList.ElementRef<AccessTokenImpl<IDType>>> addRigths(
            AccessTokenImpl<IDType> token,
            AccessRequest<? extends IDType, ? extends HierarchicalRight> request) {
        assert mainLock.isHeldByCurrentThread();

        Collection<RefList.ElementRef<AccessTokenImpl<IDType>>> result;
        result = new LinkedList<>();

        readTree.addRights(token, request.getReadRights(), result);
        writeTree.addRights(token, request.getWriteRights(), result);

        return result;
    }

    /**
     * This method returns the tokens conflicting with the specified request.
     *
     * @param request the request which is checked for conflicts
     * @param result the conflicting tokens will be added to this collection
     */
    private void getBlockingTokensList(
            AccessRequest<? extends IDType, ? extends HierarchicalRight> request,
            Collection<? super AccessTokenImpl<IDType>> result) {

        assert mainLock.isHeldByCurrentThread();
        Collection<? extends HierarchicalRight> readRights = request.getReadRights();
        Collection<? extends HierarchicalRight> writeRights = request.getWriteRights();
        getBlockingTokensList(readRights, writeRights, result);
    }

    /**
     * This method returns the tokens conflicting with the specified request.
     *
     * @param requestedReadRights the requested read rights checked for
     *   conflicts
     * @param requestedWriteRights the requested write rights checked for
     *   conflicts
     * @param result the conflicting tokens will be added to this collection
     */
    private void getBlockingTokensList(
            Collection<? extends HierarchicalRight> requestedReadRights,
            Collection<? extends HierarchicalRight> requestedWriteRights,
            Collection<? super AccessTokenImpl<IDType>> result) {

        assert mainLock.isHeldByCurrentThread();

        readTree.getBlockingTokens(requestedWriteRights, result);
        writeTree.getBlockingTokens(requestedReadRights, result);
        writeTree.getBlockingTokens(requestedWriteRights, result);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Collection<AccessToken<IDType>> getBlockingTokens(
            Collection<? extends HierarchicalRight> requestedReadRights,
            Collection<? extends HierarchicalRight> requestedWriteRights) {

        List<AccessTokenImpl<IDType>> blockingTokens = new LinkedList<>();

        mainLock.lock();
        try {
            getBlockingTokensList(requestedReadRights, requestedWriteRights,
                    blockingTokens);
        } finally {
            mainLock.unlock();
        }

        return getUniqueSharedTokens(blockingTokens);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isAvailable(
            Collection<? extends HierarchicalRight> requestedReadRights,
            Collection<? extends HierarchicalRight> requestedWriteRights) {
        mainLock.lock();
        try {
            if (readTree.hasConflict(requestedWriteRights)) {
                return false;
            }

            if (writeTree.hasConflict(requestedReadRights)) {
                return false;
            }

            if (writeTree.hasConflict(requestedWriteRights)) {
                return false;
            }
        } finally {
            mainLock.unlock();
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AccessResult<IDType> tryGetAccess(
            AccessRequest<? extends IDType, ? extends HierarchicalRight> request) {

        AccessTokenImpl<IDType> token;
        token = new AccessTokenImpl<>(request);

        List<AccessTokenImpl<IDType>> blockingTokens = new LinkedList<>();
        mainLock.lock();
        try {
            getBlockingTokensList(request, blockingTokens);
            if (blockingTokens.isEmpty()) {
                Collection<RefList.ElementRef<AccessTokenImpl<IDType>>> tokenIndexes;
                tokenIndexes = addRigths(token, request);
                token.setTokenIndexes(tokenIndexes);
                scheduleEvent(request, true);
            }
        } finally {
            mainLock.unlock();
        }
        dispatchEvents();

        token.addReleaseListener(() -> removeToken(token));

        if (blockingTokens.isEmpty()) {
            token.setSharedToken(token);
            return new AccessResult<>(token);
        }
        else {
            return new AccessResult<>(getUniqueSharedTokens(blockingTokens));
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AccessResult<IDType> getScheduledAccess(
            AccessRequest<? extends IDType, ? extends HierarchicalRight> request) {
        AccessTokenImpl<IDType> token;
        token = new AccessTokenImpl<>(request);

        List<AccessToken<IDType>> blockingTokens = new LinkedList<>();
        mainLock.lock();
        try {
            getBlockingTokensList(request, blockingTokens);

            Collection<RefList.ElementRef<AccessTokenImpl<IDType>>> tokenIndexes;
            tokenIndexes = addRigths(token, request);
            token.setTokenIndexes(tokenIndexes);

            scheduleEvent(request, true);
        } finally {
            mainLock.unlock();
        }
        dispatchEvents();

        token.addReleaseListener(() -> removeToken(token));

        Set<AccessToken<IDType>> blockingTokenSet = createSet(blockingTokens);

        AccessToken<IDType> scheduledToken;
        scheduledToken = ScheduledAccessToken.newToken(token, blockingTokenSet);
        token.setSharedToken(scheduledToken);
        return new AccessResult<>(scheduledToken, blockingTokenSet);
    }

    /**
     * This tree represents a collection of rights. The graph should be
     * considered to be a directed graph were edges point from the parent
     * to children. A path in this graph represents a hierarchical right where
     * the edges are the parts of this hierarchical right in order.
     * Vertices of the graph contains tokens and these tokens mean that
     * the right up to the point of this vertex is associated with these
     * tokens. Note that since these rights are hierarchical this implies that
     * the token also represents all the subrights (even if subrights
     * do not contain the token explicitly).
     */
    private static class AccessTree<TokenType extends AccessToken<?>> {
        private final RefList<TokenType> tokens;
        private final Map<Object, AccessTree<TokenType>> subTrees; // the edges

        public AccessTree() {
            this.subTrees = new HashMap<>();
            this.tokens = new RefLinkedList<>();
        }

        private AccessTree<TokenType> getSubTree(Object key) {
            AccessTree<TokenType> result = subTrees.get(key);
            if (result == null) {
                result = new AccessTree<>();
                subTrees.put(key, result);
            }

            return result;
        }

        private boolean isEmpty() {
            if (!tokens.isEmpty()) {
                return false;
            }

            for (AccessTree<TokenType> subTree: subTrees.values()) {
                if (!subTree.isEmpty()) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Removes all of this tree's subtrees which do not contain tokens.
         * This means that after this call every leaf element of this graph
         * will contain at least a single token unless there no tokens in this
         * tree at all. In which case every subtree will be removed:
         * {@code subTrees.size()} will return 0.
         * <P>
         * A right is not considered to part of the tree if there are no tokens
         * in its vertex. (Notice that actually a vertex defines a right:
         * the edges up to the vertex)
         *
         * @param modifications the modifications will be added to this
         *   collection
         * @return {@code true} if this method did any modifications,
         *   {@code false} if this method left the tree untouched
         */
        private boolean cleanupTree() {
            boolean modified = false;

            Iterator<Map.Entry<Object, AccessTree<TokenType>>> itr;
            itr = subTrees.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<Object, AccessTree<TokenType>> sub;
                sub = itr.next();

                if (sub.getValue().isEmpty()) {
                    itr.remove();
                    modified = true;
                }
            }
            return modified;
        }

        /**
         * Finds a subtree of this tree specified by a hierarchical right
         * and will clean up that tree: {@link #cleanupTree cleanupTree}. Also
         * cleans up every parent node of the right along the path if the whole
         * subtree is removed.
         *
         * @param right the specified subtree to be cleaned
         */
        public void cleanupRight(HierarchicalRight right) {
            AccessTree<TokenType> currentTree = this;

            List<Object> rights = right.getRights();

            List<AccessTree<?>> trees = new ArrayList<>(rights.size());
            for (Object subRight: rights) {
                trees.add(currentTree);
                currentTree = currentTree.subTrees.get(subRight);

                if (currentTree == null) {
                    return;
                }
            }

            for (int i = trees.size() - 1; i >= 0; i--) {
                AccessTree<?> tree = trees.get(i);

                if (!tree.cleanupTree()) {
                    return;
                }
            }
        }

        public void cleanupRights(Collection<? extends HierarchicalRight> rights) {
            for (HierarchicalRight right: rights) {
                cleanupRight(right);
            }
        }

        public void getBlockingTokens(
                Collection<? extends HierarchicalRight> rights,
                Collection<? super TokenType> result) {
            for (HierarchicalRight right: rights) {
                getBlockingTokens(right, result);
            }
        }

        public boolean hasConflict(
                Collection<? extends HierarchicalRight> rights) {
            for (HierarchicalRight right: rights) {
                if (hasConflict(right)) {
                    return true;
                }
            }
            return false;
        }

        private void getAllTokens(Collection<? super TokenType> result) {
            result.addAll(tokens);
            for (AccessTree<TokenType> currentTree: subTrees.values()) {
                currentTree.getAllTokens(result);
            }
        }

        /**
         * Returns the tokens that are conflicting with the specified rights
         * in this tree.
         *
         * @param right the right checked for conflicts
         * @param result conflicting tokens will be added to this collection
         */
        public void getBlockingTokens(
                HierarchicalRight right,
                Collection<? super TokenType> result) {

            AccessTree<TokenType> currentTree = this;

            for (Object subRight: right.getRights()) {
                result.addAll(currentTree.tokens);

                currentTree = currentTree.subTrees.get(subRight);
                if (currentTree == null) {
                    return;
                }
            }

            currentTree.getAllTokens(result);
        }

        private boolean hasTokens() {
            if (!tokens.isEmpty()) {
                return true;
            }
            for (AccessTree<?> currentTree: subTrees.values()) {
                if (currentTree.hasTokens()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns {@code true} if there are conflicting tokens with the
         * specified rights in this tree.
         *
         * @param right the right checked for conflicts
         * @return {@code true} if there is at least one conflicting token with
         *   the given right, {@code false} otherwise
         */
        public boolean hasConflict(HierarchicalRight right) {
            AccessTree<TokenType> currentTree = this;

            for (Object subRight: right.getRights()) {
                if (!currentTree.tokens.isEmpty()) {
                    return true;
                }

                currentTree = currentTree.subTrees.get(subRight);
                if (currentTree == null) {
                    return false;
                }
            }

            return currentTree.hasTokens();
        }

        public void addRights(
                TokenType token,
                Collection<? extends HierarchicalRight> rights,
                Collection<RefList.ElementRef<TokenType>> resultRefs) {

            for (HierarchicalRight right: rights) {
                addRight(token, right, resultRefs);
            }
        }

        /**
         * Adds a specific right to this tree.
         *
         * @param token the token which is associated with the specified rights
         * @param right the right to be added to this tree
         * @param resultRefs the references which can be removed
         *   to remove the references of the specified token added to this tree
         */
        public void addRight(
                TokenType token,
                HierarchicalRight right,
                Collection<RefList.ElementRef<TokenType>> resultRefs) {

            AccessTree<TokenType> currentTree = this;
            for (Object subRight: right.getRights()) {
                currentTree = currentTree.getSubTree(subRight);
            }

            resultRefs.add(currentTree.tokens.addLastGetReference(token));
        }

        /**
         * Returns all the rights contained in this tree.
         * The rights will be prefixed with the specified parent rights.
         *
         * @param parent the prefix of the returned rights.
         * @param result the (prefixed) rights will be added to this collection.
         * @return the right represented by {@code parent}. This is simply
         *   a performance hack, so we do not always need to create new arrays.
         */
        public HierarchicalRight getRights(Object[] parents,
                Collection<HierarchicalRight> result) {

            HierarchicalRight thisRight;

            if (!subTrees.isEmpty()) {
                Object[] rightList = new Object[parents.length + 1];
                System.arraycopy(parents, 0, rightList, 0, parents.length);

                HierarchicalRight aChildRight = null;
                int lastIndex = parents.length;
                for (Map.Entry<Object, AccessTree<TokenType>> treeEdge: subTrees.entrySet()) {
                    rightList[lastIndex] = treeEdge.getKey();
                    aChildRight = treeEdge.getValue().getRights(rightList, result);
                }

                // Note that subTrees is not empty, so aChildRight != null
                thisRight = aChildRight.getParentRight();
            }
            else {
                thisRight = HierarchicalRight.create(parents);
            }

            if (!tokens.isEmpty()) {
                result.add(thisRight);
            }

            return thisRight;
        }

        /**
         * Returns all the rights contained in this tree.
         *
         * @param result the rights will be added to this collection.
         */
        public void getRights(Collection<HierarchicalRight> result) {
            getRights(new Object[0], result);
        }
    }

    /**
     * The access token which stores references of the tokens in
     * {@code readTree} and {@code writeTree}, so these references can
     * be removed after this token terminates. This implementation
     * simply relies on the {@link GenericAccessToken}.
     */
    private static class AccessTokenImpl<IDType> extends DelegatedAccessToken<IDType> {
        private final AccessRequest<? extends IDType, ? extends HierarchicalRight> request;
        private List<RefList.ElementRef<AccessTokenImpl<IDType>>> tokenIndexes;
        private volatile AccessToken<IDType> sharedToken;

        /**
         * Initializes this token with a {@code null} shared token and an
         * empty list of token references in the right trees.
         * <P>
         * {@link #setSharedToken(org.jtrim.access.AccessToken) setSharedToken}
         * and
         * {@link #init(org.jtrim.access.AccessListener, java.util.Collection) init}
         * must be called before returning this token to the client.
         *
         * @param taskExecutor the executor to which tasks will be submitted to.
         * @param request the right request which requested this token to
         *   be created
         */
        public AccessTokenImpl(
                AccessRequest<? extends IDType, ? extends HierarchicalRight> request) {
            super(AccessTokens.createToken(request.getRequestID()));

            this.request = request;
            this.tokenIndexes = Collections.emptyList();
            this.sharedToken = null;
        }

        /**
         * Returns the reference of the token which was returned to the user.
         *
         * @return the shared token
         */
        public AccessToken<IDType> getSharedToken() {
            return sharedToken;
        }

        /**
         * Sets the token which was returned to the user. This is important
         * because when conflicting tokens need to be returned we must provide
         * the same tokens that were returned to the user. This is the case
         * with scheduled tokens where we do not return this token but
         * wrap it with a {@link ScheduledAccessToken}. If we were to return
         * this instance as conflicting token, the client could abuse that
         * reference by submitting task to it circumventing the protection
         * of {@code ScheduledAccessToken}.
         * <P>
         * This method must be called exactly once before returning the
         * requested token to the client.
         *
         * @param sharedToken the token actually returned to the client
         */
        public void setSharedToken(AccessToken<IDType> sharedToken) {
            assert this.sharedToken == null && sharedToken != null;
            this.sharedToken = sharedToken;
        }

        /**
         * Sets the references to this token in the right trees.
         * <P>
         * This method must be called exactly once before returning the
         * requested token to the client.
         *
         * @param tokenIndexes the references to this token in the right trees.
         */
        public void setTokenIndexes(Collection<RefList.ElementRef<AccessTokenImpl<IDType>>> tokenIndexes) {
            if (tokenIndexes != null) {
                this.tokenIndexes = new ArrayList<>(tokenIndexes);
            }
        }

        public AccessRequest<? extends IDType, ? extends HierarchicalRight> getRequest() {
            return request;
        }

        public List<RefList.ElementRef<AccessTokenImpl<IDType>>> getTokenIndexes() {
            return tokenIndexes;
        }
    }

    /**
     * Returns the rights that are currently in use: there are not terminated
     * {@link AccessToken AccessTokens} associated with these rights.
     * <P>
     * This method was designed for monitoring only and cannot be used
     * for synchronization control.
     * <P>
     * Note this method will return a disjunct sets for the read and write
     * rights.
     *
     * <h5>Thread safety</h5>
     * Note that although this method is thread-safe, the rights may change
     * right after this method returns, so they may not be up-to-date.
     * <h6>Synchronization transparency</h6>
     * This method is <I>synchronization transparent</I>.
     *
     * @param readRights the rights that are currently used for "reading" will
     *   be added to this collection. These rights can be requested for reading.
     *   No rights will be added more than once. This argument cannot be
     *   {@code null}.
     * @param writeRights the rights that are currently used for "writing"
     *   (i.e.: they are used exclusively) will be added to this collection.
     *   No rights will be added more than once.
     *
     * @throws NullPointerException thrown if on of the arguments is
     *   {@code null}
     */
    public void getRights(
            Collection<HierarchicalRight> readRights,
            Collection<HierarchicalRight> writeRights) {

        ExceptionHelper.checkNotNullArgument(readRights, "readRights");
        ExceptionHelper.checkNotNullArgument(writeRights, "writeRights");

        mainLock.lock();
        try {
            readTree.getRights(readRights);
            writeTree.getRights(writeRights);
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the string representation of this access manager in no
     * particular format. The string representation will contain the rights
     * currently in use.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        List<HierarchicalRight> readRights = new LinkedList<>();
        List<HierarchicalRight> writeRights = new LinkedList<>();
        getRights(readRights, writeRights);

        return "HierarchicalAccessManager{"
                + "read rights=" + readRights
                + ", write rights=" + writeRights + '}';
    }
}
