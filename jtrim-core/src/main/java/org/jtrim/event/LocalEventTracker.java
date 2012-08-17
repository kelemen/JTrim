package org.jtrim.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.utils.ExceptionHelper;

/**
 * An {@code EventTracker} implementation allowing to remove all the registered
 * event handlers in a single method call. Other than this, this class only
 * forwards its method calls to an {@code EventTracker} specified at
 * construction time.
 * <P>
 * This class is intended to be used when an {@code EventTracker} can be
 * replaced and when it was replaced, the event handlers are needed to be
 * registered again. This class is expected to be used the following way:
 * <pre>
 * LocalEventTracker eventTracker = null;
 * void setEventTracker(EventTracker newEventTracker) {
 *   if (eventTracker != null) {
 *     eventTracker.removeAllListeners();
 *   }
 *   eventTracker = new LocalEventTracker(newEventTracker);
 *   // Register events with "eventTracker" ...
 * }
 * </pre>
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
public final class LocalEventTracker implements EventTracker {
    private final EventTracker wrappedTracker;

    private final Lock mainLock;
    private final RefList<ListenerRef> localRefs;

    /**
     * Creates a new event tracker delegating its calls to the specified
     * {@code LocalEventTracker}.
     *
     * @param wrappedTracker the {@code EventTracker} to which calls will be
     *   forwarded. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public LocalEventTracker(EventTracker wrappedTracker) {
        ExceptionHelper.checkNotNullArgument(wrappedTracker, "wrappedTracker");
        this.wrappedTracker = wrappedTracker;
        this.mainLock = new ReentrantLock();
        this.localRefs = new RefLinkedList<>();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <ArgType> TrackedListenerManager<ArgType> getManagerOfType(
            Object eventKind, Class<ArgType> argType) {
        return new LocalTrackedListenerManager<>(eventKind, argType);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskExecutor createTrackedExecutor(TaskExecutor executor) {
        return wrappedTracker.createTrackedExecutor(executor);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskExecutorService createTrackedExecutorService(
            TaskExecutorService executor) {
        return wrappedTracker.createTrackedExecutorService(executor);
    }

    /**
     * Unregisters all the event handlers previously registered to any
     * {@link TrackedListenerManager} created by this {@code LocalEventTracker}.
     * <P>
     * Note that this method may ignore event handlers registered or
     * unregistered concurrently with this method call.
     */
    public void removeAllListeners() {
        List<ListenerRef> toRemove;
        mainLock.lock();
        try {
            toRemove = new ArrayList<>(localRefs);
            localRefs.clear();
        } finally {
            mainLock.unlock();
        }

        for (ListenerRef listenerRef: toRemove) {
            listenerRef.unregister();
        }
    }

    private class LocalTrackedListenerManager<ArgType>
    implements
            TrackedListenerManager<ArgType> {

        private final TrackedListenerManager<ArgType> wrappedManager;

        public LocalTrackedListenerManager(Object eventKind, Class<ArgType> argType) {
            this.wrappedManager = wrappedTracker.getManagerOfType(eventKind, argType);
        }

        @Override
        public void onEvent(ArgType arg) {
            wrappedManager.onEvent(arg);
        }

        @Override
        public ListenerRef registerListener(
                TrackedEventListener<ArgType> listener) {
            final ListenerRef result = wrappedManager.registerListener(listener);

            final RefList.ElementRef<ListenerRef> resultRef;
            mainLock.lock();
            try {
                resultRef = localRefs.addLastGetReference(result);
            } finally {
                mainLock.unlock();
            }

            return new ListenerRef() {
                @Override
                public boolean isRegistered() {
                    return result.isRegistered();
                }

                @Override
                public void unregister() {
                    mainLock.lock();
                    try {
                        resultRef.remove();
                    } finally {
                        mainLock.unlock();
                    }
                    result.unregister();
                }
            };
        }

        @Override
        public int getListenerCount() {
            return wrappedManager.getListenerCount();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LocalTrackedListenerManager<?> other
                    = (LocalTrackedListenerManager<?>)obj;
            if (!Objects.equals(this.wrappedManager, other.wrappedManager)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(wrappedManager);
            return hash;
        }
    }
}
