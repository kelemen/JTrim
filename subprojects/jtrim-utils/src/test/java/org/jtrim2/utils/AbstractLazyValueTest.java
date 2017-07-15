package org.jtrim2.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.utils.AbstractLazyValueTest.TestValue;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class AbstractLazyValueTest extends JTrimTests<Function<Supplier<TestValue>, Supplier<TestValue>>> {
    public AbstractLazyValueTest(Collection<? extends Function<Supplier<TestValue>, Supplier<TestValue>>> factories) {
        super(factories);
    }

    @SuppressWarnings("unchecked")
    protected static <T> Supplier<T> mockSupplier() {
        return (Supplier<T>) mock(Supplier.class);
    }

    protected static Supplier<TestValue> mockFactory(String str) {
        @SuppressWarnings("unchecked")
        Supplier<TestValue> result = mockSupplier();
        doAnswer((InvocationOnMock invocation) -> {
            return new TestValue(str);
        }).when(result).get();
        return result;
    }

    private static TestValue verifyResult(String expected, Supplier<TestValue> factory) {
        TestValue result = factory.get();
        assertEquals(new TestValue(expected), result);
        return result;
    }

    @Test
    public void testLazyValueSerializedCall() throws Exception {
        testAll(lazyFactory -> {
            Supplier<TestValue> src = mockFactory("Test-Value1");

            Supplier<TestValue> lazy = lazyFactory.apply(src);

            verifyZeroInteractions(src);
            TestValue value1 = verifyResult("Test-Value1", lazy);
            verify(src).get();

            TestValue value2 = verifyResult("Test-Value1", lazy);
            verifyNoMoreInteractions(src);

            assertSame(value1, value2);
        });
    }

    @Test
    public void testLazyValueConcurrent() throws Exception {
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors() * 2, 4);
        testAll(lazyFactory -> {
            for (int i = 0; i < 100; i++) {
                Supplier<TestValue> src = mockFactory("Test-Value1");
                Supplier<TestValue> lazy = lazyFactory.apply(src);

                Set<TestValue> results = Collections.synchronizedSet(
                        Collections.newSetFromMap(new IdentityHashMap<>()));

                Runnable[] testTasks = new Runnable[threadCount];
                Arrays.fill(testTasks, (Runnable) () -> {
                    TestValue value = verifyResult("Test-Value1", lazy);
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
            TestValue expected = new TestValue("Test-Value35");
            String expectedStr = expected.toString();
            Supplier<TestValue> lazy = lazyFactory.apply(() -> expected);

            String strValue = lazy.toString();
            assertFalse(strValue, strValue.contains(expectedStr));
            assertTrue(strValue, strValue.contains("?"));
        });
    }

    @Test
    public void testLazyValueToStringInitialized() throws Exception {
        testAll(lazyFactory -> {
            TestValue expected = new TestValue("Test-Value35");
            String expectedStr = expected.toString();
            Supplier<TestValue> lazy = lazyFactory.apply(() -> expected);
            lazy.get();

            String strValue = lazy.toString();
            assertTrue(strValue, strValue.contains(expectedStr));
        });
    }

    public static final class TestValue {
        private final String str;

        public TestValue(String str) {
            this.str = str;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.str);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TestValue other = (TestValue) obj;
            return Objects.equals(this.str, other.str);
        }

        @Override
        public String toString() {
            return "TestValue{" + str + '}';
        }
    }
}
