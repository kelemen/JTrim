package org.jtrim.property;

import org.jtrim.collections.Equality;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.property.LazilyNotifiedPropertySourceTest.LazilyNotifiedPropertyCreator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class LazilyNotifiedMutablePropertyTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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
        return new LazilyNotifiedPropertyCreator() {
            @Override
            public <ValueType> PropertySource<ValueType> newProperty(
                    MutableProperty<ValueType> wrapped,
                    EqualityComparator<? super ValueType> equality) {
                return create(wrapped, equality);
            }
        };
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
