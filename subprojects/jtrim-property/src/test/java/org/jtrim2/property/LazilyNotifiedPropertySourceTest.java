package org.jtrim2.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.collections.Equality;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LazilyNotifiedPropertySourceTest {
    private static <ValueType> PropertySource<ValueType> create(
            PropertySource<? extends ValueType> wrapped,
            EqualityComparator<? super ValueType> equality) {
        return new LazilyNotifiedPropertySource<>(wrapped, equality);
    }

    private static <ValueType> PropertySource<ValueType> create(
            PropertySource<? extends ValueType> wrapped) {
        return create(wrapped, Equality.naturalEquality());
    }

    /**
     * Test of getValue method, of class LazilyNotifiedPropertySource.
     */
    @Test
    public void testGetValue() {
        Object value = new Object();
        PropertySource<Object> property = create(PropertyFactory.constSource(value));
        assertSame(value, property.getValue());
    }

    private static LazilyNotifiedPropertyCreator getFactory() {
        return LazilyNotifiedPropertySourceTest::create;
    }

    public static void testLazyNotifications(LazilyNotifiedPropertyCreator factory) {
        TestObjWithIdentity objA = new TestObjWithIdentity("A");
        TestObjWithIdentity objB = new TestObjWithIdentity("B");

        MutableProperty<TestObjWithIdentity> wrapped = PropertyFactory.memProperty(objA);
        PropertySource<TestObjWithIdentity> property = factory.newProperty(wrapped, TestObjWithIdentity.STR_CMP);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        wrapped.setValue(objB);
        verify(listener).run();

        wrapped.setValue(objB);
        wrapped.setValue(new TestObjWithIdentity("B"));
        verifyNoMoreInteractions(listener);

        wrapped.setValue(objA);
        verify(listener, times(2)).run();

        listenerRef.unregister();

        wrapped.setValue(objB);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testLazyNotifications() {
        testLazyNotifications(getFactory());
    }

    private static void runConcurrentChangeDuringAddChangeListenerOnce(LazilyNotifiedPropertyCreator factory) {
        ConcurrentChangeDuringAddChangeListenerTest[] testTasks
                = new ConcurrentChangeDuringAddChangeListenerTest[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < testTasks.length; i++) {
            testTasks[i] = new ConcurrentChangeDuringAddChangeListenerTest(factory);
        }

        List<Runnable> tasksToRun = new ArrayList<>();
        for (ConcurrentChangeDuringAddChangeListenerTest testTask: testTasks) {
            tasksToRun.addAll(testTask.getTasksToRun());
        }

        Tasks.runConcurrently(tasksToRun);

        for (ConcurrentChangeDuringAddChangeListenerTest testTask: testTasks) {
            testTask.verifyAfterRun();
        }
    }

    public static void testConcurrentChangeDuringAddChangeListener(LazilyNotifiedPropertyCreator factory) {
        for (int i = 0; i < 100; i++) {
            runConcurrentChangeDuringAddChangeListenerOnce(factory);
        }
    }

    // This test could uncover a bug which would result due to disregarding the
    // note in LazilyNotifiedPropertySource.
    @Test
    public void testConcurrentChangeDuringAddChangeListener() {
        testConcurrentChangeDuringAddChangeListener(getFactory());
    }

    public static interface LazilyNotifiedPropertyCreator {
        public <ValueType> PropertySource<ValueType> newProperty(
                MutableProperty<ValueType> wrapped,
                EqualityComparator<? super ValueType> equality);
    }

    private static final class ConcurrentChangeDuringAddChangeListenerTest {
        private static final TestObjWithEquals OBJ_A = new TestObjWithEquals("A");
        private static final TestObjWithEquals OBJ_B = new TestObjWithEquals("B");

        private final MutableProperty<TestObjWithEquals> wrapped;
        private final PropertySource<TestObjWithEquals> property;
        private final CallCounterRunnable listener;

        public ConcurrentChangeDuringAddChangeListenerTest(
                LazilyNotifiedPropertyCreator factory) {
            this.wrapped = PropertyFactory.memProperty(OBJ_A);
            this.property = factory.newProperty(wrapped, Equality.naturalEquality());
            this.listener = new CallCounterRunnable();
        }

        public List<Runnable> getTasksToRun() {
            Runnable registerTask = () -> {
                property.addChangeListener(listener);
            };
            Runnable modifyTask = () -> {
                wrapped.setValue(OBJ_B);
            };

            return Arrays.asList(registerTask, modifyTask);
        }

        public void verifyAfterRun() {
            int initialCallCount = listener.getCallCount();
            assertTrue(initialCallCount <= 1);
            assertEquals(OBJ_B, property.getValue());

            wrapped.setValue(OBJ_A);
            assertEquals("Listener must be notified of the changes",
                    initialCallCount + 1,
                    listener.getCallCount());
        }
    }

    private static final class CallCounterRunnable implements Runnable {
        private final AtomicInteger callCount;

        public CallCounterRunnable() {
            callCount = new AtomicInteger(0);
        }

        @Override
        public void run() {
            callCount.incrementAndGet();
        }

        public int getCallCount() {
            return callCount.get();
        }
    }
}
