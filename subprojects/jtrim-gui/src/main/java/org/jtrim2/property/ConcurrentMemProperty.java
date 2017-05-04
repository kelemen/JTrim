package org.jtrim2.property;

import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see PropertyFactory#memPropertyConcurrent(Object, PropertyVerifier, PropertyPublisher, TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class ConcurrentMemProperty<ValueType> implements MutableProperty<ValueType> {
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
        ExceptionHelper.checkNotNullArgument(eventExecutor, "eventExecutor");

        this.value = verifier.storeValue(value);
        this.verifier = verifier;
        this.publisher = publisher;
        this.listeners = new CopyOnTriggerListenerManager<>();
        this.eventExecutor = new GenericUpdateTaskExecutor(TaskExecutors.inOrderExecutor(eventExecutor));
        this.eventDispatcherTask = () -> {
            EventListeners.dispatchRunnable(listeners);
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
