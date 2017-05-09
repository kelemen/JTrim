package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class MultiDependencyPropertyTest {
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
        ChangeListenerRobustnessTests<?> tests = new ChangeListenerRobustnessTests<>(MultiDependencyPropertyImpl::new);
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
