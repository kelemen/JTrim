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

    public LinkedEventTracker() {
        this.registerLock = new ReentrantLock();
        this.currentCauses = new ThreadLocal<>();
        this.managers = new ConcurrentHashMap<>();
    }

    @Override
    public <ArgType> TrackedListenerManager<ArgType> getManagerOfType(
            Object eventKind, Class<ArgType> argType) {
        ExceptionHelper.checkNotNullArgument(eventKind, "eventKind");
        ExceptionHelper.checkNotNullArgument(argType, "argType");

        ManagerKey key = new ManagerKey(eventKind, argType);
        return new TrackedListenerManagerImpl<>(key);
    }

    @Override
    public Executor createTrackedExecutor(final Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        return new Executor() {
            @Override
            public void execute(Runnable command) {
                LinkedCauses cause = getCausesIfAny();
                executor.execute(new TaskWrapperRunnable(cause, command));
            }
        };
    }

    @Override
    public ExecutorService createTrackedExecutorService(ExecutorService executor) {
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

        public ListenerManager<TrackedEventListener<ArgType>, TrackedEvent<ArgType>> getManager() {
            return manager;
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

            TriggeredEvent<ArgType> triggeredEvent;
            triggeredEvent = new TriggeredEvent<>(key.getEventKind(), arg);

            TrackedEvent<ArgType> trackedEvent = causes != null
                    ? new TrackedEvent<>(causes, arg)
                    : new TrackedEvent<>(arg);

            try {
                currentCauses.set(new LinkedCauses(causes, triggeredEvent));
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
        public ListenerRef<TrackedEventListener<ArgType>> registerListener(
                final TrackedEventListener<ArgType> listener) {

            ListenerRef<TrackedEventListener<ArgType>> resultRef;

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
                    resultRef = managerHolder.getManager().registerListener(listener);
                } finally {
                    registerLock.unlock();
                }

                prevManagerHolder = managerHolder;
                managerHolder = getAndCast();
            } while (managerHolder != prevManagerHolder);

            final ManagerHolder<ArgType> chosenManagerHolder = managerHolder;
            final ListenerRef<TrackedEventListener<ArgType>> chosenRef = resultRef;

            return new ListenerRef<TrackedEventListener<ArgType>>() {
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
                public TrackedEventListener<ArgType> getListener() {
                    return listener;
                }
            };
        }

        @Override
        public int getListenerCount() {
            ManagerHolder<ArgType> managerHolder = getAndCast();
            return managerHolder != null
                    ? managerHolder.getManager().getListenerCount()
                    : 0;
        }
    }

    private static final class LinkedCauses extends AbstractEventCauses {
        private final int numberOfCauses;
        private final LinkedCauses prevCauses;
        private final TriggeredEvent<?> currentCause;
        private volatile Iterable<TriggeredEvent<?>> causeIterable;

        public LinkedCauses(LinkedCauses prevCauses, TriggeredEvent<?> currentCause) {
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
            throw new UnsupportedOperationException("Cannot remove from event causes.");
        }
    }

    // Sets and restores the cause before running the wrapped task.
    private class TaskWrapperRunnable implements Runnable {
        private final LinkedCauses cause;
        private final Runnable task;

        public TaskWrapperRunnable(LinkedCauses cause, Runnable task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public void run() {
            LinkedCauses prevCause = currentCauses.get();
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
        private final LinkedCauses cause;
        private final Callable<V> task;

        public TaskWrapperCallable(LinkedCauses cause, Callable<V> task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            LinkedCauses prevCause = currentCauses.get();
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
            LinkedCauses cause = getCausesIfAny();
            return new TaskWrapperCallable<>(cause, task);
        }

        private Runnable wrapTask(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            LinkedCauses cause = getCausesIfAny();
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
