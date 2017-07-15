package org.jtrim2.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.testutils.TestObj;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LazyValuesTest {
    @Test
    public void testLazyWithPostCreate() {
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors() * 2, 4);

        for (int i = 0; i < 100; i++) {
            Collection<AcceptableObject> createdObjects = new ConcurrentLinkedQueue<>();

            AtomicInteger objCounter = new AtomicInteger();
            Supplier<AcceptableObject> src = () -> {
                AcceptableObject result = new AcceptableObject(objCounter.getAndIncrement());
                createdObjects.add(result);
                return result;
            };
            Supplier<AcceptableObject> lazy = LazyValues.lazyNonNullValue(src, AcceptableObject::accept);

            Set<AcceptableObject> results = Collections.synchronizedSet(
                    Collections.newSetFromMap(new IdentityHashMap<>()));

            Runnable[] testTasks = new Runnable[threadCount];
            Arrays.fill(testTasks, (Runnable) () -> {
                results.add(lazy.get());
            });
            Tasks.runConcurrently(testTasks);

            if (results.size() != 1) {
                throw new AssertionError("Expected the same value for all calls but received: " + results);
            }

            int numberOfAccepted = 0;
            for (AcceptableObject obj : createdObjects) {
                boolean accepted = obj.getAcceptedOnce();
                if (accepted) {
                    numberOfAccepted++;
                }
            }

            assertEquals("numberOfAccepted", 1, numberOfAccepted);
        }
    }

    @Test
    public void testLazyNonNullValueEventualInit() {
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors() * 2, 4);

        for (int i = 0; i < 100; i++) {
            Collection<AcceptableObject> createdObjects = new ConcurrentLinkedQueue<>();

            AtomicInteger objCounter = new AtomicInteger();
            Supplier<AcceptableObject> src = () -> {
                AcceptableObject result = new AcceptableObject(objCounter.getAndIncrement());
                createdObjects.add(result);
                return result;
            };
            Supplier<AcceptableObject> lazy = LazyValues.lazyNonNullValueEventualInit(src, obj -> obj.accept(true));

            Set<AcceptableObject> results = Collections.synchronizedSet(
                    Collections.newSetFromMap(new IdentityHashMap<>()));

            Runnable[] testTasks = new Runnable[threadCount];
            Arrays.fill(testTasks, (Runnable) () -> {
                results.add(lazy.get());
            });
            Tasks.runConcurrently(testTasks);

            if (results.size() != 1) {
                throw new AssertionError("Expected the same value for all calls but received: " + results);
            }

            int numberOfAccepted = 0;
            for (AcceptableObject obj : createdObjects) {
                boolean accepted = obj.getAcceptedOnlySuccessful();
                if (accepted) {
                    numberOfAccepted++;
                }
            }

            assertEquals("numberOfAccepted", 1, numberOfAccepted);
        }
    }

    public static class SafeLazyValueTest extends AbstractLazyNullableValueTest {
        public SafeLazyValueTest() {
            super(Arrays.asList(LazyValues::lazyValue));
        }
    }

    public static class LockedLazyValueTest extends AbstractLazyNullableValueTest {
        public LockedLazyValueTest() {
            super(Arrays.asList(LazyValues::lazyValueLocked));
        }

        @Test
        public void testLazyValueConcurrentLocked() throws Exception {
            int threadCount = Math.min(Runtime.getRuntime().availableProcessors() * 2, 4);
            testAll(lazyFactory -> {
                for (int i = 0; i < 100; i++) {
                    Supplier<TestObj> src = mockFactory("Test-Value1");
                    Supplier<TestObj> lazy = lazyFactory.apply(src);

                    Set<TestObj> results = Collections.synchronizedSet(
                            Collections.newSetFromMap(new IdentityHashMap<>()));

                    Runnable[] testTasks = new Runnable[threadCount];
                    Arrays.fill(testTasks, (Runnable) () -> {
                        TestObj value = verifyResult("Test-Value1", lazy);
                        results.add(value);
                    });
                    Tasks.runConcurrently(testTasks);

                    if (results.size() != 1) {
                        throw new AssertionError("Expected the same value for all calls but received: " + results);
                    }

                    verify(src, only()).get();
                    verifyResult("Test-Value1", lazy);
                    verifyNoMoreInteractions(src);
                }
            });
        }
    }

    public static class SafeLazyNonNullValueTest extends AbstractLazyNonNullValueTest {
        public SafeLazyNonNullValueTest() {
            super(Arrays.asList(
                    LazyValues::lazyNonNullValue,
                    (factory) -> LazyValues.lazyNonNullValue(factory, (obj, accepted) -> { }),
                    (factory) -> LazyValues.lazyNonNullValueEventualInit(factory, (obj) -> { })
            ));
        }
    }

    public abstract static class AbstractLazyNullableValueTest extends AbstractLazyValueTest {
        public AbstractLazyNullableValueTest(
                Collection<? extends Function<Supplier<TestObj>, Supplier<TestObj>>> factories) {
            super(factories);
        }

        @Test
        public void testLazyValueWithNullFactory() throws Exception {
            testAll(lazyFactory -> {
                Supplier<TestObj> src = mockSupplier();

                Supplier<TestObj> lazy = lazyFactory.apply(src);

                verifyZeroInteractions(src);
                assertNull("Call1", lazy.get());
                verify(src).get();

                assertNull("Call2", lazy.get());
                verify(src).get();
            });
        }

        @Test
        public void testLazyValueToStringInitializedToNull() throws Exception {
            testAll(lazyFactory -> {
                Supplier<TestObj> lazy = lazyFactory.apply(() -> null);
                lazy.get();

                String strValue = lazy.toString();
                assertTrue(strValue, strValue.contains("null"));
            });
        }
    }

    public abstract static class AbstractLazyNonNullValueTest extends AbstractLazyValueTest {
        public AbstractLazyNonNullValueTest(
                Collection<? extends Function<Supplier<TestObj>, Supplier<TestObj>>> factories) {
            super(factories);
        }

        @Test
        public void testLazyValueWithNullFactory() throws Exception {
            testAll(lazyFactory -> {
                Object secondResult = new TestObj("OBJ-2");
                Supplier<TestObj> src = mockSupplier();
                doReturn(null)
                        .doReturn(secondResult)
                        .when(src)
                        .get();

                Supplier<TestObj> lazy = lazyFactory.apply(src);

                verifyZeroInteractions(src);
                assertNull("Call1", lazy.get());
                verify(src).get();

                assertSame("Call2", secondResult, lazy.get());
                verify(src, times(2)).get();

                assertSame("Call3", secondResult, lazy.get());
                verify(src, times(2)).get();
            });
        }

        @Test
        public void testLazyValueToStringInitializedToNull() throws Exception {
            testAll(lazyFactory -> {
                Supplier<TestObj> lazy = lazyFactory.apply(() -> null);
                lazy.get();

                String strValue = lazy.toString();
                assertTrue(strValue, strValue.contains("?"));
            });
        }
    }

    public static final class AcceptableObject extends TestObj {
        private volatile boolean accepted;
        private final AtomicInteger acceptCount;

        public AcceptableObject(Object value) {
            super(value);

            this.accepted = false;
            this.acceptCount = new AtomicInteger(0);
        }

        public void accept(boolean accepted) {
            acceptCount.incrementAndGet();
            this.accepted = accepted;
        }

        public boolean getAcceptedOnce() {
            int callCount = acceptCount.get();
            if (callCount != 1) {
                throw new AssertionError("Expected exactly one accept call but received: " + callCount);
            }
            return accepted;
        }

        public boolean getAcceptedOnlySuccessful() {
            boolean result = accepted;
            int callCount = acceptCount.get();
            switch (callCount) {
                case 0:
                    break;
                case 1:
                    assertTrue("Only accepted calls are allowed.", result);
                    break;
                default:
                    throw new AssertionError("Unexpected call count: " + callCount);
            }
            return result;
        }
    }
}
