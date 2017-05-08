package org.jtrim2.property;

import org.jtrim2.collections.Equality;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @see PropertyFactory#lazilySetProperty(MutableProperty,EqualityComparator) 
 */
public class LazilySetPropertyTest {
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
        return new LazilySetProperty<>(wrapped, equality);
    }

    private static <ValueType> MutableProperty<ValueType> create(
            MutableProperty<ValueType> wrapped) {
        return create(wrapped, Equality.naturalEquality());
    }

    @Test
    public void testSetValue() {
        Object value = new Object();
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(value);
        MutableProperty<Object> property = create(wrapped);

        Object newValue = new Object();
        property.setValue(newValue);
        assertSame(newValue, wrapped.getValue());
    }

    @Test
    public void testGetValue() {
        Object value = new Object();
        MutableProperty<Object> property = create(PropertyFactory.memProperty(value));
        assertSame(value, property.getValue());
    }

    @Test
    public void testChangeListenerAfterSet() {
        MutableProperty<Object> property = create(PropertyFactory.memProperty(new Object()));

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        property.setValue(new Object());
        verify(listener).run();

        listenerRef.unregister();

        property.setValue(new Object());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testChangeListenerAfterWrappedSet() {
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(new Object());
        MutableProperty<Object> property = create(wrapped);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        wrapped.setValue(new Object());
        verify(listener).run();

        listenerRef.unregister();

        wrapped.setValue(new Object());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testLazinessOfChangeListener() {
        TestObjWithIdentity initialValue = new TestObjWithIdentity("INITIAL");
        MutableProperty<TestObjWithIdentity> wrapped = PropertyFactory.memProperty(initialValue);
        MutableProperty<TestObjWithIdentity> property = create(wrapped, TestObjWithIdentity.STR_CMP);

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);

        property.setValue(new TestObjWithIdentity("INITIAL"));
        verifyZeroInteractions(listener);

        property.setValue(new TestObjWithIdentity("NEW VALUE"));
        verify(listener).run();

        property.setValue(new TestObjWithIdentity("NEW VALUE"));
        verifyNoMoreInteractions(listener);
    }
}
