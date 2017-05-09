package org.jtrim2.property.swing;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;
import javax.swing.JComboBox;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;

/**
 * @see SwingProperties#comboBoxSelection(JComboBox)
 */
final class ComboBoxSelectionPropertySource<ValueType>
implements
        SwingPropertySource<ValueType, ItemListener> {
    private final JComboBox<? extends ValueType> comboBox;

    private ComboBoxSelectionPropertySource(JComboBox<? extends ValueType> comboBox) {
        Objects.requireNonNull(comboBox, "comboBox");
        this.comboBox = comboBox;
    }

    public static <ValueType> MutableProperty<ValueType> createProperty(final JComboBox<ValueType> comboBox) {
        PropertySource<ValueType> source = SwingProperties.fromSwingSource(
                new ComboBoxSelectionPropertySource<>(comboBox),
                ComboBoxSelectionPropertySource::createForwarder);

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

    private static ItemListener createForwarder(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return (ItemEvent e) -> listener.run();
    }
}
