package org.jtrim.access;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.event.*;

/**
 * A convenient base class for {@link AccessToken} implementations.
 * <P>
 * This class implements the convenience methods defined by {@code AccessToken}
 * and those derived from
 * {@link java.util.concurrent.AbstractExecutorService AbstractExecutorService}.
 * The implementation of these convenience methods relies on the unimplemented
 * methods, so subclasses must avoid calling (directly or indirectly) these
 * convenience methods from the unimplemented methods of {@code AccessToken}
 * to avoid infinite recursion.
 * <P>
 * This implementation also implements the registration and unregistration of
 * event listeners but <B>subclasses must call
 * {@link #onTerminate() onTerminate()} to
 * notify the listeners of the terminate event</B>.
 *
 * @param <IDType> the type of the access ID (see
 *   {@link #getAccessID() getAccessID()})
 * @author Kelemen Attila
 */
public abstract class AbstractAccessToken<IDType> extends AbstractExecutorService
        implements AccessToken<IDType> {

    private volatile ListenerManager<AccessListener, Void> eventHandlers;
    private final EventDispatcher<AccessListener, Void> eventDispatcher;
    private final AtomicBoolean terminated;

    /**
     * Initializes a not terminated {@code AccessToken}.
     */
    public AbstractAccessToken() {
        this.terminated = new AtomicBoolean(false);
        this.eventHandlers = new CopyOnTriggerListenerManager<>();
        this.eventDispatcher = new AccessLostDispatcher();
    }

    /**
     * Calls the {@link AccessListener#onLostAccess() onLostAccess()} methods
     * of the currently registered listeners. After this method was called
     * new terminate listener registration requests will be discarded and
     * the already registered listeners will be available for garbage
     * collection, so unregistering listeners is not necessary.
     * <P>
     * This method is idempotent so calling it multiple times is allowed
     * and will have no effect.
     */
    protected final void onTerminate() {
        if (terminated.compareAndSet(false, true)) {
            eventHandlers.onEvent(eventDispatcher, null);
            // FIXME: even though we do not explicitly reference the
            //   eventHandlers anymore, it can be referenced by a returned
            //   ListenerRef.
            eventHandlers = new EventHandlerTrap();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ListenerRef<AccessListener> addAccessListener(AccessListener listener) {
        return eventHandlers.registerListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void release() {
        shutdownNow();

        boolean interrupted = false;

        while (!isTerminated()) {
            try {
                awaitTermination();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <T> T executeNow(Callable<T> task) {
        CallableTask<T> callable = new CallableTask<>(task);
        executeNow(callable);
        return callable.result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <T> T executeNowAndShutdown(Callable<T> task) {
        try {
            return executeNow(task);
        } finally {
            shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean executeNowAndShutdown(Runnable task) {
        try {
            return executeNow(task);
        } finally {
            shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void executeAndShutdown(Runnable task) {
        try {
            execute(task);
        } finally {
            shutdown();
        }
    }

    /**
     * This class can convert a {@link java.util.concurrent.Callable Callable}
     * to {@link Runnable} by storing the result in a public volatile field.
     */
    private static class CallableTask<T> implements Runnable {
        private final Callable<T> callable;
        public volatile T result;

        public CallableTask(Callable<T> callable) {
            this.callable = callable;
            this.result = null;
        }

        @Override
        public void run() {
            try {
                result = callable.call();
            } catch (Exception ex) {
                throw new TaskInvokeException(ex);
            }
        }
    }

    /**
     * This event handler will consume listener registrations and do nothing.
     * This is beneficial when the terminate event was already issued because
     * listeners registered afterwards cannot receive the terminate event.
     */
    private static class EventHandlerTrap
    implements
            ListenerManager<AccessListener, Void> {

        @Override
        public ListenerRef<AccessListener> registerListener(AccessListener listener) {
            return new UnregisteredListenerRef<>(listener);
        }

        @Override
        public int getListenerCount() {
            return 0;
        }

        @Override
        public void onEvent(
                EventDispatcher<? super AccessListener, ? super Void> eventDispatcher,
                Void arg) {
        }
    }

    private static class AccessLostDispatcher
    implements
            EventDispatcher<AccessListener, Void> {

        @Override
        public void onEvent(AccessListener listener, Void arg) {
            listener.onLostAccess();
        }
    }
}
