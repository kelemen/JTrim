package org.jtrim.property.bool;

import org.jtrim.collections.Equality;
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
     * of the properties are equal based on their reference.
     * <P>
     * The returned property is notified of changes whenever the value of any of
     * the specified properties changes.
     * <P>
     * The value of the returned property is never {@code null}. This method is
     * effectively equivalent to calling
     * <pre>
     * BoolProperties.equals(property1, property2, Equality.referenceEquality())
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
    public static <ValueType> PropertySource<Boolean> same(
            PropertySource<? extends ValueType> property1,
            PropertySource<? extends ValueType> property2) {
        return equals(property1, property2, Equality.referenceEquality());
    }

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
     * BoolProperties.equals(property1, property2, Equality.naturalEquality())
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
        return equals(property1, property2, Equality.naturalEquality());
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
        return new CmpProperty(property1, property2, comparator);
    }

    /**
     * Returns a property which is {@code true}, if, and only, if the value
     * of the given property and the specified constant value are equal based on
     * their reference.
     * <P>
     * The returned property is notified of changes whenever the value of the
     * given property changes.
     * <P>
     * The value of the returned property is never {@code null}. This method is
     * effectively equivalent to calling
     * <pre>
     * BoolProperties.equalsWithConst(property, constValue, Equality.referenceEquality())
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
    public static <ValueType> PropertySource<Boolean> sameWithConst(
            PropertySource<? extends ValueType> property,
            ValueType constValue) {
        return new CmpToConstProperty(property, constValue, Equality.referenceEquality());
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
     * BoolProperties.equalsWithConst(property, constValue, Equality.naturalEquality())
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
        return new CmpToConstProperty(property, constValue, Equality.naturalEquality());
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

    /**
     * Returns a property which has the negated value of the specified property.
     * If the value of the specified property is {@code null}, then the
     * value of the returned property is also {@code null}. That is, the
     * returned property applies the following conversion:
     * <ul>
     *  <li>{@code false} -> {@code true}</li>
     *  <li>{@code true} -> {@code false}</li>
     *  <li>{@code null} -> {@code null}</li>
     * </ul>
     *
     * @param property the property whose value is to be negated. This argument
     *   cannot be {@code null}.
     * @return  a property which has the negated value of the specified
     *   property. This method never returns {@code null}.
     */
    public static PropertySource<Boolean> not(PropertySource<Boolean> property) {
        return new NotProperty(property);
    }

    /**
     * Returns a property which is {@code true}, if, and only, if at least one
     * of the specified property is {@code true}. That is, the result is the
     * logical "or" of the values of the specified properties (defining
     * {@code null} to be {@code false}).
     * <P>
     * Corner cases:
     * <ul>
     *  <li>
     *   {@code null} value for the specified property is equivalent to
     *   {@code false}.
     *  </li>
     *  <li>
     *   Specifying zero properties will yield in a property always being
     *   {@code false}.
     *  </li>
     * </ul>
     * The value of the returned property can never be {@code null}.
     *
     * @param properties the properties whose logical "or" is to be returned.
     *   This argument cannot be {@code null} and none of the specified
     *   properties can be {@code null}.
     * @return a property which is {@code true}, if, and only, if at least one
     *   of the specified property is {@code true}. This method never returns
     *   {@code null}.
     */
    @SafeVarargs
    public static PropertySource<Boolean> or(PropertySource<Boolean>... properties) {
        return new OrProperty(properties);
    }

    /**
     * Returns a property which is {@code true}, if, and only, if none of the
     * specified property is {@code true}. That is, the result is the logical
     * "and" of the values of the specified properties (defining {@code null} to
     * be {@code true}).
     * <P>
     * Corner cases:
     * <ul>
     *  <li>
     *   {@code null} value for the specified property is equivalent to
     *   {@code true}.
     *  </li>
     *  <li>
     *   Specifying zero properties will yield in a property always being
     *   {@code true}.
     *  </li>
     * </ul>
     * The value of the returned property can never be {@code null}.
     *
     * @param properties the properties whose logical "or" is to be returned.
     *   This argument cannot be {@code null} and none of the specified
     *   properties can be {@code null}.
     * @return a property which is {@code true}, if, and only, if at least one
     *   of the specified property is {@code true}. This method never returns
     *   {@code null}.
     */
    @SafeVarargs
    public static PropertySource<Boolean> and(PropertySource<Boolean>... properties) {
        return new AndProperty(properties);
    }

    private BoolProperties() {
        throw new AssertionError();
    }
}
