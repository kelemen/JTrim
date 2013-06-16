package org.jtrim.property.bool;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.collections.Comparators;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim.property.PropertyFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class CmpPropertyTest {
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

    private static EqualityComparator<TestObjWithIdentity> testObjCmp() {
        return TestObjWithIdentity.STR_CMP;
    }

    @Test
    public void testEqual() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ1"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ1"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());
        assertTrue(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testNotEqual() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ1"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ2"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());
        assertFalse(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testChange1() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = cmpProperty.addChangeListener(listener);

        property1.setValue(new TestObjWithIdentity("OBJ_A"));
        verify(listener).run();

        listenerRef.unregister();

        property1.setValue(new TestObjWithIdentity("OBJ_B"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testChange2() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = cmpProperty.addChangeListener(listener);

        property2.setValue(new TestObjWithIdentity("OBJ_A"));
        verify(listener).run();

        listenerRef.unregister();

        property2.setValue(new TestObjWithIdentity("OBJ_B"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAddChangeListenerRobustness() {
        MultiPropertyFactory<?, ?> factory = new MultiPropertyFactory<Object, Boolean>() {
            @Override
            public PropertySource<Boolean> create(
                    PropertySource<Object> property1,
                    PropertySource<Object> property2) {
                return new CmpProperty(property1, property2, Comparators.naturalEquality());
            }
        };

        ChangeListenerRobustnessTests<?> tests = new ChangeListenerRobustnessTests<>(factory);
        tests.runTests();
    }

    private static final class ListenerCounterProperty<ValueType> implements PropertySource<ValueType> {
        public static final ListenerCounterProperty<TestObjWithIdentity> TEST_PROPERTY
                = new ListenerCounterProperty<>(TestObjWithIdentity.EMPTY);

        private final AtomicInteger regCount;
        private final ValueType value;

        public ListenerCounterProperty(ValueType value) {
            this.regCount = new AtomicInteger(0);
            this.value = value;
        }

        public int getRegisteredListenerCount() {
            return regCount.get();
        }

        @Override
        public ValueType getValue() {
            return value;
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            final Runnable unregisterTask = Tasks.runOnceTask(new Runnable() {
                @Override
                public void run() {
                    regCount.decrementAndGet();
                }
            }, false);

            regCount.incrementAndGet();
            return new ListenerRef() {
                private volatile boolean registered = true;

                @Override
                public boolean isRegistered() {
                    return registered;
                }

                @Override
                public void unregister() {
                    unregisterTask.run();
                    registered = false;
                }
            };
        }
    }
}
