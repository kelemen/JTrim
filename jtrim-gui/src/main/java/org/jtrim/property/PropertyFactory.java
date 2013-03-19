package org.jtrim.property;

import java.util.Arrays;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines static factory methods for creating properties or propery sources.
 *
 * <h3>Thread safety</h3>
 * Methods of this class might be called concurrently from any thread.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are are <I>synchronization transparent</I> and might be
 * called from any context.
 *
 * @author Kelemen Attila
 */
public final class PropertyFactory {
    /**
     * Creates a new {@code MutableProperty} which stores the current value
     * of the property in an internal field and does not allow {@code null}
     * values. The {@link MutableProperty#setValue(Object)} method of the
     * returns {@code MutableProperty} will call listeners synchronously and
     * will call them regardless if the newly set value of the property is actually
     * different from the actual value.
     * <P>
     * This method call is equivalent to calling
     * {@code memProperty(initialValue, false)}.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param initialValue the initial value of the returned property. This
     *   argument cannot be {@code null}.
     * @return a new {@code MutableProperty} which stores the current value
     *   of the property in an internal field. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified initial value is
     *   {@code null}
     *
     * @see #combinedVerifier(List)
     * @see #notNullVerifier()
     * @see #noOpVerifier()
     * @see #typeCheckerVerifier(Class)
     */
    public static <ValueType> MutableProperty<ValueType> memProperty(ValueType initialValue) {
        return memProperty(initialValue, false);
    }

    /**
     * Creates a new {@code MutableProperty} which stores the current value
     * of the property in an internal field. The
     * {@link MutableProperty#setValue(Object)} method of the returns
     * {@code MutableProperty} will call listeners synchronously and will call
     * them regardless if the newly set value of the property is actually
     * different from the actual value.
     * <P>
     * This method call is equivalent to calling the other
     * {@link #memProperty(Object, PropertyVerifier) two arguments memProperty method}
     * with the appropriate {@code PropertyVerifier}.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param initialValue the initial value of the returned property. This
     *   value may only be {@code null} if and only if, the {@code allowNulls}
     *   argument is {@code true}.
     * @param allowNulls if {@code true} then {@code null} values are permitted
     *   for the returned property (i.e., any value is permitted), otherwise
     *   attempting to set {@code null} value for a property will yield a
     *   {@code NullPointerException} to be thrown
     * @return a new {@code MutableProperty} which stores the current value
     *   of the property in an internal field. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code PropertyVerifier} or
     *   the {@code PropertyPublisher} argument is {@code null}. May also be
     *   thrown if {@code allowNulls} is {@code false} and the specified initial
     *   value is {@code null}.
     *
     * @see #combinedVerifier(List)
     * @see #notNullVerifier()
     * @see #noOpVerifier()
     * @see #typeCheckerVerifier(Class)
     */
    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            boolean allowNulls) {
        if (allowNulls) {
            return memProperty(initialValue,
                    PropertyFactory.<ValueType>noOpVerifier(),
                    PropertyFactory.<ValueType>noOpPublisher());
        }
        else {
            return memProperty(initialValue,
                    PropertyFactory.<ValueType>notNullVerifier(),
                    PropertyFactory.<ValueType>noOpPublisher());
        }
    }

    /**
     * Creates a new {@code MutableProperty} which stores the current value
     * of the property in an internal field. The
     * {@link MutableProperty#setValue(Object)} method of the returns
     * {@code MutableProperty} will call listeners synchronously and will call
     * them regardless if the newly set value of the property is actually
     * different from the actual value.
     * <P>
     * This method may throw an exception if the specified
     * {@code PropertyVerifier} finds the initial value inappropriate.
     * <P>
     * This method call is equivalent to calling
     * {@code memProperty(initialValue, verifier, PropertyFactory<ValueType>noOpPublisher())}.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param initialValue the initial value of the returned property. This
     *   value will be passed to the specified {@code PropertyVerifier} and
     *   therefore must be a valid value according to this verifier.
     * @param verifier the {@code PropertyVerifier} which might create a
     *   defensive copy of the property value before storing in the internal
     *   field and may also verify the validity of the value. This argument
     *   cannot be {@code null}.
     * @return a new {@code MutableProperty} which stores the current value
     *   of the property in an internal field. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code PropertyVerifier} or
     *   the {@code PropertyPublisher} argument is {@code null}. May also be
     *   thrown if the {@code PropertyVerifier} does not allow {@code null}
     *   values and the specified initial value is {@code null}.
     *
     * @see #combinedVerifier(List)
     * @see #notNullVerifier()
     * @see #noOpVerifier()
     * @see #typeCheckerVerifier(Class)
     */
    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            PropertyVerifier<ValueType> verifier) {
        return memProperty(initialValue, verifier, PropertyFactory.<ValueType>noOpPublisher());
    }

    /**
     * Creates a new {@code MutableProperty} which stores the current value
     * of the property in an internal field. The
     * {@link MutableProperty#setValue(Object) setValue} method of the returned
     * {@code MutableProperty} will call listeners synchronously and will call
     * them regardless if the newly set value of the property is actually
     * different from the actual value.
     * <P>
     * This method may throw an exception if the specified
     * {@code PropertyVerifier} finds the initial value inappropriate.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param initialValue the initial value of the returned property. This
     *   value will be passed to the specified {@code PropertyVerifier} and
     *   therefore must be a valid value according to this verifier.
     * @param verifier the {@code PropertyVerifier} which might create a
     *   defensive copy of the property value before storing in the internal
     *   field and may also verify the validity of the value. This argument
     *   cannot be {@code null}.
     * @param publisher the {@code PropertyPublisher} which is used to make a
     *   defensive copy of the value of the property before returning
     *   (if needed). This argument cannot be {@code null}.
     * @return a new {@code MutableProperty} which stores the current value
     *   of the property in an internal field. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code PropertyVerifier} or
     *   the {@code PropertyPublisher} argument is {@code null}. May also be
     *   thrown if the {@code PropertyVerifier} does not allow {@code null}
     *   values and the specified initial value is {@code null}.
     *
     * @see #combinedVerifier(List)
     * @see #notNullVerifier()
     * @see #noOpVerifier()
     * @see #typeCheckerVerifier(Class)
     * @see #noOpPublisher()
     */
    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            PropertyVerifier<ValueType> verifier,
            PropertyPublisher<ValueType> publisher) {
        return new MemProperty<>(initialValue, verifier, publisher);
    }

    /**
     * Returns a {@code MutablePropertyProxy} implementation initially backed
     * by the specified {@code MutableProperty}. The
     * {@link MutablePropertyProxy#replaceProperty(MutableProperty) replaceProperty}
     * method of the returned {@code MutablePropertyProxy} will call listeners
     * synchronously and will call them regardless if the actual value of the
     * property changes or not.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param initialProperty the initial {@code MutableProperty} backing the
     *   returned {@code MutablePropertyProxy}. This argument cannot be
     *   {@code null}.
     * @return a {@code MutablePropertyProxy} implementation initially backed
     *   by the specified {@code MutableProperty}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   initial property is {@code null}
     */
    public static <ValueType> MutablePropertyProxy<ValueType> proxyProperty(
            MutableProperty<ValueType> initialProperty) {
        return new DefaultMutablePropertyProxy<>(initialProperty);
    }

    /**
     * Returns a {@code PropertySource} which always holds the same value.
     * Listeners registered with the {@code PropertySource} will never be
     * notified and will automatically be unregistered.
     * <P>
     * Note that every method of the returned {@code PropertySource} is
     * <I>synchronization transparent</I>.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param value the value of the returned property. This argument is allowed
     *   to be {@code null}. This value is recommended to be immutable because
     *   it is returned as is.
     * @return the property always holding the specified value. This method
     *   never returns {@code null}.
     *
     * @see #constSource(Object, PropertyPublisher) constSource(ValueType, PropertyPublisher)
     */
    public static <ValueType> PropertySource<ValueType> constSource(ValueType value) {
        return constSource(value, PropertyFactory.<ValueType>noOpPublisher());
    }

    /**
     * Returns a {@code PropertySource} which always holds the same value. The
     * returned {@code PropertySource} will always use the specified
     * {@code PropertyPublisher} to share its value. Listeners registered with
     * the {@code PropertySource} will never be notified and will automatically
     * be unregistered.
     * <P>
     * Note that every method of the returned {@code PropertySource} is
     * <I>synchronization transparent</I>.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param value the value of the returned property. This argument is allowed
     *   to be {@code null}.
     * @param publisher the {@code PropertyPublisher} used to share the value
     *   of the returned property (so it may make defensive copy of the value).
     *   This argument cannot be {@code null}.
     * @return the property always holding the specified value. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code PropertyPublisher} is {@code null}
     *
     * @see #noOpPublisher()
     */
    public static <ValueType> PropertySource<ValueType> constSource(
            ValueType value,
            PropertyPublisher<ValueType> publisher) {
        return new ConstSource<>(value, publisher);
    }

    /**
     * Returns a {@code PropertySource} delegating all of its method calls to
     * the specified {@code PropertySource}. This can be used to hide other
     * public methods of the specified {@code PropertySource} (e.g.: when
     * having a {@link MutableProperty}).
     *
     * @param <ValueType> the type of the value of the returned property
     * @param source the {@code PropertySource} to which the returned
     *   {@code PropertySource} delegates its method calls to. This argument
     *   cannot be {@code null}.
     * @return a {@code PropertySource} delegating all of its method calls to
     *   the specified {@code PropertySource}. This method never returns
     *   {@code null}.
     */
    public static <ValueType> PropertySource<ValueType> protectedView(
            PropertySource<? extends ValueType> source) {
        return new DelegatedPropertySource<>(source);
    }

    /**
     * Returns a {@code PropertySourceProxy} implementation initially backed
     * by the specified {@code PropertySource}. The
     * {@link PropertySourceProxy#replaceSource(PropertySource) replaceSource}
     * method of the returned {@code PropertySourceProxy} will call listeners
     * synchronously and will call them regardless if the actual value of the
     * property changes or not.
     *
     * @param <ValueType> the type of the value of the returned property
     * @param initialSource the initial {@code PropertySource} backing the
     *   returned {@code PropertySourceProxy}. This argument cannot be
     *   {@code null}.
     * @return a {@code PropertySourceProxy} implementation initially backed
     *   by the specified {@code PropertySource}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   initial property is {@code null}
     */
    public static <ValueType> PropertySourceProxy<ValueType> proxySource(
            PropertySource<? extends ValueType> initialSource) {
        return new DefaultPropertySourceProxy<>(initialSource);
    }

    /**
     * Returns a {@code PropertyVerifier} which does nothing but returns its
     * argument.
     *
     * @param <ValueType> the type of the value to be verified
     * @return a {@code PropertyVerifier} which does nothing but returns its
     *   argument. This method never returns {@code null}.
     */
    public static <ValueType> PropertyVerifier<ValueType> noOpVerifier() {
        return NoOpVerifier.getInstance();
    }

    /**
     * Returns a {@code PropertyVerifier} which throws a
     * {@code NullPointerException} if the passed argument is {@code null},
     * otherwise it will simply return its argument.
     *
     * @param <ValueType> the type of the value to be verified
     * @return a {@code PropertyVerifier} which verifies that its argument is
     *   not {@code null}. This method never returns {@code null}.
     */
    public static <ValueType> PropertyVerifier<ValueType> notNullVerifier() {
        return NotNullVerifier.getInstance();
    }

     /**
     * Returns a {@code PropertyVerifier} which throws an
     * {@code IllegalArgumentException} if the passed argument does not
     * implement the specified type, otherwise it will simply return its
     * argument. The returned {@code PropertyVerifier} always allows
     * {@code null} values.
     *
     * @param <ValueType> the type of the value to be verified
     * @param expectedType the type which the argument of the
     *   {@code PropertyVerifier} must implement. This argument cannot be
     *   {@code null}.
     * @return a {@code PropertyVerifier} which verifies that its argument
     *   implements the specified type. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified type is {@code null}
     */
    public static <ValueType> PropertyVerifier<ValueType> typeCheckerVerifier(
            Class<ValueType> expectedType) {
        return new TypeCheckerVerifier<>(expectedType);
    }

    /**
     * Returns a {@code PropertyVerifier} calling both specified
     * {@code PropertyVerifier} in the order they were specified.
     * <P>
     * This is just a convenience method for calling
     * {@link #combinedVerifier(List)}.
     *
     * @param <ValueType> the type of the value to be verified
     * @param verifier1 the {@code PropertyVerifier} to be applied first.
     *   This argument cannot be {@code null}.
     * @param verifier2 the {@code PropertyVerifier} to be applied second.
     *   This argument cannot be {@code null}.
     * @return a {@code PropertyVerifier} calling each specified
     *   {@code PropertyVerifier} in the order they were specified for a given
     *   argument. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code PropertyVerifier} is {@code null}
     *
     * @see #combinedVerifier(PropertyVerifier, PropertyVerifier)
     */
    public static <ValueType> PropertyVerifier<ValueType> combinedVerifier(
            PropertyVerifier<ValueType> verifier1,
            PropertyVerifier<ValueType> verifier2) {
        return combinedVerifier(Arrays.asList(verifier1, verifier2));
    }

    /**
     * Returns a {@code PropertyVerifier} calling each specified
     * {@code PropertyVerifier} in the order they were specified for a given
     * argument.
     * <P>
     * You may specify an empty list of {@code PropertyVerifier} instances, in
     * which case, this method will return a {@code PropertyVerifier} doing
     * nothing but returning its argument.
     *
     * @param <ValueType> the type of the value to be verified
     * @param verifiers the list {@code PropertyVerifier} to be applied in the
     *   order they need to be applied. This argument cannot be {@code null}
     *   and cannot contain {@code null} elements.
     * @return a {@code PropertyVerifier} calling each specified
     *   {@code PropertyVerifier} in the order they were specified for a given
     *   argument. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified list or any of its
     *   element is {@code null}.
     *
     * @see #combinedVerifier(PropertyVerifier, PropertyVerifier)
     */
    public static <ValueType> PropertyVerifier<ValueType> combinedVerifier(
            List<? extends PropertyVerifier<ValueType>> verifiers) {

        switch (verifiers.size()) {
            case 0:
                return noOpVerifier();
            case 1: {
                PropertyVerifier<ValueType> result = verifiers.get(0);
                ExceptionHelper.checkNotNullArgument(result, "verifiers[0]");
                return result;
            }
            default:
                return new CombinedVerifier<>(verifiers);
        }
    }

    /**
     * Returns a {@code PropertyVerifier} verifying the elements of a list.
     * The returned {@code PropertyVerifier} will always return an unmodifiable
     * copy of the list passed to it. Therefore, if the elements of the list are
     * immutable, the list returned by the {@code PropertyVerifier} might be
     * safe to share.
     *
     * @param <ValueType> the type of the elements of the list to be verified
     * @param elementVerifier the {@code PropertyVerifier} called for each
     *   element of the list passed to the returned {@code PropertyVerifier}.
     *   This argument cannot be {@code null}
     * @param allowNullList {@code true} if the passed list is allowed to be
     *   {@code null}, {@code false} otherwise. If this argument is
     *   {@code true} and the list to be verified is {@code null} then
     *   {@code null} is returned by the returned {@code PropertyVerifier}. If
     *   this argument is {@code false} and the list to be verified is
     *   {@code null} then {@code NullPointerException} is thrown by the
     *   returned {@code PropertyVerifier}.
     * @return a {@code PropertyVerifier} verifying the elements of a list.
     *   This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code PropertyVerifier} is {@code null}
     */
    public static <ValueType> PropertyVerifier<List<ValueType>> listVerifier(
            PropertyVerifier<ValueType> elementVerifier,
            boolean allowNullList) {
        return new ListVerifier<>(elementVerifier, allowNullList);
    }

    /**
     * Returns a {@code PropertyPublisher} which does nothing but returns its
     * argument.
     *
     * @param <ValueType> the type of the value to be stored
     * @return a {@code PropertyPublisher} which does nothing but returns its
     *   argument. This method never returns {@code null}.
     */
    public static <ValueType> PropertyPublisher<ValueType> noOpPublisher() {
        return NoOpPublisher.getInstance();
    }

    private PropertyFactory() {
        throw new AssertionError();
    }
}
