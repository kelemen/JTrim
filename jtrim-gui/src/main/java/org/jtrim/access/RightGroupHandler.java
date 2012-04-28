package org.jtrim.access;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class RightGroupHandler<RightType>
implements
        AccessStateListener<RightType> {

    private final ReentrantLock mainLock;
    private final Map<RightType, Set<RightGroup>> groups;

    public RightGroupHandler(int expectedGroupCount) {
        this.mainLock = new ReentrantLock();
        this.groups = CollectionsEx.newHashMap(expectedGroupCount);
    }

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
            for (RightGroup group: affectedGroups) {
                group.checkChanges(accessManager);
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
