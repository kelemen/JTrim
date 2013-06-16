package org.jtrim.property.bool;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
abstract class MultiDependencyProperty<InputType, OutputType>
implements
        PropertySource<OutputType> {

    protected final PropertySource<InputType>[] properties;

    @SafeVarargs
    @SuppressWarnings("varargs")
    public MultiDependencyProperty(PropertySource<InputType>... properties) {
        this.properties = properties.clone();
        ExceptionHelper.checkNotNullElements(this.properties, "properties");
    }

    @Override
    public final ListenerRef addChangeListener(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        ListenerRef[] refs = new ListenerRef[properties.length];

        try {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = properties[i].addChangeListener(listener);
            }
            return ListenerRegistries.combineListenerRefs(refs);
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
