package org.jtrim.property.swing;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#spinnerValue(JSpinner)
 *
 * @author Kelemen Attila
 */
public final class SpinnerValuePropertySource implements SwingPropertySource<Object, ChangeListener> {
    private final JSpinner spinner;

    private SpinnerValuePropertySource(JSpinner spinner) {
        ExceptionHelper.checkNotNullArgument(spinner, "spinner");
        this.spinner = spinner;
    }

    public static MutableProperty<Object> createProperty(final JSpinner spinner) {
        PropertySource<Object> source = SwingProperties.fromSwingSource(
                new SpinnerValuePropertySource(spinner),
                ListenerForwarderFactory.INSTANCE);

        return new AbstractMutableProperty<Object>(source) {
            @Override
            public void setValue(Object value) {
                spinner.setValue(value);
            }
        };
    }

    @Override
    public Object getValue() {
        return spinner.getValue();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        spinner.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        spinner.removeChangeListener(listener);
    }

    private static final class ListenerForwarderFactory
    implements
            SwingForwarderFactory<ChangeListener> {

        private static final ListenerForwarderFactory INSTANCE = new ListenerForwarderFactory();

        @Override
        public ChangeListener createForwarder(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            return new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    listener.run();
                }
            };
        }
    }
}
