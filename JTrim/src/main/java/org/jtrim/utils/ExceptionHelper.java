package org.jtrim.utils;

import java.util.Collection;

/**
 * Contains static helper methods for throwing specific exceptions. These
 * includes helper method for checking arguments of methods and throw an
 * exception if they are inappropriate.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class ExceptionHelper {
    /**
     * Throws the specified exception if it is an instance of {@link Error} or
     * {@link RuntimeException}, or throws a {@code RuntimeException} exception
     * with the specified exception as its cause.
     * <P>
     * Note that this method never returns normally and always throws an
     * exception.
     *
     * @param ex the exception to be thrown by this method. If it cannot be
     *   thrown due to being a checked exception, it will be wrapped in a cause
     *   of a {@link RuntimeException} and the {@code RuntimeException} is
     *   thrown instead. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified exception is
     *   {@code null}
     */
    public static void rethrow(Throwable ex) {
        ExceptionHelper.checkNotNullArgument(ex, "ex");

        if (ex instanceof Error) {
            throw (Error)ex;
        }
        else if (ex instanceof RuntimeException) {
            throw (RuntimeException)ex;
        }
        else {
            throw new RuntimeException(ex);
        }
    }

    private static String longToString(long value) {
        if (value == Integer.MIN_VALUE) {
            return "Integer.MIN_VALUE";
        }
        else if (value == Integer.MAX_VALUE) {
            return "Integer.MAX_VALUE";
        }
        else if (value == Long.MIN_VALUE) {
            return "Long.MIN_VALUE";
        }
        else if (value == Long.MAX_VALUE) {
            return "Long.MAX_VALUE";
        }
        else {
            return Long.toString(value);
        }
    }

    private static String getIntervalString(long start, long end) {
        return "[" + longToString(start) + ", " + longToString(end) + "]";
    }

    private static String getArgumentNotInRangeMessage(
            long value, long minIndex, long maxIndex, String argName) {
        return "Argument \"" + argName + "\" is not within "
                + getIntervalString(minIndex, maxIndex)
                + ". Value = " + longToString(value);
    }

    private static String getIntervalNotInRangeMessage(
            long intervalStart, long intervalEnd, long minIndex, long maxIndex,
            String argName) {
        return "Interval \"" + argName + "\" is not within "
                + getIntervalString(minIndex, maxIndex) + ". Interval = "
                + getIntervalString(intervalStart, intervalEnd);
    }

    /**
     * Checks if all the values of the specified interval is within
     * the allowed range and throws an {@link IllegalArgumentException} if
     * it is not. In case the interval is within the allowed range, this method
     * returns immediately to the caller.
     * <P>
     * More formally, this method will throw an {@code IllegalArgumentException}
     * if and only if:
     * <P>
     * {@code intervalStart < minIndex || intervalEnd > maxIndex}
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param intervalStart the inclusive lower bound of the interval. This
     *   argument can be any possible {@code int} value.
     * @param intervalEnd the exclusive upper bound of the interval. This
     *   argument can be any possible {@code int} value.
     * @param minIndex the inclusive lower bound for the allowed value for the
     *   interval. This argument can be any possible {@code int} value.
     * @param maxIndex the inclusive upper bound for the allowed value for the
     *   interval. This argument can be any possible {@code int} value.
     * @param argName the name of the interval to be checked. This string will
     *   be included in the message of the thrown
     *   {@code IllegalArgumentException} if the interval is found out of range.
     *   This argument is allowed to be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified interval is
     *   outside of the allowed range
     */
    public static void checkIntervalInRange(
            int intervalStart, int intervalEnd, int minIndex, int maxIndex,
            String argName) {
        if (intervalStart < minIndex || intervalEnd > maxIndex) {
            throw new IllegalArgumentException(getIntervalNotInRangeMessage(
                    intervalStart, intervalEnd, minIndex, maxIndex, argName));
        }
    }

    /**
     * Checks if all the values of the specified interval is within
     * the allowed range and throws an {@link IllegalArgumentException} if
     * it is not. In case the interval is within the allowed range, this method
     * returns immediately to the caller.
     * <P>
     * More formally, this method will throw an {@code IllegalArgumentException}
     * if and only if:
     * <P>
     * {@code intervalStart < minIndex || intervalEnd > maxIndex}
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param intervalStart the inclusive lower bound of the interval. This
     *   argument can be any possible {@code long} value.
     * @param intervalEnd the exclusive upper bound of the interval. This
     *   argument can be any possible {@code long} value.
     * @param minIndex the inclusive lower bound for the allowed value for the
     *   interval. This argument can be any possible {@code long} value.
     * @param maxIndex the inclusive upper bound for the allowed value for the
     *   interval. This argument can be any possible {@code long} value.
     * @param argName the name of the interval to be checked. This string will
     *   be included in the message of the thrown
     *   {@code IllegalArgumentException} if the interval is found out of range.
     *   This argument is allowed to be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified interval is
     *   outside of the allowed range
     */
    public static void checkIntervalInRange(
            long intervalStart, long intervalEnd, long minIndex, long maxIndex,
            String argName) {
        if (intervalStart < minIndex || intervalEnd > maxIndex) {
            throw new IllegalArgumentException(getIntervalNotInRangeMessage(
                    intervalStart, intervalEnd, minIndex, maxIndex, argName));
        }
    }

    /**
     * Checks if the specified argument is within its allowed range and throws
     * an {@link IllegalArgumentException} if it is not. In case the argument
     * is within the allowed range, this method returns immediately to the
     * caller.
     * <P>
     * More formally, this method will throw an {@code IllegalArgumentException}
     * if and only if:
     * <P>
     * {@code value < minIndex || value > maxIndex}
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param value the value to be checked if it is within the allowed range.
     *   This argument can be any possible {@code int} value.
     * @param minIndex the inclusive lower bound for the specified value.
     *   This argument can be any possible {@code int} value.
     * @param maxIndex the inclusive upper bound for the specified value.
     *   This argument can be any possible {@code int} value.
     * @param argName the name of the value to be checked. This string will
     *   be included in the message of the thrown
     *   {@code IllegalArgumentException} if the value is found to be out of its
     *   allowed range. This argument can be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified value is not
     *   within its allowed range
     */
    public static void checkArgumentInRange(
            int value, int minIndex, int maxIndex, String argName) {
        if (value < minIndex || value > maxIndex) {
            throw new IllegalArgumentException(getArgumentNotInRangeMessage(
                    value, minIndex, maxIndex, argName));
        }
    }

    /**
     * Checks if the specified argument is within its allowed range and throws
     * an {@link IllegalArgumentException} if it is not. In case the argument
     * is within the allowed range, this method returns immediately to the
     * caller.
     * <P>
     * More formally, this method will throw an {@code IllegalArgumentException}
     * if and only if:
     * <P>
     * {@code value < minIndex || value > maxIndex}
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param value the value to be checked if it is within the allowed range.
     *   This argument can be any possible {@code long} value.
     * @param minIndex the inclusive lower bound for the specified value.
     *   This argument can be any possible {@code long} value.
     * @param maxIndex the inclusive upper bound for the specified value.
     *   This argument can be any possible {@code long} value.
     * @param argName the name of the value to be checked. This string will
     *   be included in the message of the thrown
     *   {@code IllegalArgumentException} if the value is found to be out of its
     *   allowed range. This argument can be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified value is not
     *   within its allowed range
     */
    public static void checkArgumentInRange(
            long value, long minIndex, long maxIndex, String argName) {
        if (value < minIndex || value > maxIndex) {
            throw new IllegalArgumentException(getArgumentNotInRangeMessage(
                    value, minIndex, maxIndex, argName));
        }
    }

    /**
     * Checks if the elements of the specified array or the array itself is
     * not null and throws a {@link NullPointerException} if any of them is
     * found to be {@code null}.
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param elements the array to be checked not to contain {@code null}
     *   elements
     * @param argumentName the name of the array to be checked. This string will
     *   be included in the thrown {@code NullPointerException} if the array
     *   is {@code null} or contains {@code null} elements. This argument is
     *   allowed to be {@code null}.
     *
     * @throws NullPointerException thrown if the specified array is
     *   {@code null} or contains {@code null} elements
     */
    public static void checkNotNullElements(Object[] elements, String argumentName) {
        ExceptionHelper.checkNotNullArgument(elements, "elements");

        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null) {
                String elementName = argumentName + "[" + i + "]";
                String message = getNullArgumentMessage(elementName);
                throw new NullPointerException(message);
            }
        }
    }

    /**
     * Checks if the elements of the specified collection or the collection
     * itself is not null and throws a {@link NullPointerException} if any of
     * them is found to be {@code null}.
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param elements the collection to be checked not to contain {@code null}
     *   elements
     * @param argumentName the name of the collection to be checked. This string
     *   will be included in the thrown {@code NullPointerException} if the
     *   collection is {@code null} or contains {@code null} elements. This
     *   argument is allowed to be {@code null}.
     *
     * @throws NullPointerException thrown if the specified collection is
     *   {@code null} or contains {@code null} elements
     */
    public static void checkNotNullElements(Collection<?> elements, String argumentName) {
        ExceptionHelper.checkNotNullArgument(elements, "elements");

        if (elements.isEmpty()) {
            return;
        }

        int index = 0;
        for (Object element: elements) {
            if (element == null) {
                String elementName = argumentName + "[" + index + "]";
                String message = getNullArgumentMessage(elementName);
                throw new NullPointerException(message);
            }
            index++;
        }
    }

    /**
     * Checks if the specified argument is {@code null} and throws a
     * {@link NullPointerException} if it is {@code null}.
     * <P>
     * This method was designed for simple parameter validation of methods.
     * <P>
     * This method is effectively the same as
     * {@link java.util.Objects#requireNonNull(Object, String)} but throws an
     * exception which has more descriptive message if only the name of the
     * argument is specified.
     *
     * @param argument the object to be checked if it is {@code null} or not
     * @param argumentName the name of the argument to be checked if it is
     *   {@code null} or not. This string will be included in the thrown
     *   {@code NullPointerException} if the argument is found to be {@code null}. This
     *   argument is allowed to be {@code null}.
     */
    public static void checkNotNullArgument(Object argument,
            String argumentName) {

        if (argument == null) {
            throw new NullPointerException(getNullArgumentMessage(argumentName));
        }
    }

    private static String getNullArgumentMessage(String argumentName) {
        return "Argument \"" + argumentName + "\" cannot be null.";
    }

    private ExceptionHelper() {
        throw new AssertionError();
    }
}

