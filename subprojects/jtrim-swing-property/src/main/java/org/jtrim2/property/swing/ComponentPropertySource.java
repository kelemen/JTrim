package org.jtrim2.property.swing;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.Objects;
import org.jtrim2.property.PropertySource;

/**
 * @see SwingProperties#componentPropertySource(Component, String, Class)
 */
final class ComponentPropertySource<ValueType>
implements
        SwingPropertySource<ValueType, PropertyChangeListener> {

    private static final String GET_PREFIX = "get";

    private final Component component;
    private final String propertyName;
    private final Method getterMethod;
    private final Class<ValueType> valueType;

    private ComponentPropertySource(
            Component component,
            String propertyName,
            Class<ValueType> valueType) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(valueType, "valueType");

        if (propertyName.isEmpty()) {
            throw new IllegalArgumentException("Property name cannot be empty.");
        }

        this.propertyName = propertyName;
        this.component = component;
        this.getterMethod = getGetterMethod(component, propertyName);
        this.valueType = valueType;

        if (!valueType.isAssignableFrom(getterMethod.getReturnType())) {
            throw new IllegalArgumentException("The getter method has an unexpected return type."
                    + " Expected: " + valueType.getName()
                    + ". Actual: " + getterMethod.getReturnType().getName());
        }
    }

    private static Method getGetterMethod(Component component, String propertyName) {
        String getterName = getterFromPropertyName(propertyName);
        try {
            return component.getClass().getMethod(getterName);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new IllegalArgumentException("Property cannot be accessed: " + propertyName, ex);
        }
    }

    private static String getterFromPropertyName(String propertyName) {
        char firstChar = Character.toUpperCase(propertyName.charAt(0));

        StringBuilder result = new StringBuilder(propertyName.length() + GET_PREFIX.length());
        result.append(GET_PREFIX);
        result.append(firstChar);
        result.append(propertyName, 1, propertyName.length());
        return result.toString();
    }

    public static <ValueType>PropertySource<ValueType> createProperty(
            Component component,
            String propertyName,
            Class<ValueType> valueType) {
        ComponentPropertySource<ValueType> property
                = new ComponentPropertySource<>(component, propertyName, valueType);
        return SwingProperties.fromSwingSource(property, ComponentPropertySource::createForwarder);
    }

    @Override
    public ValueType getValue() {
        try {
            return valueType.cast(getterMethod.invoke(component));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Unexpected error while retrieving property via the method " + getterMethod,
                    ex);
        }
    }

    @Override
    public void addChangeListener(PropertyChangeListener listener) {
        component.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removeChangeListener(PropertyChangeListener listener) {
        component.removePropertyChangeListener(propertyName, listener);
    }

    private static PropertyChangeListener createForwarder(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return (PropertyChangeEvent evt) -> listener.run();
    }
}
