package org.jtrim.collections;

import java.util.Comparator;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains helper methods for arrays not present in {@link java.util.Arrays}.
 * <P>
 * This class cannot be instantiated or inherited.
 *
 * @author Kelemen Attila
 */
public final class ArraysEx {
    private static final String EMPTY_ARRAY_MESSAGE = "empty array";
    private ArraysEx() {
        throw new AssertionError();
    }

    /**
     * An {@code Object} array with zero length, containing no elements. Since
     * zero length arrays are immutable, this array can be used for better
     * performance when an empty {@code Object} array is needed.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Returns a readonly view of a part of an array. This method is equivalent
     * to {@link java.util.Arrays#asList(Object[]) java.util.Arrays#asList(T...)}
     * except that the returned list cannot be modified.
     * <P>
     * The elements of the returned list will reflect any changes done to the
     * underlying array. The underlying array will not be copied; the returned
     * list will retain a reference to the specified array. The returned
     * list is serializable.
     * <P>
     * Note that the returned list is also completely safe to use in multiple
     * threads in any context if the underlying array does not change.
     *
     * @param <T> the type of the elements of the array
     * @param array the array to be viewed as a list. This argument cannot be
     *   {@code null}.
     * @param offset the starting index of the array. The element at this index
     *   will be the first element of the returned list (unless length is zero).
     *   This argument must be non-negative and not larger than the length of
     *   the specified array. The offset can be the same as the length of the
     *   specified array if and only if the specified length is zero. Note that
     *   the offset can never be larger than the length of the array.
     * @param length the number of elements to be used from the array.
     *   The returned list will have the same size as this length.
     *   This argument cannot be negative and {@code (offset + length)} must not
     *   be larger than the length of the specified array.
     * @return a readonly list of the part of the specified array. This argument
     *   never returns {@code null}.
     *
     * @throws ArrayIndexOutOfBoundsException thrown if the offset and length
     *   specifies indexes outside of the specified array or if offset is larger
     *   than the length of the array.
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static <T> List<T> viewAsList(T[] array, int offset, int length) {
        return new ArrayView<>(array, offset, length);
    }

    /**
     * Returns a readonly view of an array. This method is equivalent to
     * {@link java.util.Arrays#asList(Object[]) java.util.Arrays#asList(T...)}
     * except that the returned list cannot be modified.
     * <P>
     * The elements of the returned list will reflect any changes done to the
     * underlying array. The underlying array will not be copied; the returned
     * list will retain a reference to the specified array. The returned
     * list is serializable.
     * <P>
     * Note that the returned list is also completely safe to use in multiple
     * threads in any context if the underlying array does not change.
     * <P>
     * This method is equivalent to calling
     * {@link ArraysEx#viewAsList(Object[], int, int) viewAsList(array, 0, array.length)}.
     *
     * @param <T> the type of the elements of the array
     * @param array the array to be viewed as a list. This argument cannot be
     *   {@code null}.
     * @return a readonly list of the part of the specified array. This argument
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static <T> List<T> viewAsList(T[] array) {
        return new ArrayView<>(array, 0, array.length);
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static byte findMax(byte[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        byte max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static char findMax(char[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        char max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static short findMax(short[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        short max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static int findMax(int[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static long findMax(long[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        long max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned. If the array contains {@code NaN}s this method will only
     * return {@code NaN} if the array only contains {@code NaN}s.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static float findMax(float[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        float max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned. If the array contains {@code NaN}s this method will only
     * return {@code NaN} if the array only contains {@code NaN}s.
     *
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the largest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static double findMax(double[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        double max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call. The order of the elements of the array is
     * determined by the specified {@link java.util.Comparator Comparator}.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned.
     *
     * @param <T> the type of the elements in the array
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     * @param cmp the comparator to be used to determine which element is the
     *   largest. This argument cannot be {@code null}.
     * @return the largest element of the specified array defined by the
     *   specified comparator
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array or comparator
     *   is {@code null}. This exception can be thrown also if the array
     *   contains {@code null} values and the comparator does not permit
     *   {@code null} elements.
     */
    public static <T> T findMax(T[] array, Comparator<? super T> cmp) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        ExceptionHelper.checkNotNullArgument(cmp, "cmp");

        T max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (cmp.compare(max, array[i]) < 0) max = array[i];
        }

        return max;
    }

    /**
     * Returns the largest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call. The order of the elements of the array is
     * determined by the natural ordering the elements in the array.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned.
     *
     * @param <T> the type of the elements in the array
     * @param array the array in which the largest element is to be found.
     *   This argument cannot be {@code null} or empty.
     * @return the largest element of the specified array defined by the
     *   specified comparator
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array
     *   is {@code null}. This exception can be thrown also if the array
     *   contains {@code null} values.
     */
    public static <T extends Comparable<? super T>> T findMax(T[] array) {
        return findMax(array, CollectionsEx.naturalOrder());
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static byte findMin(byte[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        byte min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static char findMin(char[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        char min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static short findMin(short[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        short min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static int findMin(int[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        int min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static long findMin(long[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        long min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned. If the array contains {@code NaN}s this method will only
     * return {@code NaN} if the array only contains {@code NaN}s.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static float findMin(float[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        float min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned. If the array contains {@code NaN}s this method will only
     * return {@code NaN} if the array only contains {@code NaN}s.
     *
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     *
     * @return the smallest element of the specified array
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array is
     *   {@code null}
     */
    public static double findMin(double[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        double min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call. The order of the elements of the array is
     * determined by the specified {@link java.util.Comparator Comparator}.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned.
     *
     * @param <T> the type of the elements in the array
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     * @param cmp the comparator to be used to determine which element is the
     *   largest. This argument cannot be {@code null}.
     * @return the smallest element of the specified array defined by the
     *   specified comparator
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array or comparator
     *   is {@code null}. This exception can be thrown also if the array
     *   contains {@code null} values and the comparator does not permit
     *   {@code null} elements.
     */
    public static <T> T findMin(T[] array, Comparator<? super T> cmp) {
        if (array.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MESSAGE);
        }

        T min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (cmp.compare(min, array[i]) > 0) min = array[i];
        }

        return min;
    }

    /**
     * Returns the smallest element of the specified array.
     * The result of this method is unspecified if the underlying array changes
     * during this method call. The order of the elements of the array is
     * determined by the natural ordering the elements in the array.
     * <P>
     * In case there can be multiple valid results any of them can be
     * returned.
     *
     * @param <T> the type of the elements in the array
     * @param array the array in which the smallest element is to be found.
     *   This argument cannot be {@code null} or empty.
     * @return the smallest element of the specified array defined by the
     *   specified comparator
     *
     * @throws IllegalArgumentException thrown if the array is empty
     * @throws NullPointerException thrown if the specified array
     *   is {@code null}. This exception can be thrown also if the array
     *   contains {@code null} values.
     */
    public static <T extends Comparable<? super T>> T findMin(T[] array) {
        return findMin(array, CollectionsEx.naturalOrder());
    }
}
