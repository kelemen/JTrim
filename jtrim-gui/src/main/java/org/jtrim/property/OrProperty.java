package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
final class OrProperty extends MultiDependencyProperty<Boolean, Boolean> {
    @SafeVarargs
    @SuppressWarnings("varargs")
    public OrProperty(PropertySource<Boolean>... properties) {
        super(properties);
    }

    @Override
    public Boolean getValue() {
        for (PropertySource<? extends Boolean> property: properties) {
            Boolean value = property.getValue();
            if (value != null && value.booleanValue()) {
                return true;
            }
        }
        return false;
    }
}
