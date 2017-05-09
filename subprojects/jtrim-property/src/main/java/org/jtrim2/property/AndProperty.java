package org.jtrim2.property;

final class AndProperty extends MultiDependencyProperty<Boolean, Boolean> {
    @SafeVarargs
    @SuppressWarnings("varargs")
    public AndProperty(PropertySource<Boolean>... properties) {
        super(properties);
    }

    @Override
    public Boolean getValue() {
        for (PropertySource<? extends Boolean> property: properties) {
            Boolean value = property.getValue();
            if (value != null && !value.booleanValue()) {
                return false;
            }
        }
        return true;
    }
}
