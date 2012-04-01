package org.jtrim.concurrent.async;

import java.util.Collection;

/**
 * Contains static helper methods to create consistent string format for
 * {@link AsyncDataLink} and {@link AsyncDataQuery} implementations.
 * Implementations should consider using methods in this class to create a
 * string representation which looks consistent with the ones in JTrim. The
 * string formats are not intended to be parsable and therefore the exact result
 * of the methods of this class may change in future implementations.
 * <P>
 * This class cannot be inherited or instantiated.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be used by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are synchronization transparent unless otherwise noted.
 *
 * @see AsyncDataLink
 * @see AsyncDataQuery
 *
 * @author Kelemen Attila
 */
public final class AsyncFormatHelper {
    private static final String INDENTATION = "  ";

    // This constant is consitent with StringBuilder which also
    // appends "null" when a null object is passed.
    private static final String NULL_STR = "null";

    private AsyncFormatHelper() {
        throw new AssertionError();
    }

    /**
     * Adds an indentation before every line (optionally the first line can be
     * an exception) of the given string. The line separator is the {@code '\n'}
     * character (UNICODE: 000A).
     * <P>
     * The indentation is currently two space characters.
     *
     * @param text the possibly multi line text which is to be indented. This
     *   argument can be {@code null}, in which case the string {@code "null"}
     *   is used for this argument.
     * @param indentFirstLine {@code true} if the first line of the specified
     *   text must be indented, {@code false} if the first line must not be
     *   indented
     * @return the specified text after the applied indentation. This method
     *   never returns {@code null} (not even if the specified text was
     *   {@code null}).
     *
     * @see #toIndentedString(Object, boolean)
     */
    public static String indentText(String text, boolean indentFirstLine) {
        String result = text != null
                ? text.replace("\n", "\n" + INDENTATION)
                : NULL_STR;

        return indentFirstLine ? (INDENTATION + result) : result;
    }

    /**
     * Converts the specified object to string (using its
     * {@link #toString() toString} method) and returns this string after
     * applying an indentation. The indentation is done the same way as done by
     * the {@link #indentText(String, boolean)} method.
     *
     * @param obj the object to be converted to string and to be indented. This
     *   argument can be {@code null}, in which case its string representation
     *   is assumed to be {@code null} (and therefore the string {@code "null"}
     *   will be used).
     * @param indentFirstLine {@code true} if the first line of the specified
     *   text must be indented, {@code false} if the first line must not be
     *   indented
     * @return the string representation of the specified object after the
     *   applied indentation. This method never returns {@code null} (not even
     *   if the specified object was {@code null} or its string representation
     *   was {@code null}).
     *
     * @see #indentText(String, boolean)
     */
    public static String toIndentedString(Object obj, boolean indentFirstLine) {
        return indentText(obj != null ? obj.toString() : null, indentFirstLine);
    }

    /**
     * Checks whether the specified text contains only a single line. That is,
     * returns {@code true} if the specified text is {@code null} or does not
     * contain the {@code '\n'} character (UNICODE: 000A).
     *
     * @param text the text to be checked if it contains multiple lines or not.
     *   This argument can be {@code null} and in this case it is assumed to be
     *   a single line text.
     * @return {@code true} if the line does not contain multiple lines,
     *   {@code false} otherwise
     */
    public static boolean isSingleLine(String text) {
        return text != null ? text.indexOf('\n') < 0 : true;
    }

    /**
     * Appends the given string after indenting it to the specified
     * {@code StringBuilder}.
     * <P>
     * If the passed text contains only a {@link #isSingleLine(String) single line},
     * it will be appended as is without indentation. Otherwise a newline
     * character ({@code '\n'}) is appended and the text indented (including its
     * first line).
     *
     * @param text the text to be appended to the specified
     *   {@code StringBuilder} after indenting it. This argument can be
     *   {@code null} in which case the string "null" is appended (without
     *   indentation).
     * @param result the {@code StringBuilder} to which the string is to be
     *   appended. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code StringBuilder} is {@code null}
     *
     * @see #appendIndented(Object, StringBuilder)
     */
    public static void appendIndented(String text, StringBuilder result) {
        if (isSingleLine(text)) {
            result.append(text);
        }
        else {
            result.append('\n');
            result.append(indentText(text, true));
        }
    }

    /**
     * Appends the object converted to a string (by calling its
     * {@link Object#toString()} method) after indenting it to the specified
     * {@code StringBuilder}. If the object is {@code null}, its string
     * representation is assumed to be {@code null}. This method is apart from
     * the need to call the {@code toString} method is identical to
     * <P>
     * If the string representation contains only a
     * {@link #isSingleLine(String) single line}, it will be appended as is
     * without indentation. Otherwise a newline character ({@code '\n'}) is
     * appended and the text indented (including its first line).
     *
     * @param obj the object to be converted to string and then appended to the
     *   specified {@code StringBuilder} after indenting it. This argument can
     *   be {@code null} in which case the string "null" is appended (without
     *   indentation).
     * @param result the {@code StringBuilder} to which the string
     *   representation of the specified object is to be appended. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code StringBuilder} is {@code null}
     */
    public static void appendIndented(Object obj, StringBuilder result) {
        appendIndented(obj != null ? obj.toString() : null, result);
    }

    /**
     * Converts the specified array to a string. This methods works similar to
     * the {@code java.util.Arrays#toString(Object[])} but for arrays having
     * length of at least 2 returns a multi line string representation where
     * every element has its own dedicated line. This is intended to be more
     * readable for larger arrays.
     *
     * @param array the array to be converted to string. This argument can be
     *   {@code null}, in which case the string "null" is returned.
     * @return the string representation of the passed array. This method never
     *   returns {@code null}.
     *
     * @see #collectionToString(Collection)
     */
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

    /**
     * Converts the specified {@code Collection} to a string. This method works
     * exactly the same as the {@link #arrayToString(Object[])} method but
     * for collections rather than array. The order of the elements depends
     * on the order the iterator of the collection returns them.
     *
     * @param elements the collection to be converted to a string. This argument
     *   can be {@code null}, in which case the string "null" is returned.
     * @return the string representation of the passed collection. This method
     *   never returns {@code null}.
     *
     * @see #arrayToString(Object[])
     */
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
