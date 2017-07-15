package org.jtrim2.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.testutils.TestObj;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LazyValuesTest {
    public static class SafeLazyValueTest extends AbstractLazyNullableValueTest {
        public SafeLazyValueTest() {
            super(Arrays.asList(LazyValues::lazyValue));
        }
    }

    public static class LockedLazyValueTest extends AbstractLazyNullableValueTest {
        public LockedLazyValueTest() {
            super(Arrays.asList(LazyValues::lazyValueLocked));
        }
    }

    public static class SafeLazyNonNullValueTest extends AbstractLazyNonNullValueTest {
        public SafeLazyNonNullValueTest() {
            super(Arrays.asList(LazyValues::lazyNonNullValue));
        }
    }

    public static abstract class AbstractLazyNullableValueTest extends AbstractLazyValueTest {
        public AbstractLazyNullableValueTest(
                Collection<? extends Function<Supplier<TestValue>, Supplier<TestValue>>> factories) {
            super(factories);
        }

        @Test
        public void testLazyValueWithNullFactory() throws Exception {
            testAll(lazyFactory -> {
                Supplier<TestValue> src = mockSupplier();

                Supplier<TestValue> lazy = lazyFactory.apply(src);

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
                Supplier<TestValue> lazy = lazyFactory.apply(() -> null);
                lazy.get();

                String strValue = lazy.toString();
                assertTrue(strValue, strValue.contains("null"));
            });
        }
    }

    public static abstract class AbstractLazyNonNullValueTest extends AbstractLazyValueTest {
        public AbstractLazyNonNullValueTest(
                Collection<? extends Function<Supplier<TestValue>, Supplier<TestValue>>> factories) {
            super(factories);
        }

        @Test
        public void testLazyValueWithNullFactory() throws Exception {
            testAll(lazyFactory -> {
                Object secondResult = new TestObj("OBJ-2");
                Supplier<TestValue> src = mockSupplier();
                doReturn(null)
                        .doReturn(secondResult)
                        .when(src)
                        .get();

                Supplier<TestValue> lazy = lazyFactory.apply(src);

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
                Supplier<TestValue> lazy = lazyFactory.apply(() -> null);
                lazy.get();

                String strValue = lazy.toString();
                assertTrue(strValue, strValue.contains("?"));
            });
        }
    }
}
