package org.jtrim2.property;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;

final class PropertyOfProperty<S, N> implements PropertySource<N> {
    private final PropertySource<? extends S> rootSrc;
    private final Function<? super S, ? extends PropertySource<? extends N>> nestedPropertyGetter;

    public PropertyOfProperty(
            PropertySource<? extends S> rootSrc,
            Function<? super S, ? extends PropertySource<? extends N>> nestedPropertyGetter) {
        this.rootSrc = Objects.requireNonNull(rootSrc, "rootSrc");
        this.nestedPropertyGetter = Objects.requireNonNull(nestedPropertyGetter, "nestedPropertyGetter");
    }

    private PropertySource<? extends N> getNestedProperty() {
        S rootValue = rootSrc.getValue();
        return nestedPropertyGetter.apply(rootValue);
    }

    @Override
    public N getValue() {
        return getNestedProperty().getValue();
    }

    private void registerWithNestedListener(Runnable listener, AtomicReference<ListenerRef> nestedListenerRef) {
        ListenerRef newRef = getNestedProperty().addChangeListener(listener);
        ListenerRef prevRef = nestedListenerRef.getAndSet(newRef);
        if (prevRef != null) {
            prevRef.unregister();
        } else {
            nestedListenerRef.compareAndSet(newRef, null);
            newRef.unregister();
        }
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        AtomicReference<ListenerRef> nestedListenerRef = new AtomicReference<>(ListenerRefs.unregistered());
        // nestedListenerRef.get() == null means that the the client
        // unregistered its listener and therefore, we must no longer
        // register listeners. That is, once this property is null, we may
        // never set it.

        ListenerRef listenerRef = rootSrc.addChangeListener(() -> {
            registerWithNestedListener(listener, nestedListenerRef);
            listener.run();
        });

        registerWithNestedListener(listener, nestedListenerRef);

        return () -> {
            listenerRef.unregister();
            ListenerRef nestedRef = nestedListenerRef.getAndSet(null);
            if (nestedRef != null) {
                nestedRef.unregister();
            }
        };
    }
}
