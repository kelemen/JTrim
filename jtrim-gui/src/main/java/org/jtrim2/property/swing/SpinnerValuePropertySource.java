package org.jtrim2.property.swing;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see SwingProperties#spinnerValue(JSpinner)
 *
 * @author Kelemen Attila
 */
final class SpinnerValuePropertySource implements SwingPropertySource<Object, ChangeListener> {
    private final JSpinner spinner;

    private SpinnerValuePropertySource(JSpinner spinner) {
        ExceptionHelper.checkNotNullArgument(spinner, "spinner");
        this.spinner = spinner;
    }

    public static MutableProperty<Object> createProperty(final JSpinner spinner) {
        PropertySource<Object> source = SwingProperties.fromSwingSource(
                new SpinnerValuePropertySource(spinner),
                SpinnerValuePropertySource::createForwarder);

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

    private static ChangeListener createForwarder(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        return (ChangeEvent e) -> listener.run();
    }
}
