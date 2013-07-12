package org.jtrim.property.swing;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#comboBoxSelection(JComboBox)
 *
 * @author Kelemen Attila
 */
final class ComboBoxSelectionPropertySource<ValueType>
implements
        SwingPropertySource<ValueType, ItemListener> {
    private final JComboBox<? extends ValueType> comboBox;

    private ComboBoxSelectionPropertySource(JComboBox<? extends ValueType> comboBox) {
        ExceptionHelper.checkNotNullArgument(comboBox, "comboBox");
        this.comboBox = comboBox;
    }

    public static <ValueType> MutableProperty<ValueType> createProperty(final JComboBox<ValueType> comboBox) {
        PropertySource<ValueType> source = SwingProperties.fromSwingSource(
                new ComboBoxSelectionPropertySource<>(comboBox),
                ListenerForwarderFactory.INSTANCE);

        return new AbstractMutableProperty<ValueType>(source) {
            @Override
            public void setValue(ValueType value) {
                comboBox.setSelectedItem(value);
            }
        };
    }

    @Override
    public ValueType getValue() {
        int index = comboBox.getSelectedIndex();
        return index >= 0 ? comboBox.getItemAt(index) : null;
    }

    @Override
    public void addChangeListener(ItemListener listener) {
        comboBox.addItemListener(listener);
    }

    @Override
    public void removeChangeListener(ItemListener listener) {
        comboBox.removeItemListener(listener);
    }

    private static final class ListenerForwarderFactory
    implements
            SwingForwarderFactory<ItemListener> {

        private static final ListenerForwarderFactory INSTANCE = new ListenerForwarderFactory();

        @Override
        public ItemListener createForwarder(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            return new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    listener.run();
                }
            };
        }
    }
}
