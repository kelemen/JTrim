package org.jtrim2.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Contains static helper methods for throwing specific exceptions. These
 * includes helper method for checking arguments of methods and throw an
 * exception if they are inappropriate.
 *
 * <h2>Thread safety</h2>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>.
 */
public final class ExceptionHelper {
    /**
     * Throws the specified exception if it is an instance of {@link Error} or
     * {@link RuntimeException}, or throws a {@code RuntimeException} exception
     * with the specified exception as its cause; does nothing if the specified
     * exception is {@code null}.
     * <P>
     * This method is effectively equivalent to:
     * <pre>
     * if (ex != null) {
     *     ExceptionHelper.rethrow(ex)
     * }
     * </pre>
     *
     * @param ex the exception to be thrown by this method. If it cannot be
     *   thrown due to being a checked exception, it will be wrapped in a cause
     *   of a {@link RuntimeException} and the {@code RuntimeException} is
     *   thrown instead. This argument can be {@code null}, in which case this
     *   method is a no-op.
     *
     * @throws NullPointerException thrown if the specified exception is
     *   {@code null}
     */
    public static void rethrowIfNotNull(Throwable ex) {
        if (ex != null) {
            throw throwUnchecked(ex);
        }
    }

    /**
     * Throws the specified exception if it is an instance of {@link Error} or
     * {@link RuntimeException}, or is an instance of the given type, or
     * throws a {@code RuntimeException} exception with the specified exception
     * as its cause; does nothing if the specified exception is {@code null}.
     *
     * @param <T> the type of the checked exception type which might be rethrown
     *   as is
     * @param ex the exception to be thrown by this method. If it cannot be
     *   thrown due to being a checked exception, it will be wrapped in a cause
     *   of a {@link RuntimeException} and the {@code RuntimeException} is
     *   thrown instead. This argument can be {@code null}, in which case this
     *   method is a no-op.
     * @param checkedType a type defining a {@code Throwable}, if the specified
     *   exception implements this class, it is simply rethrown instead of
     *   wrapped. This argument cannot be {@code null}. However, if {@code null}
     *   is passed for this argument, the specified exception is still rethrown
     *   but a {@code NullPointerException} will be attached to it as a suppressed
     *   exception.
     *
     * @throws NullPointerException thrown if the specified exception is
     *   {@code null} and the given exception type is {@code null} as well
     * @throws T thrown if the given exception is an instance of the given class
     */
    public static <T extends Throwable> void rethrowCheckedIfNotNull(
            Throwable ex,
            Class<? extends T> checkedType) throws T {
        if (ex != null) {
            throw throwChecked(ex, checkedType);
        } else {
            Objects.requireNonNull(checkedType, "checkedType");
        }
    }

    /**
     * Throws the specified exception if it is an instance of {@link Error} or
     * {@link RuntimeException}, or throws a {@code RuntimeException} exception
     * with the specified exception as its cause.
     * <P>
     * Note that this method never returns normally and always throws an
     * exception.
     * <P>
     * <B>Note</B>: Consider using {@link #throwUnchecked(Throwable) } for
     * convenience.
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
        throw throwUnchecked(ex);
    }

    /**
     * Throws the specified exception if it is an instance of {@link Error} or
     * {@link RuntimeException}, or throws a {@code RuntimeException} exception
     * with the specified exception as its cause.
     * <P>
     * Note that this method never returns normally and always throws an
     * exception. The return value is simply for convenience, so that you
     * can write:
     * <P>
     * {@code throw ExceptionHelper.throwUnchecked(myException);}
     * <P>
     * This allows the Java compiler to detect that the code after this method
     * is not reachable.
     *
     * @param ex the exception to be thrown by this method. If it cannot be
     *   thrown due to being a checked exception, it will be wrapped in a cause
     *   of a {@link RuntimeException} and the {@code RuntimeException} is
     *   thrown instead. This argument cannot be {@code null}.
     * @return this method never returns, the return value is provided solely
     *   for convenience
     *
     * @throws NullPointerException thrown if the specified exception is
     *   {@code null}
     */
    public static RuntimeException throwUnchecked(Throwable ex) {
        throw throwChecked(ex, RuntimeException.class);
    }

    /**
     * Throws the specified exception if it is an instance of {@link Error} or
     * {@link RuntimeException} or is of the given type, or throws a
     * {@code RuntimeException} exception with the specified exception as its
     * cause.
     * <P>
     * Note that this method never returns normally and always throws an
     * exception. The return value is simply for convenience, so that you
     * can write:
     * <P>
     * {@code throw ExceptionHelper.throwChecked(myException, MyException.class);}
     * <P>
     * This allows the Java compiler to detect that the code after this method
     * is not reachable.
     *
     * @param <T> the type of the checked exception type which might be rethrown
     *   as is
     * @param ex the exception to be thrown by this method. If it cannot be
     *   thrown due to being a checked exception of the wrong type, it will be
     *   wrapped in a cause of a {@link RuntimeException} and the
     *   {@code RuntimeException} is thrown instead. This argument cannot be
     * {@code null}.
     * @param checkedType a type defining a {@code Throwable}, if the specified
     *   exception implements this class, it is simply rethrown instead of
     *   wrapped. This argument cannot be {@code null}. However, if {@code null}
     *   is passed for this argument, the specified exception is still rethrown
     *   but a {@code NullPointerException} will be attached to it as a suppressed
     *   exception.
     * @return this method never returns, the return value is provided solely
     *   for convenience
     *
     * @throws NullPointerException thrown if the specified exception is
     *   {@code null}
     * @throws T thrown if the given exception is an instance of the given class
     */
    public static <T extends Throwable> RuntimeException throwChecked(
            Throwable ex,
            Class<? extends T> checkedType) throws T {

        Objects.requireNonNull(ex, "ex");

        if (checkedType == null) {
            ex.addSuppressed(new NullPointerException("Checked exception type may not be null"));
        } else if (checkedType.isInstance(ex)) {
            throw checkedType.cast(ex);
        }

        if (ex instanceof Error) {
            throw (Error) ex;
        } else if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        } else {
            throw new RuntimeException(ex);
        }
    }

    private static String longToString(long value) {
        if (value == Integer.MIN_VALUE) {
            return "Integer.MIN_VALUE";
        } else if (value == Integer.MAX_VALUE) {
            return "Integer.MAX_VALUE";
        } else if (value == Long.MIN_VALUE) {
            return "Long.MIN_VALUE";
        } else if (value == Long.MAX_VALUE) {
            return "Long.MAX_VALUE";
        } else {
            return Long.toString(value);
        }
    }

    private static String getIntervalString(long start, long end) {
        return "[" + longToString(start) + ", " + longToString(end) + "]";
    }

    private static String getIntervalStringOpenEnd(long start, long end) {
        return "[" + longToString(start) + ", " + longToString(end) + ")";
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
                + getIntervalStringOpenEnd(intervalStart, intervalEnd);
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
     * {@code intervalStart < minIndex || intervalEnd > maxIndex + 1}
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
        if (intervalStart < minIndex || intervalEnd > maxIndex + 1) {
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
     * {@code intervalStart < minIndex || intervalEnd > maxIndex + 1}
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
        if (intervalStart < minIndex || intervalEnd > maxIndex + 1) {
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
     * @return the {@code value} argument
     *
     * @throws IllegalArgumentException thrown if the specified value is not
     *   within its allowed range
     */
    public static int checkArgumentInRange(
            int value, int minIndex, int maxIndex, String argName) {
        if (value < minIndex || value > maxIndex) {
            throw new IllegalArgumentException(getArgumentNotInRangeMessage(
                    value, minIndex, maxIndex, argName));
        }
        return value;
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
     * @return the {@code value} argument
     *
     * @throws IllegalArgumentException thrown if the specified value is not
     *   within its allowed range
     */
    public static long checkArgumentInRange(
            long value, long minIndex, long maxIndex, String argName) {
        if (value < minIndex || value > maxIndex) {
            throw new IllegalArgumentException(getArgumentNotInRangeMessage(
                    value, minIndex, maxIndex, argName));
        }
        return value;
    }

    /**
     * Checks if the elements of the specified array or the array itself is
     * not null and throws a {@link NullPointerException} if any of them is
     * found to be {@code null}.
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param <T> the type of the elements of the given array
     * @param elements the array to be checked not to contain {@code null}
     *   elements
     * @param argumentName the name of the array to be checked. This string will
     *   be included in the thrown {@code NullPointerException} if the array
     *   is {@code null} or contains {@code null} elements. This argument is
     *   allowed to be {@code null}.
     * @return the {@code elements} argument
     *
     * @throws NullPointerException thrown if the specified array is
     *   {@code null} or contains {@code null} elements
     */
    public static <T> T[] checkNotNullElements(
            T[] elements, String argumentName) {
        Objects.requireNonNull(elements, "elements");

        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null) {
                String elementName = argumentName + "[" + i + "]";
                String message = getNullArgumentMessage(elementName);
                throw new NullPointerException(message);
            }
        }
        return elements;
    }

    /**
     * Checks if the elements of the specified collection or the collection
     * itself is not null and throws a {@link NullPointerException} if any of
     * them is found to be {@code null}.
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param <C> the type of the collection whose elements are to be checked
     * @param elements the collection to be checked not to contain {@code null}
     *   elements
     * @param argumentName the name of the collection to be checked. This string
     *   will be included in the thrown {@code NullPointerException} if the
     *   collection is {@code null} or contains {@code null} elements. This
     *   argument is allowed to be {@code null}.
     * @return the {@code elements} argument
     *
     * @throws NullPointerException thrown if the specified collection is
     *   {@code null} or contains {@code null} elements
     */
    public static <C extends Collection<?>> C checkNotNullElements(
            C elements, String argumentName) {
        Objects.requireNonNull(elements, "elements");

        if (elements.isEmpty()) {
            return elements;
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

        return elements;
    }

    /**
     * Checks if the elements of the specified collection or the collection
     * itself is not null and throws a {@link NullPointerException} if any of
     * them is found to be {@code null}.
     * <P>
     * This method was designed for simple parameter validation of methods.
     *
     * @param <C> the type of the collection whose elements are to be checked
     * @param elements the collection to be checked not to contain {@code null}
     *   elements
     * @param argumentName the name of the collection to be checked. This string
     *   will be included in the thrown {@code NullPointerException} if the
     *   collection is {@code null} or contains {@code null} elements. This
     *   argument is allowed to be {@code null}.
     * @return the {@code elements} argument
     *
     * @throws NullPointerException thrown if the specified collection is
     *   {@code null} or contains {@code null} elements
     */
    public static <C extends Map<?, ?>> C checkNotNullMapElements(
            C elements, String argumentName) {
        Objects.requireNonNull(elements, "elements");

        if (elements.isEmpty()) {
            return elements;
        }

        elements.forEach((key, value) -> {
            if (key == null) {
                String elementName = argumentName + ".key";
                String message = getNullArgumentMessage(elementName);
                throw new NullPointerException(message);
            }
            if (value == null) {
                String elementName = argumentName + "[" + key + "]";
                String message = getNullArgumentMessage(elementName);
                throw new NullPointerException(message);
            }
        });

        return elements;
    }

    private static String getNullArgumentMessage(String argumentName) {
        return "Argument \"" + argumentName + "\" cannot be null.";
    }

    private ExceptionHelper() {
        throw new AssertionError();
    }
}

