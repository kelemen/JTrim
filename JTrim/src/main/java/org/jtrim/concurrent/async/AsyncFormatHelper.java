/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.*;

/**
 *
 * @author Kelemen Attila
 */
public final class AsyncFormatHelper {
    private static final String INDENTATION = "  ";
    private static final String NULL_STR = "null";
    private AsyncFormatHelper() {
        throw new AssertionError();
    }

    public static String indentText(String text, boolean indentFirstLine) {
        String result = text != null
                ? text.replaceAll("\n", "\n" + INDENTATION)
                : NULL_STR;

        return indentFirstLine ? (INDENTATION + result) : result;
    }

    public static String toIndentedString(Object obj, boolean indentFirstLine) {
        return indentText(obj != null ? obj.toString() : null, indentFirstLine);
    }

    public static boolean isSingleLine(String text) {
        return text != null ? text.indexOf('\n') < 0 : true;
    }

    public static void appendIndented(String text, StringBuilder result) {
        if (isSingleLine(text)) {
            result.append(text);
        }
        else {
            result.append('\n');
            result.append(indentText(text, true));
        }
    }

    public static void appendIndented(Object obj, StringBuilder result) {
        appendIndented(obj != null ? obj.toString() : null, result);
    }

    public static String arrayToString(Object[] array) {
        if (array == null) {
            return NULL_STR;
        }

        if (array.length == 0) {
            return "[]";
        }
        else if (array.length == 1) {
            return "[" + array[0] + "]";
        }
        else {
            StringBuilder result = new StringBuilder(32 * array.length);

            result.append('[');
            for (int i = 0; i < array.length; i++) {
                result.append('\n');
                result.append(array[i]);
            }
            result.append(']');

            return result.toString();
        }
    }

    public static String collectionToString(Collection<?> elements) {
        if (elements == null) {
            return NULL_STR;
        }

        int size = elements.size();
        if (size == 0) {
            return "[]";
        }
        else if (size == 1) {
            return "[" + elements.toArray()[0] + "]";
        }
        else {
            StringBuilder result = new StringBuilder(32 * elements.size());

            result.append('[');
            for (Object element: elements) {
                result.append('\n');
                result.append(element);
            }
            result.append(']');

            return result.toString();
        }
    }
}
