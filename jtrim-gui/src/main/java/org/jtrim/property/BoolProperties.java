package org.jtrim.property;

import org.jtrim.collections.Equality;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

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
     * of the properties are equal based on their equals method (same as
     * {@code equalProperties}).
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
     * <P>
     * Note: This method does the same thing as
     * {@link #equalProperties(PropertySource, PropertySource) equalProperties}.
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
     * of the properties are equal based on their equals method (same as
     * {@code equals}).
     * <P>
     * The returned property is notified of changes whenever the value of any of
     * the specified properties changes.
     * <P>
     * The value of the returned property is never {@code null}. This method is
     * effectively equivalent to calling
     * <pre>
     * BoolProperties.equalProperties(property1, property2, Equality.naturalEquality())
     * </pre>
     * <B>Warning</B>: It is assumed that the values of the properties are
     * immutable. Or at least, the result of the comparison may only change if
     * the value of the properties change (as defined by the
     * {@link PropertySource#addChangeListener(Runnable) change listener}).
     * <P>
     * Note: This method does the same thing as
     * {@link #equals(PropertySource, PropertySource) equals}. This method is
     * only provided, because {@code equals} cannot be used with static imports.
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
    public static <ValueType> PropertySource<Boolean> equalProperties(
            PropertySource<? extends ValueType> property1,
            PropertySource<? extends ValueType> property2) {
        return equals(property1, property2, Equality.naturalEquality());
    }

    /**
     * Returns a property which is {@code true}, if, and only, if the values
     * of the properties are equal based on the given comparison (same as
     * {@code equalProperties}).
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
     * <P>
     * Note: This method does the same thing as
     * {@link #equalProperties(PropertySource, PropertySource, EqualityComparator) equalProperties}.
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
     * Returns a property which is {@code true}, if, and only, if the values
     * of the properties are equal based on the given comparison (same as
     * {@code equals}).
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
     * <P>
     * Note: This method does the same thing as
     * {@link #equals(PropertySource, PropertySource, EqualityComparator) equals}.
     * This method is only provided, because {@code equals} cannot be used with
     * static imports.
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
    public static <ValueType> PropertySource<Boolean> equalProperties(
            PropertySource<? extends ValueType> property1,
            PropertySource<? extends ValueType> property2,
            EqualityComparator<? super ValueType> comparator) {
        return new CmpProperty(property1, property2, comparator);
    }

    /**
     * Returns a property which is {@code true}, if, and only, if the value
     * of the given property is {@code null}.
     * <P>
     * The returned property is notified of changes whenever the value of the
     * given property changes.
     * <P>
     * The value of the returned property is never {@code null}. This method is
     * effectively equivalent to calling
     * <pre>
     * BoolProperties.sameWithConst(property, null)
     * </pre>
     *
     * @param <ValueType> the type of the value of the properties
     * @param property the property to be compared against {@code null}.
     *   This argument cannot be {@code null}.
     * @return a property which is {@code true}, if, and only, if the value of
     *   the given property is {@code null}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified property is {@code null}
     */
    public static <ValueType> PropertySource<Boolean> isNull(PropertySource<? extends ValueType> property) {
        return sameWithConst(property, null);
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
     * @throws NullPointerException thrown if the specified property is {@code null}
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
     * @throws NullPointerException thrown if the specified property is {@code null}
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

    private static Runnable listenerForwarderTask(
            final PropertySource<Boolean> property,
            final BoolPropertyListener listener) {

        return new Runnable() {
            private Boolean prevValue = null;

            @Override
            public void run() {
                Boolean newValueObj = property.getValue();
                boolean newValue = newValueObj != null
                        ? newValueObj.booleanValue()
                        : false;

                if (prevValue == null || prevValue.booleanValue() != newValue) {
                    prevValue = newValue;
                    listener.onChangeValue(newValue);
                }
            }
        };
    }

    /**
     * Adds a {@code BoolPropertyListener} to be notified when the specified
     * property changes. The listener is notified lazily just like
     * {@link PropertyFactory#lazilyNotifiedSource(PropertySource) PropertyFactory.lazilyNotifiedSource}
     * does.
     * <P>
     * Note that this method treats {@code null} values of the specified
     * property as {@code false} values. If you don't like this behaviour,
     * wrap the specified property and convert its {@code null} values to
     * {@code true}.
     *
     * @param property the {@code PropertySource} which is to be checked for
     *   changes. This argument cannot be {@code null}.
     * @param listener the listener to be notified whenever the value of the
     *   specified {@code PropertySource} changes. This argument cannot be
     *   {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listener, so that it will no longer be notified of
     *   subsequent changes. This method may never return {@code null}.
     */
    public static ListenerRef addBoolPropertyListener(
            PropertySource<Boolean> property,
            BoolPropertyListener listener) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        return property.addChangeListener(listenerForwarderTask(property, listener));
    }

    /**
     * Adds a {@code BoolPropertyListener} to be notified when the specified
     * property changes on a given executor. The listener is notified lazily just like
     * {@link PropertyFactory#lazilyNotifiedSource(PropertySource) PropertyFactory.lazilyNotifiedSource}
     * does.
     * <P>
     * Note that this method treats {@code null} values of the specified
     * property as {@code false} values. If you don't like this behaviour,
     * wrap the specified property and convert its {@code null} values to
     * {@code true}.
     * <P>
     * <B>Warning</B>: The specified executor should not execute tasks
     * concurrently, otherwise the listener might get notified concurrently,
     * which is not expected by {@code BoolPropertyListener} implementations.
     *
     * @param property the {@code PropertySource} which is to be checked for
     *   changes. This argument cannot be {@code null}.
     * @param listener the listener to be notified whenever the value of the
     *   specified {@code PropertySource} changes. This argument cannot be
     *   {@code null}.
     * @param executor the executor on which the
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listener, so that it will no longer be notified of
     *   subsequent changes. This method may never return {@code null}.
     */
    public static ListenerRef addBoolPropertyListener(
            PropertySource<Boolean> property,
            BoolPropertyListener listener,
            final UpdateTaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        final Runnable listenerForwarderTask = listenerForwarderTask(property, listener);
        return property.addChangeListener(() -> {
            executor.execute(listenerForwarderTask);
        });
    }

    private BoolProperties() {
        throw new AssertionError();
    }
}
