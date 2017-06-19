package org.jtrim2.utils;

import java.util.Arrays;

public class LazyValuesTest {
    public static class SafeLazyValueTest extends AbstractLazyValueTest {
        public SafeLazyValueTest() {
            super(Arrays.asList(LazyValues::lazyValue));
        }
    }

    public static class LockedLazyValueTest extends AbstractLazyValueTest {
        public LockedLazyValueTest() {
            super(Arrays.asList(LazyValues::lazyValueLocked));
        }
    }
}
