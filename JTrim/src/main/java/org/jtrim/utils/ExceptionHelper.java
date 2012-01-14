/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.utils;

import java.util.*;

/**
 *
 * @author Kelemen Attila
 */
public final class ExceptionHelper {
    private ExceptionHelper() {
        throw new AssertionError();
    }

    public static void rethrow(Throwable ex) {
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

    public static void checkIntervalInRange(
            int intervalStart, int intervalEnd, int minIndex, int maxIndex,
            String argName) {
        if (intervalStart < minIndex || intervalEnd > maxIndex) {
            throw new IllegalArgumentException(getIntervalNotInRangeMessage(
                    intervalStart, intervalEnd, minIndex, maxIndex, argName));
        }
    }

    public static void checkIntervalInRange(
            long intervalStart, long intervalEnd, long minIndex, long maxIndex,
            String argName) {
        if (intervalStart < minIndex || intervalEnd > maxIndex) {
            throw new IllegalArgumentException(getIntervalNotInRangeMessage(
                    intervalStart, intervalEnd, minIndex, maxIndex, argName));
        }
    }

    public static void checkArgumentInRange(
            int value, int minIndex, int maxIndex, String argName) {
        if (value < minIndex || value > maxIndex) {
            throw new IllegalArgumentException(getArgumentNotInRangeMessage(
                    value, minIndex, maxIndex, argName));
        }
    }

    public static void checkArgumentInRange(
            long value, long minIndex, long maxIndex, String argName) {
        if (value < minIndex || value > maxIndex) {
            throw new IllegalArgumentException(getArgumentNotInRangeMessage(
                    value, minIndex, maxIndex, argName));
        }
    }

    public static void checkNotNullElements(Object[] elements, String argumentName) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null) {
                String elementName = argumentName + "[" + i + "]";
                String message = getNullArgumentMessage(elementName);
                throw new NullPointerException(message);
            }
        }
    }

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

    public static void checkNotNullArgument(Object argument,
            String argumentName) {

        if (argument == null) {
            throw new NullPointerException(getNullArgumentMessage(argumentName));
        }
    }

    private static String getNullArgumentMessage(String argumentName) {
        return "Argument \"" + argumentName + "\" cannot be null.";
    }
}

