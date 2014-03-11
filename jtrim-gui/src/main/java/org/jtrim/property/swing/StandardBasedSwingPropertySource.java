package org.jtrim.property.swing;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#toSwingSource(PropertySource, EventDispatcher)
 *
 * @author Kelemen Attila
 */
final class StandardBasedSwingPropertySource<ValueType, ListenerType>
implements
        SwingPropertySource<ValueType, ListenerType> {
    private static final Logger LOGGER = Logger.getLogger(StandardBasedSwingPropertySource.class.getName());

    private final PropertySource<? extends ValueType> property;
    private final EventDispatcher<? super ListenerType, Void> eventDispatcher;

    private final Lock listenersLock;
    private final Map<ListenerType, Counter> listeners;
    private ListenerRef forwarderRef;

    public StandardBasedSwingPropertySource(
            PropertySource<? extends ValueType> property,
            EventDispatcher<? super ListenerType, Void> eventDispatcher) {

        ExceptionHelper.checkNotNullArgument(property, "property");
        ExceptionHelper.checkNotNullArgument(eventDispatcher, "eventDispatcher");

        this.property = property;
        this.eventDispatcher = eventDispatcher;
        this.listeners = new IdentityHashMap<>();
        this.listenersLock = new ReentrantLock();
        this.forwarderRef = null;
    }

    @Override
    public ValueType getValue() {
        return property.getValue();
    }

    private void fireEvents() {
        List<CountedListener<ListenerType>> listenersToNotify;

        listenersLock.lock();
        try {
            if (listeners.isEmpty()) {
                return;
            }

            listenersToNotify = new ArrayList<>(listeners.size());
            for (Map.Entry<ListenerType, Counter> entry: listeners.entrySet()) {
                listenersToNotify.add(new CountedListener<>(entry.getValue().count, entry.getKey()));
            }
        } finally {
            listenersLock.unlock();
        }

        for (CountedListener<ListenerType> listener: listenersToNotify) {
            for (int i = 0; i < listener.registerCount; i++) {
                try {
                    eventDispatcher.onEvent(listener.listener, null);
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected exception thrown by a listener.", ex);
                }
            }
        }
    }

    @Override
    public void addChangeListener(ListenerType listener) {
        if (listener == null) {
            return;
        }

        listenersLock.lock();
        try {
            if (forwarderRef == null) {
                forwarderRef = property.addChangeListener(new Runnable() {
                    @Override
                    public void run() {
                        fireEvents();
                    }
                });
            }

            Counter counter = listeners.get(listener);
            if (counter == null) {
                counter = new Counter();
                listeners.put(listener, counter);
            }
            else {
                counter.count++;
            }
        } finally {
            listenersLock.unlock();
        }
    }

    @Override
    public void removeChangeListener(ListenerType listener) {
        if (listener == null) {
            return;
        }

        ListenerRef toUnregister = null;

        listenersLock.lock();
        try {
            Counter counter = listeners.get(listener);
            if (counter == null) {
                return;
            }

            if (counter.count == 1) {
                listeners.remove(listener);

                if (listeners.isEmpty()) {
                    toUnregister = forwarderRef;
                    forwarderRef = null;
                }
            }
            else {
                counter.count--;
            }
        } finally {
            listenersLock.unlock();
        }

        if (toUnregister != null) {
            toUnregister.unregister();
        }
    }

    private static final class CountedListener<ListenerType> {
        public final int registerCount;
        public final ListenerType listener;

        public CountedListener(int registerCount, ListenerType listener) {
            this.registerCount = registerCount;
            this.listener = listener;
        }
    }

    private static final class Counter {
        public int count = 1;
    }
}
