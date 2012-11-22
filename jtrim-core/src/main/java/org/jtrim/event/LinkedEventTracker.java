package org.jtrim.event;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.DelegatedTaskExecutorService;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.TaskFuture;
import org.jtrim.utils.ExceptionHelper;

/**
 * An {@link EventTracker} implementations which stores the causes in a singly
 * linked list. Therefore, checking for the existence of a particular cause of
 * an event requires every cause of the event to be checked.
 * <P>
 * This implementations can recognize the causality between events only by
 * the mandatory ways defined by the {@code EventTracker} interface.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>. Note that only
 * the methods provided by this class are <I>synchronization transparent</I>,
 * the {@code TrackedListenerManager} is not.
 *
 * @author Kelemen Attila
 */
public final class LinkedEventTracker
implements
        EventTracker {

    private final ThreadLocal<LinkedCauses> currentCauses;

    // This map stores the container of registered listeners to be notified
    // when event triggers. The map maps the arguments of getContainerOfType
    // to the listener. If a key does not contain a listener registered
    // it is equivalent to not having a listener registered.
    //
    // The "?" type argument is the same as the class stored in the
    // associated ManagerKey.
    //
    // ManagerHolder holds the ListenerManager to simplify its name, this is
    // the only reason ManagerHolder exists.
    private final ConcurrentMap<ManagerKey, ManagerHolder<?>> managers;

    // Held while adding a listener to a ListenerManager in "managers", so
    // when removing listener can be confident that it does not remove a
    // ListenerManager from the map which contains registered listeners.
    private final Lock registerLock;

    /**
     * Creates a new {@code LinkedEventTracker} which does not have a listener
     * registered to any event and does not know about any event to be a cause.
     */
    public LinkedEventTracker() {
        this.registerLock = new ReentrantLock();
        this.currentCauses = new ThreadLocal<>();
        this.managers = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <ArgType> TrackedListenerManager<ArgType> getManagerOfType(
            Object eventKind, Class<ArgType> argType) {
        ExceptionHelper.checkNotNullArgument(eventKind, "eventKind");
        ExceptionHelper.checkNotNullArgument(argType, "argType");

        ManagerKey key = new ManagerKey(eventKind, argType);
        return new TrackedListenerManagerImpl<>(key);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskExecutor createTrackedExecutor(final TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        return new TaskExecutor() {
            @Override
            public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
                LinkedCauses cause = getCausesIfAny();
                executor.execute(cancelToken, new TaskWrapper(cause, task), wrapCleanupTask(cause, cleanupTask));
            }
        };
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskExecutorService createTrackedExecutorService(
            TaskExecutorService executor) {
        return new TaskWrapperExecutor(executor);
    }

    private LinkedCauses getCausesIfAny() {
        LinkedCauses result = currentCauses.get();
        if (result == null) {
            // Remove the unnecessary thread local variable from the underlying
            // map.
            currentCauses.remove();
        }
        return result;
    }

    private void setAsCurrentCause(LinkedCauses newCause) {
        if (newCause != null) {
            currentCauses.set(newCause);
        }
        else {
            currentCauses.remove();
        }
    }

    private CleanupTask wrapCleanupTask(LinkedCauses cause, CleanupTask task) {
        return task != null ? new CleanupTaskWrapper(cause, task) : null;
    }

    private static final class ManagerKey {
        private final Object eventKind;
        private final Class<?> argClass;

        public ManagerKey(Object eventKind, Class<?> argClass) {
            ExceptionHelper.checkNotNullArgument(eventKind, "eventKind");
            ExceptionHelper.checkNotNullArgument(argClass, "argClass");

            this.eventKind = eventKind;
            this.argClass = argClass;
        }

        public Object getEventKind() {
            return eventKind;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ManagerKey other = (ManagerKey)obj;
            return this.argClass == other.argClass
                    && Objects.equals(this.eventKind, other.eventKind);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(eventKind);
            hash = 79 * hash + System.identityHashCode(argClass);
            return hash;
        }
    }

    private static final class ManagerHolder<ArgType> {
        private final ListenerManager<TrackedEventListener<ArgType>, TrackedEvent<ArgType>> manager;
        private final EventDispatcher<TrackedEventListener<ArgType>, TrackedEvent<ArgType>> eventDispatcher;

        public ManagerHolder() {
            this.manager = new CopyOnTriggerListenerManager<>();
            this.eventDispatcher
                    = new EventDispatcher<TrackedEventListener<ArgType>, TrackedEvent<ArgType>>() {
                @Override
                public void onEvent(
                        TrackedEventListener<ArgType> eventListener,
                        TrackedEvent<ArgType> arg) {
                    eventListener.onEvent(arg);
                }
            };
        }

        public int getListenerCount() {
            return manager.getListenerCount();
        }

        public ListenerRef registerListener(
                TrackedEventListener<ArgType> listener) {
            return manager.registerListener(listener);
        }

        public boolean isEmpty() {
            return manager.getListenerCount() == 0;
        }

        public void dispatchEvent(TrackedEvent<ArgType> arg) {
            manager.onEvent(eventDispatcher, arg);
        }
    }

    private final class TrackedListenerManagerImpl<ArgType>
    implements
            TrackedListenerManager<ArgType> {

        private final ManagerKey key;

        public TrackedListenerManagerImpl(ManagerKey key) {
            assert key != null;
            // If generics were reified, the following condition assert should
            // not fail:
            // assert key.argType == ArgType.class
            this.key = key;
        }

        // This cast is safe because the ArgType is the same as the class
        // defined by key.argClass.
        @SuppressWarnings("unchecked")
        private ManagerHolder<ArgType> getAndCast() {
            return (ManagerHolder<ArgType>)managers.get(key);
        }

        @Override
        public void onEvent(ArgType arg) {
            ManagerHolder<ArgType> managerHolder = getAndCast();
            if (managerHolder == null) {
                return;
            }

            LinkedCauses causes = currentCauses.get();
            try {
                TriggeredEvent<ArgType> triggeredEvent;
                triggeredEvent = new TriggeredEvent<>(key.getEventKind(), arg);

                TrackedEvent<ArgType> trackedEvent = causes != null
                        ? new TrackedEvent<>(causes, arg)
                        : new TrackedEvent<>(arg);

                currentCauses.set(new LinkedCauses(causes, triggeredEvent));
                managerHolder.dispatchEvent(trackedEvent);
            } finally {
                setAsCurrentCause(causes);
            }
        }

        @Override
        public ListenerRef registerListener(
                final TrackedEventListener<ArgType> listener) {
            ListenerRef resultRef;

            // We have to try multiple times if the ManagerHolder is removed
            // concurrently from the map because it
            ManagerHolder<ArgType> prevManagerHolder;
            ManagerHolder<ArgType> managerHolder = getAndCast();
            do {
                while (managerHolder == null) {
                    managers.putIfAbsent(key, new ManagerHolder<ArgType>());
                    managerHolder = getAndCast();
                }

                registerLock.lock();
                try {
                    resultRef = managerHolder.registerListener(listener);
                } finally {
                    registerLock.unlock();
                }

                prevManagerHolder = managerHolder;
                managerHolder = getAndCast();
            } while (managerHolder != prevManagerHolder);

            final ManagerHolder<ArgType> chosenManagerHolder = managerHolder;
            final ListenerRef chosenRef = resultRef;

            return new ListenerRef() {
                @Override
                public boolean isRegistered() {
                    return chosenRef.isRegistered();
                }

                private void cleanupManagers() {
                    registerLock.lock();
                    try {
                        if (chosenManagerHolder.isEmpty()) {
                            managers.remove(key, chosenManagerHolder);
                        }
                    } finally {
                        registerLock.unlock();
                    }
                }

                @Override
                public void unregister() {
                    try {
                        chosenRef.unregister();
                    } finally {
                        cleanupManagers();
                    }
                }
            };
        }

        @Override
        public int getListenerCount() {
            ManagerHolder<ArgType> managerHolder = getAndCast();
            return managerHolder != null
                    ? managerHolder.getListenerCount()
                    : 0;
        }

        private LinkedEventTracker getOuter() {
            return LinkedEventTracker.this;
        }

        // Providing the equals and hashCode is not necessary according to the
        // documentation but providing it does not hurt and may protect a
        // careless coder.

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TrackedListenerManagerImpl<?> other
                    = (TrackedListenerManagerImpl<?>)obj;
            if (this.getOuter() != other.getOuter()) {
                return false;
            }
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.key);
            hash = 29 * hash + System.identityHashCode(getOuter());
            return hash;
        }
    }

    private static final class LinkedCauses extends AbstractEventCauses {
        private final int numberOfCauses;
        private final LinkedCauses prevCauses;
        private final TriggeredEvent<?> currentCause;
        private volatile Iterable<TriggeredEvent<?>> causeIterable;

        public LinkedCauses(
                LinkedCauses prevCauses, TriggeredEvent<?> currentCause) {
            this.prevCauses = prevCauses;
            this.currentCause = currentCause;
            this.causeIterable = null;
            this.numberOfCauses = prevCauses != null
                    ? prevCauses.getNumberOfCauses() + 1
                    : 1;
        }

        @Override
        public int getNumberOfCauses() {
            return numberOfCauses;
        }

        @Override
        public Iterable<TriggeredEvent<?>> getCauses() {
            Iterable<TriggeredEvent<?>> result = causeIterable;
            if (result == null) {
                result = new Iterable<TriggeredEvent<?>>() {
                    @Override
                    public Iterator<TriggeredEvent<?>> iterator() {
                        return new LinkedCausesIterator<>(LinkedCauses.this);
                    }
                };
                causeIterable = result;
            }
            return result;
        }

        public TriggeredEvent<?> getCurrentCause() {
            return currentCause;
        }
    }

    private static final class LinkedCausesIterator<EventKindType>
    implements
            Iterator<TriggeredEvent<?>> {

        private LinkedCauses currentCauses;

        public LinkedCausesIterator(LinkedCauses currentCauses) {
            this.currentCauses = currentCauses;
        }

        @Override
        public boolean hasNext() {
            return currentCauses != null;
        }

        @Override
        public TriggeredEvent<?> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            TriggeredEvent<?> result = currentCauses.getCurrentCause();
            currentCauses = currentCauses.prevCauses;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove from event causes.");
        }
    }

    private class CleanupTaskWrapper implements CleanupTask {
        private final LinkedCauses cause;
        private final CleanupTask task;

        public CleanupTaskWrapper(LinkedCauses cause, CleanupTask task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public void cleanup(boolean canceled, Throwable error) throws Exception {
            LinkedCauses prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                task.cleanup(canceled, error);
            } finally {
                setAsCurrentCause(prevCause);
            }
        }

    }

    // Sets and restores the cause before running the wrapped task.
    private class TaskWrapper implements CancelableTask {
        private final LinkedCauses cause;
        private final CancelableTask task;

        public TaskWrapper(LinkedCauses cause, CancelableTask task) {
            ExceptionHelper.checkNotNullArgument(task, "task");

            this.cause = cause;
            this.task = task;
        }

        @Override
        public void execute(CancellationToken cancelToken) throws Exception {
            LinkedCauses prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                task.execute(cancelToken);
            } finally {
                setAsCurrentCause(prevCause);
            }
        }
    }

    // Sets and restores the cause before running the wrapped task.
    private class FunctionWrapper<V> implements CancelableFunction<V> {
        private final LinkedCauses cause;
        private final CancelableFunction<V> task;

        public FunctionWrapper(LinkedCauses cause, CancelableFunction<V> task) {
            ExceptionHelper.checkNotNullArgument(task, "task");

            this.cause = cause;
            this.task = task;
        }

        @Override
        public V execute(CancellationToken cancelToken) throws Exception {
            LinkedCauses prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                return task.execute(cancelToken);
            } finally {
                setAsCurrentCause(prevCause);
            }
        }
    }

    // Wraps tasks before submitting it to the wrapped executor
    // to set and restore the cause before running the submitted task.
    private final class TaskWrapperExecutor extends DelegatedTaskExecutorService {
        public TaskWrapperExecutor(TaskExecutorService wrapped) {
            super(wrapped);
        }

        @Override
        public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
            LinkedCauses causes = getCausesIfAny();
            wrappedExecutor.execute(cancelToken,
                    new TaskWrapper(causes, task),
                    wrapCleanupTask(causes, cleanupTask));
        }

        @Override
        public TaskFuture<?> submit(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
            LinkedCauses causes = getCausesIfAny();
            return wrappedExecutor.submit(cancelToken,
                    new TaskWrapper(causes, task),
                    wrapCleanupTask(causes, cleanupTask));
        }

        @Override
        public <V> TaskFuture<V> submit(CancellationToken cancelToken, CancelableFunction<V> task, CleanupTask cleanupTask) {
            LinkedCauses causes = getCausesIfAny();
            return wrappedExecutor.submit(cancelToken,
                    new FunctionWrapper<>(causes, task),
                    wrapCleanupTask(causes, cleanupTask));
        }
    }
}
