
package org.jtrim.property;

import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ConcurrentMemProperty<ValueType> implements MutableProperty<ValueType> {
    private volatile ValueType value;
    private final PropertyVerifier<ValueType> verifier;
    private final PropertyPublisher<ValueType> publisher;
    private final ListenerManager<Runnable> listeners;
    private final UpdateTaskExecutor eventExecutor;
    private final Runnable eventDispatcherTask;

    public ConcurrentMemProperty(
            ValueType value,
            PropertyVerifier<ValueType> verifier,
            PropertyPublisher<ValueType> publisher,
            TaskExecutor eventExecutor) {
        ExceptionHelper.checkNotNullArgument(verifier, "verifier");
        ExceptionHelper.checkNotNullArgument(publisher, "publisher");

        this.value = verifier.storeValue(value);
        this.verifier = verifier;
        this.publisher = publisher;
        this.listeners = new CopyOnTriggerListenerManager<>();
        this.eventExecutor = new GenericUpdateTaskExecutor(TaskExecutors.inOrderExecutor(eventExecutor));
        this.eventDispatcherTask = new Runnable() {
            @Override
            public void run() {
                EventListeners.dispatchRunnable(listeners);
            }
        };
    }

    @Override
    public void setValue(ValueType value) {
        this.value = verifier.storeValue(value);
        eventExecutor.execute(eventDispatcherTask);
    }

    @Override
    public ValueType getValue() {
        return publisher.returnValue(value);
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return listeners.registerListener(listener);
    }
}
