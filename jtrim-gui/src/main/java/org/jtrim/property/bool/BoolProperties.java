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

    /**
     * Returns a property which is {@code true}, if, and only, if the value
     * of the given property and the specified constant value are equal based on
     * their equals method.
     * <P>
     * The returned property is notified of changes whenever the value of the
     * given property changes.
     * <P>
     * The value of the returned property is never {@code null}. This method is
     * effectively equivalent to calling
     * <pre>
     * BoolProperties.equalsWithConst(property, constValue, Comparators.naturalEquality())
     * </pre>
     * <P>
     * <B>Warning</B>: It is assumed that the value of the specified property
     * and the constant value is immutable. Or at least, the result of the
     * comparison may only change if the value of the specified property changes
     * (as defined by the {@link PropertySource#addChangeListener(Runnable) change listener}).
     *
     * @param <ValueType> the type of the value of the properties
     * @param property the property to be compared against the constant value.
     *   This argument cannot be {@code null} (but its value can be {@code null}).
     * @param constValue the constant value to be compared against the value of
     *   the specified property. This argument is allowed to be {@code null}
     *   (note however, that {@code null} only equals to {@code null} for any
     *   properly implemented {@code equals} method).
     * @return a property which is {@code true}, if, and only, if the values of
     *   the given property and the specified constant value are equal based on
     *   the given comparison. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified property or the
     *   comparison is {@code null}
     */
    public static <ValueType> PropertySource<Boolean> equalsWithConst(
            PropertySource<? extends ValueType> property,
            ValueType constValue) {
        return new CmpToConstProperty(property, constValue, Comparators.naturalEquality());
    }

    /**
     * Returns a property which is {@code true}, if, and only, if the value
     * of the given property and the specified constant value are equal based on
     * the given comparison.
     * <P>
     * The returned property is notified of changes whenever the value of the
     * given property changes.
     * <P>
     * The value of the returned property is never {@code null}.
     * <P>
     * <B>Warning</B>: It is assumed that the value of the specified property
     * and the constant value is immutable. Or at least, the result of the
     * comparison may only change if the value of the specified property changes
     * (as defined by the {@link PropertySource#addChangeListener(Runnable) change listener}).
     *
     * @param <ValueType> the type of the value of the properties
     * @param property the property to be compared against the constant value.
     *   This argument cannot be {@code null} (but its value can be {@code null}).
     * @param constValue the constant value to be compared against the value of
     *   the specified property. This argument is allowed to be {@code null}
     *   (note however, that {@code null} only equals to {@code null} for any
     *   properly implemented {@code EqualityComparator}).
     * @param comparator the {@code EqualityComparator} defining what values are
     *   to be considered equivalent. This argument cannot be {@code null}.
     * @return a property which is {@code true}, if, and only, if the values of
     *   the given property and the specified constant value are equal based on
     *   the given comparison. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified property or the
     *   comparison is {@code null}
     */
    public static <ValueType> PropertySource<Boolean> equalsWithConst(
            PropertySource<? extends ValueType> property,
            ValueType constValue,
            EqualityComparator<? super ValueType> comparator) {
        return new CmpToConstProperty(property, constValue, comparator);
    }

    private BoolProperties() {
        throw new AssertionError();
    }
}
