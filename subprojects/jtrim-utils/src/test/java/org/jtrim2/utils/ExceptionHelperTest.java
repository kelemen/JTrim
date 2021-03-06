package org.jtrim2.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("ThrowableResultIgnored")
public class ExceptionHelperTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(ExceptionHelper.class);
    }

    @Test
    public void testRethrowIfNotNullException() {
        try {
            ExceptionHelper.rethrowIfNotNull(new TestException());
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestRuntimeException.class)
    public void testRethrowIfNotNullRuntimException() {
        ExceptionHelper.rethrowIfNotNull(new TestRuntimeException());
    }

    @Test(expected = TestError.class)
    public void testRethrowIfNotNullError() {
        ExceptionHelper.rethrowIfNotNull(new TestError());
    }

    @Test
    public void testRethrowIfNotNullNull() {
        ExceptionHelper.rethrowIfNotNull(null);
    }

    @Test
    public void testRethrowCheckedIfNotNullException() throws TestException2 {
        try {
            ExceptionHelper.rethrowCheckedIfNotNull(new TestException(), TestException2.class);
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestException.class)
    public void testRethrowCheckedIfNotNullException2() throws TestException {
        ExceptionHelper.rethrowCheckedIfNotNull(new TestException(), TestException.class);
    }

    @Test(expected = TestRuntimeException.class)
    public void testRethrowCheckedIfNotNullRuntimException() throws TestException {
        ExceptionHelper.rethrowCheckedIfNotNull(new TestRuntimeException(), TestException.class);
    }

    @Test(expected = TestError.class)
    public void testRethrowCheckedIfNotNullError() throws TestException {
        ExceptionHelper.rethrowCheckedIfNotNull(new TestError(), TestException.class);
    }

    @Test
    public void testRethrowCheckedIfNotNullNull() throws TestException {
        ExceptionHelper.rethrowCheckedIfNotNull(null, TestException.class);
    }

    @Test
    public void testRethrowException() {
        try {
            ExceptionHelper.rethrow(new TestException());
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestRuntimeException.class)
    public void testRethrowRuntimException() {
        ExceptionHelper.rethrow(new TestRuntimeException());
    }

    @Test(expected = TestError.class)
    public void testRethrowError() {
        ExceptionHelper.rethrow(new TestError());
    }

    @Test(expected = NullPointerException.class)
    public void testRethrowNull() {
        ExceptionHelper.rethrow(null);
    }

    @Test
    public void testThrowUncheckedException() {
        try {
            ExceptionHelper.throwUnchecked(new TestException());
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestRuntimeException.class)
    public void testThrowUncheckedRuntimException() {
        ExceptionHelper.throwUnchecked(new TestRuntimeException());
    }

    @Test(expected = TestError.class)
    public void testThrowUncheckedError() {
        ExceptionHelper.throwUnchecked(new TestError());
    }

    @Test(expected = NullPointerException.class)
    public void testThrowUncheckedNull() {
        ExceptionHelper.throwUnchecked(null);
    }

    @Test
    public void testThrowCheckedException() throws TestException2 {
        try {
            ExceptionHelper.throwChecked(new TestException(), TestException2.class);
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestException.class)
    public void testThrowCheckedException2() throws TestException {
        ExceptionHelper.throwChecked(new TestException(), TestException.class);
    }

    @Test(expected = TestRuntimeException.class)
    public void testThrowCheckedRuntimException() throws TestException {
        ExceptionHelper.throwChecked(new TestRuntimeException(), TestException.class);
    }

    @Test(expected = TestError.class)
    public void testThrowCheckedError() throws TestException {
        ExceptionHelper.throwChecked(new TestError(), TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowCheckedNull1() throws TestException {
        ExceptionHelper.throwChecked(null, TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowCheckedNull2() throws Throwable {
        ExceptionHelper.throwChecked(null, null);
    }

    @Test
    public void testThrowCheckedNull3() throws Throwable {
        try {
            ExceptionHelper.throwChecked(new RuntimeException(), null);
            fail("Expected TestException.");
        } catch (RuntimeException ex) {
            Throwable[] suppressed = ex.getSuppressed();
            assertTrue("Must have suppressed exception", suppressed.length == 1);
            assertTrue(suppressed[0] instanceof NullPointerException);
        }
    }

    @Test
    public void testCheckIntervalInRangeForIntsFitsEasily() {
        ExceptionHelper.checkIntervalInRange(10, 20, 5, 25, "arg");
    }

    @Test
    public void testCheckIntervalInRangeForIntsBarelyFits() {
        ExceptionHelper.checkIntervalInRange(10, 20, 10, 19, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsBarelyWrong1() {
        ExceptionHelper.checkIntervalInRange(10, 20, 10, 18, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsBarelyWrong2() {
        ExceptionHelper.checkIntervalInRange(10, 20, 11, 19, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsBeforeAllowed() {
        ExceptionHelper.checkIntervalInRange(10, 20, 0, 5, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsAfterAllowed() {
        ExceptionHelper.checkIntervalInRange(10, 20, 20, 25, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsBeginOverlap() {
        ExceptionHelper.checkIntervalInRange(10, 20, 5, 15, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsEndOverlap() {
        ExceptionHelper.checkIntervalInRange(10, 20, 15, 25, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsWithoutUpperBound() {
        ExceptionHelper.checkIntervalInRange(10, 20, 15, Integer.MAX_VALUE, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForIntsWithoutLowerBound() {
        ExceptionHelper.checkIntervalInRange(10, 20, Integer.MIN_VALUE, 15, "arg");
    }

    @Test
    public void testCheckIntervalInRangeForLongsFitsEasily() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 5L, 25L, "arg");
    }

    @Test
    public void testCheckIntervalInRangeForLongsBarelyFits() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 10L, 19L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsBarelyWrong1() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 10L, 18L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsBarelyWrong2() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 11L, 19L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsBeforeAllowed() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 0L, 5L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsAfterAllowed() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 20L, 25L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsBeginOverlap() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 5L, 15L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsEndOverlap() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 15L, 25L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsWithoutUpperBound() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, 15L, Long.MAX_VALUE, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIntervalInRangeForLongsWithoutLowerBound() {
        ExceptionHelper.checkIntervalInRange(10L, 20L, Long.MIN_VALUE, 15L, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForIntsReturnsSame() {
        int actual = ExceptionHelper.checkArgumentInRange(15, 10, 20, "arg");
        assertEquals(15, actual);
    }

    @Test
    public void testCheckArgumentInRangeForIntsEasilyFits() {
        ExceptionHelper.checkArgumentInRange(15, 10, 20, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForIntsBarelyFitsLow() {
        ExceptionHelper.checkArgumentInRange(10, 10, 20, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForIntsBarelyFitsHigh() {
        ExceptionHelper.checkArgumentInRange(20, 10, 20, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForIntsBarelyFitsSingle() {
        ExceptionHelper.checkArgumentInRange(10, 10, 10, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForIntsBarelyWrongLow() {
        ExceptionHelper.checkArgumentInRange(9, 10, 20, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForIntsBarelyWrongHigh() {
        ExceptionHelper.checkArgumentInRange(21, 10, 20, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForIntsWrongWithoutUpperBound() {
        ExceptionHelper.checkArgumentInRange(5, 10, Integer.MAX_VALUE, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForIntsWrongWithoutLowerBound() {
        ExceptionHelper.checkArgumentInRange(25, Integer.MIN_VALUE, 20, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForLongsReturnsSame() {
        long actual = ExceptionHelper.checkArgumentInRange(15L, 10L, 20L, "arg");
        assertEquals(15L, actual);
    }

    @Test
    public void testCheckArgumentInRangeForLongsEasilyFits() {
        ExceptionHelper.checkArgumentInRange(15L, 10L, 20L, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForLongsBarelyFitsLow() {
        ExceptionHelper.checkArgumentInRange(10L, 10L, 20L, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForLongsBarelyFitsHigh() {
        ExceptionHelper.checkArgumentInRange(20L, 10L, 20L, "arg");
    }

    @Test
    public void testCheckArgumentInRangeForLongsBarelyFitsSingle() {
        ExceptionHelper.checkArgumentInRange(10L, 10L, 10L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForLongsBarelyWrongLow() {
        ExceptionHelper.checkArgumentInRange(9L, 10L, 20L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForLongsBarelyWrongHigh() {
        ExceptionHelper.checkArgumentInRange(21L, 10L, 20L, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForLongsWrongWithoutUpperBound() {
        ExceptionHelper.checkArgumentInRange(5L, 10L, Long.MAX_VALUE, "arg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgumentInRangeForLongsWrongWithoutLowerBound() {
        ExceptionHelper.checkArgumentInRange(25L, Long.MIN_VALUE, 20L, "arg");
    }

    @Test
    public void testCheckNotNullElementsArrayEmpty() {
        ExceptionHelper.checkNotNullElements(new Object[]{}, "arg");
    }

    @Test
    public void testCheckNotNullElementsArraySimple() {
        ExceptionHelper.checkNotNullElements(new Object[]{new Object(), new Object()}, "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsArrayNull() {
        ExceptionHelper.checkNotNullElements((Object[]) null, "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsArrayNullFirst() {
        ExceptionHelper.checkNotNullElements(new Object[]{null, new Object(), new Object()}, "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsArrayNullLast() {
        ExceptionHelper.checkNotNullElements(new Object[]{new Object(), new Object(), null}, "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsArrayNullMiddle() {
        ExceptionHelper.checkNotNullElements(new Object[]{new Object(), null, new Object()}, "arg");
    }

    @Test
    public void testCheckNotNullElementsReturnSameArray() {
        Integer[] expected = new Integer[]{1, 2, 3};
        Integer[] actual = ExceptionHelper.checkNotNullElements(expected, "arg");
        assertSame(expected, actual);
    }

    @Test
    public void testCheckNotNullElementsCollectionEmpty() {
        ExceptionHelper.checkNotNullElements(Collections.emptySet(), "arg");
    }

    @Test
    public void testCheckNotNullElementsCollectionSimple() {
        ExceptionHelper.checkNotNullElements(Arrays.asList(new Object(), new Object()), "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsCollectionNull() {
        ExceptionHelper.checkNotNullElements((Collection<?>) null, "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsCollectionNullFirst() {
        ExceptionHelper.checkNotNullElements(Arrays.asList(null, new Object(), new Object()), "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsCollectionNullLast() {
        ExceptionHelper.checkNotNullElements(Arrays.asList(new Object(), new Object(), null), "arg");
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNullElementsCollectionNullMiddle() {
        ExceptionHelper.checkNotNullElements(Arrays.asList(new Object(), null, new Object()), "arg");
    }

    @Test
    public void testCheckNotNullElementsCollectionReturnSame() {
        List<Integer> expected = Arrays.asList(1, 2);
        List<Integer> actual = ExceptionHelper.checkNotNullElements(expected, "arg");
        assertSame(expected, actual);
    }

    @Test
    public void testCheckNotNullElementsCollectionReturnSameForEmpty() {
        List<Integer> expected = new ArrayList<>();
        List<Integer> actual = ExceptionHelper.checkNotNullElements(expected, "arg");
        assertSame(expected, actual);
    }

    @Test
    public void testCheckNotNullMapElementsEmpty() {
        Map<Object, Object> expected = Collections.emptyMap();
        Map<Object, Object> actual = ExceptionHelper.checkNotNullMapElements(expected, "test-arg");
        assertSame(expected, actual);
    }

    @Test
    public void testCheckNotNullMapElementsSimple() {
        Map<Object, Object> expected = new HashMap<>();
        expected.put("Key-1-5465654", "Value-1-4343");
        expected.put("Key-2-5465654", "Value-2-4343");
        Map<Object, Object> actual = ExceptionHelper.checkNotNullMapElements(expected, "test-arg");
        assertSame(expected, actual);
    }

    @Test
    public void testCheckNotNullMapElementsNull() {
        TestUtils.expectError(NullPointerException.class, () -> {
            ExceptionHelper.checkNotNullMapElements(null, "arg");
        });
    }

    @Test
    public void testCheckNotNullMapElementsNullKey() {
        Map<Object, Object> expected = new HashMap<>();
        expected.put("Key-1-5436436", "Value-1-435");
        expected.put(null, "Value-X-435");
        expected.put("Key-2-5436436", "Value-2-435");

        TestUtils.expectError(NullPointerException.class, () -> {
            ExceptionHelper.checkNotNullMapElements(expected, "arg");
        });
    }

    @Test
    public void testCheckNotNullMapElementsNullValue() {
        Map<Object, Object> expected = new HashMap<>();
        expected.put("Key-1-5436436", "Value-1-435");
        expected.put("Key-X-5436436", null);
        expected.put("Key-2-5436436", "Value-2-435");

        TestUtils.expectError(NullPointerException.class, () -> {
            ExceptionHelper.checkNotNullMapElements(expected, "arg");
        });
    }

    @Test
    public void testCheckNotNullMapElementsNullKeyOnly() {
        Map<Object, Object> expected = new HashMap<>();
        expected.put(null, "Value-X-439544");

        TestUtils.expectError(NullPointerException.class, () -> {
            ExceptionHelper.checkNotNullMapElements(expected, "arg");
        });
    }

    @Test
    public void testCheckNotNullMapElementsNullValueOnly() {
        Map<Object, Object> expected = new HashMap<>();
        expected.put("Key-X-gegre", null);

        TestUtils.expectError(NullPointerException.class, () -> {
            ExceptionHelper.checkNotNullMapElements(expected, "arg");
        });
    }

    private static class TestRuntimeException extends RuntimeException {
        private static final long serialVersionUID = -7461262938744477494L;
    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 4344550423252703187L;
    }

    private static class TestException2 extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class TestError extends Error {
        private static final long serialVersionUID = -8295518361535183378L;
    }
}
