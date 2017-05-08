package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConverterPropertyTest {
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

    @SuppressWarnings("unchecked")
    private static PropertySource<Object> mockProperty() {
        return mock(PropertySource.class);
    }

    private static PropertySource<ObjectWrapper> create(
            PropertySource<?> source,
            ValueConverter<Object, ObjectWrapper> converter) {
        return new ConverterProperty<>(source, converter);
    }

    @Test
    public void testGetValue() {
        Object source = new Object();
        PropertySource<ObjectWrapper> property = create(
                PropertyFactory.constSource(source),
                WrapperConverter.INSTANCE);

        ObjectWrapper value = property.getValue();
        assertSame(source, value.wrapped);
    }

    @Test
    public void testAddChangeListener() {
        Runnable listener = mock(Runnable.class);
        PropertySource<Object> wrapped = mockProperty();

        PropertySource<ObjectWrapper> property = create(
                wrapped, WrapperConverter.INSTANCE);

        ListenerRef listenerRef = mock(ListenerRef.class);
        stub(wrapped.addChangeListener(any(Runnable.class))).toReturn(listenerRef);

        verifyZeroInteractions(wrapped);

        assertSame(listenerRef, property.addChangeListener(listener));

        verify(wrapped).addChangeListener(listener);
        verifyZeroInteractions(listener);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null, WrapperConverter.INSTANCE);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mockProperty(), null);
    }

    private static final class ObjectWrapper {
        public final Object wrapped;

        public ObjectWrapper(Object wrapped) {
            this.wrapped = wrapped;
        }
    }

    private enum WrapperConverter implements ValueConverter<Object, ObjectWrapper> {
        INSTANCE;

        @Override
        public ObjectWrapper convert(Object input) {
            return new ObjectWrapper(input);
        }
    }
}
