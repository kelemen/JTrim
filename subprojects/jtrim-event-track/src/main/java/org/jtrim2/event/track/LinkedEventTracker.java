package org.jtrim2.event.track;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.DelegatedTaskExecutorService;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;

/**
 * An {@link EventTracker} implementations which stores the causes in a singly
 * linked list. Therefore, checking for the existence of a particular cause of
 * an event requires every cause of the event to be checked.
 * <P>
 * This implementations can recognize the causality between events only by
 * the mandatory ways defined by the {@code EventTracker} interface.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>. Note that only
 * the methods provided by this class are <I>synchronization transparent</I>,
 * the {@code TrackedListenerManager} is not.
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
        Objects.requireNonNull(eventKind, "eventKind");
        Objects.requireNonNull(argType, "argType");

        ManagerKey key = new ManagerKey(eventKind, argType);
        return new TrackedListenerManagerImpl<>(key);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskExecutor createTrackedExecutor(final TaskExecutor executor) {
        return new TrackedExecutor(executor);
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
        } else {
            currentCauses.remove();
        }
    }

    private static final class ManagerKey {
        private final Object eventKind;
        private final Class<?> argClass;

        public ManagerKey(Object eventKind, Class<?> argClass) {
            Objects.requireNonNull(eventKind, "eventKind");
            Objects.requireNonNull(argClass, "argClass");

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
            final ManagerKey other = (ManagerKey) obj;
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
        private final ListenerManager<TrackedEventListener<ArgType>> manager;

        public ManagerHolder() {
            this.manager = new CopyOnTriggerListenerManager<>();
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
            manager.onEvent(TrackedEventListener::onEvent, arg);
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
            return (ManagerHolder<ArgType>) managers.get(key);
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
                    managers.putIfAbsent(key, new ManagerHolder<>());
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

            return () -> {
                try {
                    chosenRef.unregister();
                } finally {
                    cleanupManagers(chosenManagerHolder);
                }
            };
        }

        private void cleanupManagers(ManagerHolder<ArgType> managerHolder) {
            registerLock.lock();
            try {
                if (managerHolder.isEmpty()) {
                    managers.remove(key, managerHolder);
                }
            } finally {
                registerLock.unlock();
            }
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
            if (obj == this) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TrackedListenerManagerImpl<?> other = (TrackedListenerManagerImpl<?>) obj;
            return this.getOuter() == other.getOuter()
                    && Objects.equals(this.key, other.key);
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
                result = () -> new LinkedCausesIterator<>(LinkedCauses.this);
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

    // Sets and restores the cause before running the wrapped task.
    private class RunnableWrapper implements Runnable {
        private final LinkedCauses cause;
        private final Runnable task;

        public RunnableWrapper(LinkedCauses cause, Runnable task) {
            this.cause = cause;
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void run() {
            LinkedCauses prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                task.run();
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
            this.cause = cause;
            this.task = Objects.requireNonNull(task, "task");
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
            this.cause = cause;
            this.task = Objects.requireNonNull(task, "task");
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
        public void execute(Runnable command) {
            LinkedCauses causes = getCausesIfAny();
            wrappedExecutor.execute(new RunnableWrapper(causes, command));
        }

        @Override
        public CompletionStage<Void> execute(CancellationToken cancelToken, CancelableTask task) {
            LinkedCauses causes = getCausesIfAny();
            return wrappedExecutor.execute(cancelToken, new TaskWrapper(causes, task));
        }

        @Override
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {

            LinkedCauses causes = getCausesIfAny();
            return wrappedExecutor.executeFunction(cancelToken, new FunctionWrapper<>(causes, function));
        }
    }

    private class TrackedExecutor implements TaskExecutor {
        private final TaskExecutor executor;

        public TrackedExecutor(TaskExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
        }

        @Override
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {

            LinkedCauses cause = getCausesIfAny();
            return executor.executeFunction(cancelToken, new FunctionWrapper<>(cause, function));
        }
    }
}
