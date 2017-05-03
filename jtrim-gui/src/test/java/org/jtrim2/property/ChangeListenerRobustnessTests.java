package org.jtrim2.property;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @param <InputType> the type of the value of the properties wrapped by the
 *   tested property
 *
 * @author Kelemen Attila
 */
public final class ChangeListenerRobustnessTests<InputType> {
    private final MultiPropertyFactory<InputType, ?> factory;

    public ChangeListenerRobustnessTests(MultiPropertyFactory<InputType, ?> factory) {
        ExceptionHelper.checkNotNullArgument(factory, "factory");
        this.factory = factory;
    }

    public void runTests() {
        testFailingAddChangeListener1();
        testFailingAddChangeListener1FailingUnregister();
        testFailingAddChangeListener1ReturnsNull();
        testFailingAddChangeListener2();
        testFailingAddChangeListener2FailingUnregister();
        testFailingAddChangeListener2ReturnsNull();
    }

    private void testFailingAddChangeListener1() {
        PropertySource<InputType> property1 = BoolPropertiesTest.mockProperty();
        ListenerCounterProperty<InputType> property2 = new ListenerCounterProperty<>(null);

        Throwable error = new RuntimeException();
        stub(property1.addChangeListener(any(Runnable.class))).toThrow(error);

        PropertySource<?> tested = factory.create(property1, property2);

        try {
            tested.addChangeListener(mock(Runnable.class));
            fail("Expected exception.");
        } catch (Throwable ex) {
            assertSame(error, ex);
        }

        assertEquals(0, property2.getRegisteredListenerCount());
    }

    private void testFailingAddChangeListener2() {
        ListenerCounterProperty<InputType> property1 = new ListenerCounterProperty<>(null);
        PropertySource<InputType> property2 = BoolPropertiesTest.mockProperty();

        Throwable error = new RuntimeException();
        stub(property2.addChangeListener(any(Runnable.class))).toThrow(error);

        PropertySource<?> tested = factory.create(property1, property2);

        try {
            tested.addChangeListener(mock(Runnable.class));
            fail("Expected exception.");
        } catch (Throwable ex) {
            assertSame(error, ex);
        }

        assertEquals(0, property1.getRegisteredListenerCount());
    }

    private void testFailingAddChangeListener1ReturnsNull() {
        PropertySource<InputType> property1 = BoolPropertiesTest.mockProperty();
        ListenerCounterProperty<InputType> property2 = new ListenerCounterProperty<>(null);

        stub(property1.addChangeListener(any(Runnable.class))).toReturn(null);

        PropertySource<?> tested = factory.create(property1, property2);
        try {
            tested.addChangeListener(mock(Runnable.class));
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
        }

        assertEquals(0, property2.getRegisteredListenerCount());
    }

    private void testFailingAddChangeListener2ReturnsNull() {
        ListenerCounterProperty<InputType> property1 = new ListenerCounterProperty<>(null);
        PropertySource<InputType> property2 = BoolPropertiesTest.mockProperty();

        stub(property2.addChangeListener(any(Runnable.class))).toReturn(null);

        PropertySource<?> tested = factory.create(property1, property2);
        try {
            tested.addChangeListener(mock(Runnable.class));
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
        }

        assertEquals(0, property1.getRegisteredListenerCount());
    }

    private void testFailingAddChangeListener1FailingUnregister() {
        PropertySource<InputType> property1 = BoolPropertiesTest.mockProperty();
        PropertySource<InputType> property2 = BoolPropertiesTest.mockProperty();

        RuntimeException unregisterError = new RuntimeException();
        FailingListenerRef failingListenerRef = new FailingListenerRef(unregisterError);

        Throwable error = new RuntimeException();
        stub(property1.addChangeListener(any(Runnable.class))).toThrow(error);
        stub(property2.addChangeListener(any(Runnable.class))).toReturn(failingListenerRef);

        PropertySource<?> tested = factory.create(property1, property2);
        try {
            tested.addChangeListener(mock(Runnable.class));
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

    private void testFailingAddChangeListener2FailingUnregister() {
        PropertySource<InputType> property1 = BoolPropertiesTest.mockProperty();
        PropertySource<InputType> property2 = BoolPropertiesTest.mockProperty();

        RuntimeException unregisterError = new RuntimeException();
        FailingListenerRef failingListenerRef = new FailingListenerRef(unregisterError);

        stub(property1.addChangeListener(any(Runnable.class))).toReturn(failingListenerRef);

        Throwable error = new RuntimeException();
        stub(property2.addChangeListener(any(Runnable.class))).toThrow(error);

        PropertySource<?> tested = factory.create(property1, property2);
        try {
            tested.addChangeListener(mock(Runnable.class));
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

            final Runnable unregisterTask = Tasks.runOnceTask(regCount::decrementAndGet, false);

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
