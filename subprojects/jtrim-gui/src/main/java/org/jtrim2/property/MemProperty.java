package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;

/**
 * @see PropertyFactory#memProperty(Object, PropertyVerifier, PropertyPublisher)
 *
 * @author Kelemen Attila
 */
final class MemProperty<ValueType> implements MutableProperty<ValueType> {
    private volatile ValueType value;
    private final PropertyVerifier<ValueType> verifier;
    private final PropertyPublisher<ValueType> publisher;
    private final ListenerManager<Runnable> listeners;

    public MemProperty(
            ValueType value,
            PropertyVerifier<ValueType> verifier,
            PropertyPublisher<ValueType> publisher) {
        Objects.requireNonNull(verifier, "verifier");
        Objects.requireNonNull(publisher, "publisher");

        this.value = verifier.storeValue(value);
        this.verifier = verifier;
        this.publisher = publisher;
        this.listeners = new CopyOnTriggerListenerManager<>();
    }

    @Override
    public void setValue(ValueType value) {
        this.value = verifier.storeValue(value);
        EventListeners.dispatchRunnable(listeners);
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
