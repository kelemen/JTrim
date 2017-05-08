package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;

/**
 * @see PropertyFactory#convert(PropertySource, ValueConverter)
 */
final class ConverterProperty<InputType, ValueType> implements PropertySource<ValueType> {
    private final PropertySource<? extends InputType> source;
    private final ValueConverter<? super InputType, ? extends ValueType> converter;

    public ConverterProperty(
            PropertySource<? extends InputType> source,
            ValueConverter<? super InputType, ? extends ValueType> converter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(converter, "converter");

        this.source = source;
        this.converter = converter;
    }

    @Override
    public ValueType getValue() {
        return converter.convert(source.getValue());
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return source.addChangeListener(listener);
    }
}
