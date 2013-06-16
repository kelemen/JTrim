package org.jtrim.property.bool;

import org.jtrim.collections.Comparators;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class MultiDependencyPropertyTest {
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

    @Test
    public void testSuccessfulAddChangeListener() {
        PropertySource<Object> property1 = BoolPropertiesTest.mockProperty();
        PropertySource<Object> property2 = BoolPropertiesTest.mockProperty();

        stub(property1.addChangeListener(any(Runnable.class))).toReturn(mock(ListenerRef.class));
        stub(property2.addChangeListener(any(Runnable.class))).toReturn(mock(ListenerRef.class));

        MultiDependencyPropertyImpl tested = new MultiDependencyPropertyImpl(property1, property2);

        Runnable listener = mock(Runnable.class);
        tested.addChangeListener(listener);
        verify(property1).addChangeListener(listener);
        verify(property2).addChangeListener(listener);
    }

    @Test
    public void testAddChangeListenerRobustness() {
        MultiPropertyFactory<?, ?> factory = new MultiPropertyFactory<Object, Object>() {
            @Override
            public PropertySource<Object> create(
                    PropertySource<Object> property1,
                    PropertySource<Object> property2) {
                return new MultiDependencyPropertyImpl(property1, property2);
            }
        };

        ChangeListenerRobustnessTests<?> tests = new ChangeListenerRobustnessTests<>(factory);
        tests.runTests();
    }

    public class MultiDependencyPropertyImpl extends MultiDependencyProperty<Object, Object> {
        @SafeVarargs
        @SuppressWarnings("varargs")
        public MultiDependencyPropertyImpl(PropertySource<Object>... properties) {
            super(properties);
        }


        @Override
        public Object getValue() {
            return null;
        }
    }

}
