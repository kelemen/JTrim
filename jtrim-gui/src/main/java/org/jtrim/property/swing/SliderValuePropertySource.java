package org.jtrim.property.swing;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#sliderValue(JSlider)
 *
 * @author Kelemen Attila
 */
final class SliderValuePropertySource implements SwingPropertySource<Integer, ChangeListener> {
    private final JSlider slider;

    private SliderValuePropertySource(JSlider slider) {
        ExceptionHelper.checkNotNullArgument(slider, "slider");
        this.slider = slider;
    }

    public static MutableProperty<Integer> createProperty(final JSlider slider) {
        PropertySource<Integer> source = SwingProperties.fromSwingSource(
                new SliderValuePropertySource(slider),
                SliderValuePropertySource::createForwarder);

        return new AbstractMutableProperty<Integer>(source) {
            @Override
            public void setValue(Integer value) {
                slider.setValue(value);
            }
        };
    }

    @Override
    public Integer getValue() {
        return slider.getValue();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        slider.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        slider.removeChangeListener(listener);
    }

    private static ChangeListener createForwarder(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        return (ChangeEvent e) -> listener.run();
    }
}
