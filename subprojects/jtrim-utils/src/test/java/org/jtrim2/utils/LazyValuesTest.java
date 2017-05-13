package org.jtrim2.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.jtrim2.concurrent.Tasks;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LazyValuesTest {
    private static Supplier<TestValue> mockFactory(String str) {
        @SuppressWarnings("unchecked")
        Supplier<TestValue> result = mock(Supplier.class);
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
    public void testLazyValueSerializedCall() {
        Supplier<TestValue> src = mockFactory("Test-Value1");

        Supplier<TestValue> lazy = LazyValues.lazyValue(src);

        verifyZeroInteractions(src);
        TestValue value1 = verifyResult("Test-Value1", lazy);
        verify(src).get();

        TestValue value2 = verifyResult("Test-Value1", lazy);
        verifyNoMoreInteractions(src);

        assertSame(value1, value2);
    }

    @Test
    public void testLazyValueConcurrent() {
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors() * 2, 4);
        for (int i = 0; i < 100; i++) {
            Supplier<TestValue> src = mockFactory("Test-Value1");
            Supplier<TestValue> lazy = LazyValues.lazyValue(src);

            Set<TestValue> results = Collections.synchronizedSet(
                    Collections.newSetFromMap(new IdentityHashMap<TestValue, Boolean>()));

            Runnable[] testTasks = new Runnable[threadCount];
            Arrays.fill(testTasks, (Runnable)() -> {
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
    }

    @Test
    public void testLazyValueToStringNotInitialized() {
        String expectedStr = "Test-Value35";
        Supplier<String> lazy = LazyValues.lazyValue(() -> expectedStr);

        String strValue = lazy.toString();
        assertFalse(strValue, strValue.contains(expectedStr));
    }

    @Test
    public void testLazyValueToStringInitialized() {
        String expectedStr = "Test-Value35";
        Supplier<String> lazy = LazyValues.lazyValue(() -> expectedStr);
        lazy.get();

        String strValue = lazy.toString();
        assertTrue(strValue, strValue.contains(expectedStr));
    }

    private static final class TestValue {
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

            final TestValue other = (TestValue)obj;
            return Objects.equals(this.str, other.str);
        }

        @Override
        public String toString() {
            return "TestValue{" + str + '}';
        }
    }
}
