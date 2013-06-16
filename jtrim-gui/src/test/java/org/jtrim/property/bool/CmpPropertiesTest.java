package org.jtrim.property.bool;

import java.util.concurrent.atomic.AtomicInteger;
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
public class CmpPropertiesTest {
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

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());
        assertTrue(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testNotEqual() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ1"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ2"));

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());
        assertFalse(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testChange1() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ"));

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());

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

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = cmpProperty.addChangeListener(listener);

        property2.setValue(new TestObjWithIdentity("OBJ_A"));
        verify(listener).run();

        listenerRef.unregister();

        property2.setValue(new TestObjWithIdentity("OBJ_B"));
        verifyNoMoreInteractions(listener);
    }

    @SuppressWarnings("unchecked")
    private static <ValueType> PropertySource<ValueType> mockProperty() {
        return mock(PropertySource.class);
    }

    @Test
    public void testFailingAddChangeListener1() {
        PropertySource<TestObjWithIdentity> property1 = mockProperty();
        ListenerCounterProperty<TestObjWithIdentity> property2 = ListenerCounterProperty.TEST_PROPERTY;

        Throwable error = new RuntimeException();
        stub(property1.addChangeListener(any(Runnable.class))).toThrow(error);

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());

        try {
            cmpProperty.addChangeListener(mock(Runnable.class));
            fail("Expected exception.");
        } catch (Throwable ex) {
            assertSame(error, ex);
        }

        assertEquals(0, property2.getRegisteredListenerCount());
    }

    @Test
    public void testFailingAddChangeListener2() {
        ListenerCounterProperty<TestObjWithIdentity> property1 = ListenerCounterProperty.TEST_PROPERTY;
        PropertySource<TestObjWithIdentity> property2 = mockProperty();

        Throwable error = new RuntimeException();
        stub(property2.addChangeListener(any(Runnable.class))).toThrow(error);

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());

        try {
            cmpProperty.addChangeListener(mock(Runnable.class));
            fail("Expected exception.");
        } catch (Throwable ex) {
            assertSame(error, ex);
        }

        assertEquals(0, property1.getRegisteredListenerCount());
    }

    @Test
    public void testFailingAddChangeListener1ReturnsNull() {
        PropertySource<TestObjWithIdentity> property1 = mockProperty();
        ListenerCounterProperty<TestObjWithIdentity> property2 = ListenerCounterProperty.TEST_PROPERTY;

        stub(property1.addChangeListener(any(Runnable.class))).toReturn(null);

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());
        try {
            cmpProperty.addChangeListener(mock(Runnable.class));
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
        }

        assertEquals(0, property2.getRegisteredListenerCount());
    }

    @Test
    public void testFailingAddChangeListener2ReturnsNull() {
        ListenerCounterProperty<TestObjWithIdentity> property1 = ListenerCounterProperty.TEST_PROPERTY;
        PropertySource<TestObjWithIdentity> property2 = mockProperty();

        stub(property2.addChangeListener(any(Runnable.class))).toReturn(null);

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());
        try {
            cmpProperty.addChangeListener(mock(Runnable.class));
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
        }

        assertEquals(0, property1.getRegisteredListenerCount());
    }

    @Test
    public void testFailingAddChangeListener1FailingUnregister() {
        PropertySource<TestObjWithIdentity> property1 = mockProperty();
        PropertySource<TestObjWithIdentity> property2 = mockProperty();

        RuntimeException unregisterError = new RuntimeException();
        FailingListenerRef failingListenerRef = new FailingListenerRef(unregisterError);

        Throwable error = new RuntimeException();
        stub(property1.addChangeListener(any(Runnable.class))).toThrow(error);
        stub(property2.addChangeListener(any(Runnable.class))).toReturn(failingListenerRef);

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());
        try {
            cmpProperty.addChangeListener(mock(Runnable.class));
            fail("Expected exception");
        } catch (Throwable ex) {
            assertSame(error, ex);
            if (failingListenerRef.getUnregisterCallCount() > 0) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(unregisterError, suppressed[0]);
            }
        }
    }

    @Test
    public void testFailingAddChangeListener2FailingUnregister() {
        PropertySource<TestObjWithIdentity> property1 = mockProperty();
        PropertySource<TestObjWithIdentity> property2 = mockProperty();

        RuntimeException unregisterError = new RuntimeException();
        FailingListenerRef failingListenerRef = new FailingListenerRef(unregisterError);

        stub(property1.addChangeListener(any(Runnable.class))).toReturn(failingListenerRef);

        Throwable error = new RuntimeException();
        stub(property2.addChangeListener(any(Runnable.class))).toThrow(error);

        CmpProperties cmpProperty = new CmpProperties(property1, property2, testObjCmp());
        try {
            cmpProperty.addChangeListener(mock(Runnable.class));
            fail("Expected exception");
        } catch (Throwable ex) {
            assertSame(error, ex);
            if (failingListenerRef.getUnregisterCallCount() > 0) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(unregisterError, suppressed[0]);
            }
        }
    }

    private static final class FailingListenerRef implements ListenerRef {
        private final RuntimeException error;
        private final AtomicInteger unregisterCallCount;

        public FailingListenerRef(RuntimeException error) {
            ExceptionHelper.checkNotNullArgument(error, "error");
            this.error = error;
            this.unregisterCallCount = new AtomicInteger(0);
        }

        @Override
        public boolean isRegistered() {
            return true;
        }

        @Override
        public void unregister() {
            unregisterCallCount.incrementAndGet();
            throw error;
        }

        public int getUnregisterCallCount() {
            return unregisterCallCount.get();
        }
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
