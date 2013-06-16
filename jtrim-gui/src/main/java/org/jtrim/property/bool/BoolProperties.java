package org.jtrim.property.bool;

import org.jtrim.collections.Comparators;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.property.PropertySource;

/**
 * Defines static factory methods for properties having a {@code Boolean} value.
 *
 * @author Kelemen Attila
 */
public final class BoolProperties {
    /**
     * Returns a property which is {@code true}, if, and only, if the values
     * of the properties are equal based on their equals method.
     * <P>
     * The returned property is notified of changes whenever the value of any of
     * the specified properties changes.
     * <P>
     * The value of the returned property is never {@code null}. This method is
     * effectively equivalent to calling
     * <pre>
     * BoolProperties.equals(property1, property2, Comparators.naturalEquality())
     * </pre>
     * <B>Warning</B>: It is assumed that the values of the properties are
     * immutable. Or at least, the result of the comparison may only change if
     * the value of the properties change (as defined by the
     * {@link PropertySource#addChangeListener(Runnable) change listener}).
     *
     * @param <ValueType> the type of the value of the properties
     * @param property1 the first property to be compared. This argument cannot
     *   be {@code null} (but its value can be {@code null}).
     * @param property2 the second property to be compared. This argument cannot
     *   be {@code null} (but its value can be {@code null}).
     * @return a property which is {@code true}, if, and only, if the values
     *   of the properties are equal based on the given comparison. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <ValueType> PropertySource<Boolean> equals(
            PropertySource<? extends ValueType> property1,
            PropertySource<? extends ValueType> property2) {
        return equals(property1, property2, Comparators.naturalEquality());
    }

    /**
     * Returns a property which is {@code true}, if, and only, if the values
     * of the properties are equal based on the given comparison.
     * <P>
     * The returned property is notified of changes whenever the value of any of
     * the specified properties changes.
     * <P>
     * The value of the returned property is never {@code null}.
     * <P>
     * <B>Warning</B>: It is assumed that the values of the properties are
     * immutable. Or at least, the result of the comparison may only change if
     * the value of the properties change (as defined by the
     * {@link PropertySource#addChangeListener(Runnable) change listener}).
     *
     * @param <ValueType> the type of the value of the properties
     * @param property1 the first property to be compared. This argument cannot
     *   be {@code null} (but its value can be {@code null}).
     * @param property2 the second property to be compared. This argument cannot
     *   be {@code null} (but its value can be {@code null}).
     * @param comparator the {@code EqualityComparator} defining what values are
     *   to be considered equivalent. This argument cannot be {@code null}.
     * @return a property which is {@code true}, if, and only, if the values
     *   of the properties are equal based on the given comparison. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <ValueType> PropertySource<Boolean> equals(
            PropertySource<? extends ValueType> property1,
            PropertySource<? extends ValueType> property2,
            EqualityComparator<? super ValueType> comparator) {
        return new CmpProperties(property1, property2, comparator);
    }

    private BoolProperties() {
        throw new AssertionError();
    }
}
