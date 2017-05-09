package org.jtrim2.property;

import org.jtrim2.collections.Equality;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.property.LazilyNotifiedPropertySourceTest.LazilyNotifiedPropertyCreator;
import org.junit.Test;

import static org.junit.Assert.*;

public class LazilyNotifiedMutablePropertyTest {
    private static <ValueType> MutableProperty<ValueType> create(
            MutableProperty<ValueType> wrapped,
            EqualityComparator<? super ValueType> equality) {
        return new LazilyNotifiedMutableProperty<>(wrapped, equality);
    }

    private static <ValueType> MutableProperty<ValueType> create(
            MutableProperty<ValueType> wrapped) {
        return create(wrapped, Equality.naturalEquality());
    }

    private static LazilyNotifiedPropertyCreator getFactory() {
        return LazilyNotifiedMutablePropertyTest::create;
    }

    /**
     * Test of setValue method, of class LazilyNotifiedMutableProperty.
     */
    @Test
    public void testSetValue() {
        Object value = new Object();
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(value);
        MutableProperty<Object> property = create(wrapped);

        Object newValue = new Object();
        property.setValue(newValue);
        assertSame(newValue, wrapped.getValue());
    }

    /**
     * Test of getValue method, of class LazilyNotifiedMutableProperty.
     */
    @Test
    public void testGetValue() {
        Object value = new Object();
        MutableProperty<Object> property = create(PropertyFactory.memProperty(value));
        assertSame(value, property.getValue());
    }

    @Test
    public void testLazyNotifications() {
        LazilyNotifiedPropertySourceTest.testLazyNotifications(getFactory());
    }

    @Test
    public void testConcurrentChangeDuringAddChangeListener() {
        LazilyNotifiedPropertySourceTest.testConcurrentChangeDuringAddChangeListener(getFactory());
    }
}
