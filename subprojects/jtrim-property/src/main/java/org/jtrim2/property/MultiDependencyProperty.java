package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.utils.ExceptionHelper;

abstract class MultiDependencyProperty<InputType, OutputType>
implements
        PropertySource<OutputType> {

    protected final PropertySource<? extends InputType>[] properties;

    @SafeVarargs
    @SuppressWarnings("varargs")
    public MultiDependencyProperty(PropertySource<? extends InputType>... properties) {
        this.properties = properties.clone();
        ExceptionHelper.checkNotNullElements(this.properties, "properties");
    }

    @Override
    public final ListenerRef addChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");

        ListenerRef[] refs = new ListenerRef[properties.length];

        try {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = properties[i].addChangeListener(listener);
            }
            return ListenerRefs.combineListenerRefs(refs);
        } catch (Throwable ex) {
            for (ListenerRef ref: refs) {
                try {
                    if (ref != null) {
                        ref.unregister();
                    }
                } catch (Throwable unregisterEx) {
                    ex.addSuppressed(unregisterEx);
                }
            }
            throw ex;
        }
    }
}
