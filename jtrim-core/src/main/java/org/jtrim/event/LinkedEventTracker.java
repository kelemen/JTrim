package org.jtrim.event;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class LinkedEventTracker<EventKindType>
implements
        EventTracker<EventKindType> {

    private final ThreadLocal<LinkedCauses<EventKindType>> currentCauses;

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
    private final ConcurrentMap<ManagerKey<?>, ManagerHolder<EventKindType, ?>> managers;

    // Held while adding a listener to a ListenerManager in "managers", so
    // when removing listener can be confident that it does not remove a
    // ListenerManager from the map which contains registered listeners.
    private final Lock registerLock;

    public LinkedEventTracker() {
        this.registerLock = new ReentrantLock();
        this.currentCauses = new ThreadLocal<>();
        this.managers = new ConcurrentHashMap<>();
    }

    @Override
    public <ArgType> TrackedListenerManager<EventKindType, ArgType> getManagerOfType(
            EventKindType eventKind, Class<ArgType> argType) {
        ExceptionHelper.checkNotNullArgument(eventKind, "eventKind");
        ExceptionHelper.checkNotNullArgument(argType, "argType");

        ManagerKey<EventKindType> key = new ManagerKey<>(eventKind, argType);
        return new TrackedListenerManagerImpl<>(key);
    }

    @Override
    public Executor createTrackedExecutor(final Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        return new Executor() {
            @Override
            public void execute(Runnable command) {
                LinkedCauses<EventKindType> cause = getCausesIfAny();
                executor.execute(new TaskWrapperRunnable(cause, command));
            }
        };
    }

    @Override
    public ExecutorService createTrackedExecutorService(ExecutorService executor) {
        return new TaskWrapperExecutor(executor);
    }

    private LinkedCauses<EventKindType> getCausesIfAny() {
        LinkedCauses<EventKindType> result = currentCauses.get();
        if (result == null) {
            // Remove the unnecessary thread local variable from the underlying
            // map.
            currentCauses.remove();
        }
        return result;
    }

    private static final class ManagerKey<EventKindType> {
        private final EventKindType eventKind;
        private final Class<?> argClass;

        public ManagerKey(EventKindType eventKind, Class<?> argClass) {
            ExceptionHelper.checkNotNullArgument(eventKind, "eventKind");
            ExceptionHelper.checkNotNullArgument(argClass, "argClass");

            this.eventKind = eventKind;
            this.argClass = argClass;
        }

        public EventKindType getEventKind() {
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
            final ManagerKey<?> other = (ManagerKey<?>)obj;
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

    private static final class ManagerHolder<EventKindType, ArgType> {
        private final ListenerManager<
                TrackedEventListener<EventKindType, ArgType>,
                TrackedEvent<EventKindType, ArgType>> manager;

        private final EventDispatcher<
                TrackedEventListener<EventKindType, ArgType>,
                TrackedEvent<EventKindType, ArgType>> eventDispatcher;

        public ManagerHolder() {
            this.manager = new CopyOnTriggerListenerManager<>();
            this.eventDispatcher = new EventDispatcher<
                    TrackedEventListener<EventKindType, ArgType>,
                    TrackedEvent<EventKindType, ArgType>>() {
                @Override
                public void onEvent(
                        TrackedEventListener<EventKindType, ArgType> eventListener,
                        TrackedEvent<EventKindType, ArgType> arg) {
                    eventListener.onEvent(arg);
                }
            };
        }

        public ListenerManager<
                TrackedEventListener<EventKindType, ArgType>,
                TrackedEvent<EventKindType, ArgType>> getManager() {
            return manager;
        }

        public boolean isEmpty() {
            return manager.getListenerCount() == 0;
        }

        public void dispatchEvent(TrackedEvent<EventKindType, ArgType> arg) {
            manager.onEvent(eventDispatcher, arg);
        }
    }

    private final class TrackedListenerManagerImpl<ArgType>
    implements
            TrackedListenerManager<EventKindType, ArgType> {

        private final ManagerKey<EventKindType> key;

        public TrackedListenerManagerImpl(ManagerKey<EventKindType> key) {
            assert key != null;
            // If generics were reified, the following condition assert should
            // not fail:
            // assert key.argType == ArgType.class
            this.key = key;
        }

        // This cast is safe because the ArgType is the same as the class
        // defined by key.argClass.
        @SuppressWarnings("unchecked")
        private ManagerHolder<EventKindType, ArgType> getAndCast() {
            return (ManagerHolder<EventKindType, ArgType>)managers.get(key);
        }

        @Override
        public void onEvent(ArgType arg) {
            ManagerHolder<EventKindType, ArgType> managerHolder = getAndCast();
            if (managerHolder == null) {
                return;
            }

            LinkedCauses<EventKindType> causes = currentCauses.get();

            TriggeredEvent<EventKindType, ArgType> triggeredEvent;
            triggeredEvent = new TriggeredEvent<>(key.getEventKind(), arg);

            TrackedEvent<EventKindType, ArgType> trackedEvent = causes != null
                    ? new TrackedEvent<>(causes, arg)
                    : new TrackedEvent<EventKindType, ArgType>(arg);

            try {
                currentCauses.set(new LinkedCauses<>(causes, triggeredEvent));
                managerHolder.dispatchEvent(trackedEvent);
            } finally {
                if (causes != null) {
                    currentCauses.set(causes);
                }
                else {
                    currentCauses.remove();
                }
            }
        }

        @Override
        public ListenerRef<TrackedEventListener<EventKindType, ArgType>> registerListener(
                final TrackedEventListener<EventKindType, ArgType> listener) {

            ListenerRef<TrackedEventListener<EventKindType, ArgType>> resultRef;

            // We have to try multiple times if the ManagerHolder is removed
            // concurrently from the map because it
            ManagerHolder<EventKindType, ArgType> prevManagerHolder;
            ManagerHolder<EventKindType, ArgType> managerHolder = getAndCast();
            do {
                while (managerHolder == null) {
                    managers.putIfAbsent(key, new ManagerHolder<EventKindType, ArgType>());
                    managerHolder = getAndCast();
                }

                registerLock.lock();
                try {
                    resultRef = managerHolder.getManager().registerListener(listener);
                } finally {
                    registerLock.unlock();
                }

                prevManagerHolder = managerHolder;
                managerHolder = getAndCast();
            } while (managerHolder != prevManagerHolder);

            final ManagerHolder<EventKindType, ArgType> chosenManagerHolder = managerHolder;
            final ListenerRef<TrackedEventListener<EventKindType, ArgType>> chosenRef = resultRef;

            return new ListenerRef<TrackedEventListener<EventKindType, ArgType>>() {
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

                @Override
                public TrackedEventListener<EventKindType, ArgType> getListener() {
                    return listener;
                }
            };
        }

        @Override
        public int getListenerCount() {
            ManagerHolder<EventKindType, ArgType> managerHolder = getAndCast();
            return managerHolder != null
                    ? managerHolder.getManager().getListenerCount()
                    : 0;
        }
    }

    private static final class LinkedCauses<EventKindType>
    extends
            AbstractEventCauses<EventKindType> {

        private final int numberOfCauses;
        private final LinkedCauses<EventKindType> prevCauses;
        private final TriggeredEvent<EventKindType, ?> currentCause;
        private volatile Iterable<TriggeredEvent<EventKindType, ?>> causeIterable;

        public LinkedCauses(
                LinkedCauses<EventKindType> prevCauses,
                TriggeredEvent<EventKindType, ?> currentCause) {
            this.prevCauses = prevCauses;
            this.currentCause = currentCause;
            this.causeIterable = null;
            this.numberOfCauses = prevCauses != null
                    ? prevCauses.getNumberOfCauses() + 1
                    : 0;
        }

        @Override
        public int getNumberOfCauses() {
            return numberOfCauses;
        }

        @Override
        public Iterable<TriggeredEvent<EventKindType, ?>> getCauses() {
            Iterable<TriggeredEvent<EventKindType, ?>> result = causeIterable;
            if (result == null) {
                result = new Iterable<TriggeredEvent<EventKindType, ?>>() {
                    @Override
                    public Iterator<TriggeredEvent<EventKindType, ?>> iterator() {
                        return new LinkedCausesIterator<>(LinkedCauses.this);
                    }
                };
                causeIterable = result;
            }
            return result;
        }

        public TriggeredEvent<EventKindType, ?> getCurrentCause() {
            return currentCause;
        }
    }

    private static final class LinkedCausesIterator<EventKindType>
    implements
            Iterator<TriggeredEvent<EventKindType, ?>> {

        private LinkedCauses<EventKindType> currentCauses;

        public LinkedCausesIterator(LinkedCauses<EventKindType> currentCauses) {
            this.currentCauses = currentCauses;
        }

        @Override
        public boolean hasNext() {
            return currentCauses != null;
        }

        @Override
        public TriggeredEvent<EventKindType, ?> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            TriggeredEvent<EventKindType, ?> result = currentCauses.getCurrentCause();
            currentCauses = currentCauses.prevCauses;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove from event causes.");
        }
    }

    // Sets and restores the cause before running the wrapped task.
    private class TaskWrapperRunnable implements Runnable {
        private final LinkedCauses<EventKindType> cause;
        private final Runnable task;

        public TaskWrapperRunnable(LinkedCauses<EventKindType> cause, Runnable task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public void run() {
            LinkedCauses<EventKindType> prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                task.run();
            } finally {
                if (prevCause != null) {
                    currentCauses.set(prevCause);
                }
                else {
                    currentCauses.remove();
                }
            }
        }
    }

    // Sets and restores the cause before running the wrapped task.
    private class TaskWrapperCallable<V> implements Callable<V> {
        private final LinkedCauses<EventKindType> cause;
        private final Callable<V> task;

        public TaskWrapperCallable(LinkedCauses<EventKindType> cause, Callable<V> task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            LinkedCauses<EventKindType> prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                return task.call();
            } finally {
                if (prevCause != null) {
                    currentCauses.set(prevCause);
                }
                else {
                    currentCauses.remove();
                }
            }
        }
    }

    // Wraps tasks before submitting it to the wrapped executor
    // to set and restore the cause before running the submitted task.
    private final class TaskWrapperExecutor implements ExecutorService {
        private final ExecutorService wrapped;

        public TaskWrapperExecutor(ExecutorService wrapped) {
            ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

            this.wrapped = wrapped;
        }

        private <V> Callable<V> wrapTask(Callable<V> task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            LinkedCauses<EventKindType> cause = getCausesIfAny();
            return new TaskWrapperCallable<>(cause, task);
        }

        private Runnable wrapTask(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            LinkedCauses<EventKindType> cause = getCausesIfAny();
            return new TaskWrapperRunnable(cause, task);
        }

        private <T> Collection<Callable<T>> wrapManyTasks(Collection<? extends Callable<T>> tasks) {
            List<Callable<T>> result = new ArrayList<>(tasks.size());
            for (Callable<T> task: tasks) {
                result.add(wrapTask(task));
            }
            return result;
        }

        @Override
        public void shutdown() {
            wrapped.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return wrapped.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return wrapped.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return wrapped.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return wrapped.isTerminated();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return wrapped.submit(wrapTask(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return wrapped.submit(wrapTask(task), result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return wrapped.submit(wrapTask(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return wrapped.invokeAll(wrapManyTasks(tasks));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return wrapped.invokeAll(wrapManyTasks(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return wrapped.invokeAny(wrapManyTasks(tasks));
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return wrapped.invokeAny(wrapManyTasks(tasks), timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            wrapped.execute(wrapTask(command));
        }
    }
}
