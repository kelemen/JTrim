package org.jtrim2.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.TestObj;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class AbstractLazyValueTest extends JTrimTests<Function<Supplier<TestObj>, Supplier<TestObj>>> {
    public AbstractLazyValueTest(Collection<? extends Function<Supplier<TestObj>, Supplier<TestObj>>> factories) {
        super(factories);
    }

    @SuppressWarnings("unchecked")
    protected static <T> Supplier<T> mockSupplier() {
        return (Supplier<T>) mock(Supplier.class);
    }

    protected static Supplier<TestObj> mockFactory(String str) {
        @SuppressWarnings("unchecked")
        Supplier<TestObj> result = mockSupplier();
        doAnswer((InvocationOnMock invocation) -> {
            return new TestObj(str);
        }).when(result).get();
        return result;
    }

    protected static TestObj verifyResult(String expected, Supplier<TestObj> factory) {
        TestObj result = factory.get();
        assertEquals(new TestObj(expected), result);
        return result;
    }

    @Test
    public void testLazyValueSerializedCall() throws Exception {
        testAll(lazyFactory -> {
            Supplier<TestObj> src = mockFactory("Test-Value1");

            Supplier<TestObj> lazy = lazyFactory.apply(src);

            verifyNoInteractions(src);
            TestObj value1 = verifyResult("Test-Value1", lazy);
            verify(src).get();

            TestObj value2 = verifyResult("Test-Value1", lazy);
            verifyNoMoreInteractions(src);

            assertSame(value1, value2);
        });
    }

    @Test
    public void testLazyValueConcurrent() throws Exception {
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

                verify(src, atLeastOnce()).get();
                verifyResult("Test-Value1", lazy);
                verifyNoMoreInteractions(src);
            }
        });
    }

    @Test
    public void testLazyValueToStringNotInitialized() throws Exception {
        testAll(lazyFactory -> {
            TestObj expected = new TestObj("Test-Value35");
            String expectedStr = expected.toString();
            Supplier<TestObj> lazy = lazyFactory.apply(() -> expected);

            String strValue = lazy.toString();
            assertFalse(strValue, strValue.contains(expectedStr));
            assertTrue(strValue, strValue.contains("?"));
        });
    }

    @Test
    public void testLazyValueToStringInitialized() throws Exception {
        testAll(lazyFactory -> {
            TestObj expected = new TestObj("Test-Value35");
            String expectedStr = expected.toString();
            Supplier<TestObj> lazy = lazyFactory.apply(() -> expected);
            lazy.get();

            String strValue = lazy.toString();
            assertTrue(strValue, strValue.contains(expectedStr));
        });
    }
}
