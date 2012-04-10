package org.jtrim.concurrent.async;

import java.util.Arrays;
import java.util.List;
import org.jtrim.collections.ArraysEx;

/**
 * Defines an aggregate state of progress of multiple data retrieval processes.
 * <P>
 * {@link AsyncDataLink} instances relying on multiple other
 * {@link AsyncDataLink} instances may choose to return an instance of
 * {@code MultiAsyncDataState} containing all the data states. The order of
 * states are important: each state's data retrieval process is considered to be
 * the subprocess of the data retrieval process of the previous state.
 * <P>
 * Note that the {@code AsyncDataLink} instance created by the
 * {@link AsyncDatas#convertResult(AsyncDataLink, AsyncDataQuery)} method
 * returns {@code MultiAsyncDataState} instances as its state of progress.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. The wrapped list of {@code AsyncDataState} instances cannot be
 * modified after construction time.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see AsyncDatas#convertResult(AsyncDataLink, AsyncDataQuery)
 * @see AsyncDatas#convertResults(AsyncDataQuery, AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
public final class MultiAsyncDataState implements AsyncDataState {
    private final AsyncDataState[] subStates;

    /**
     * Initializes the {@code MultiAsyncDataState} with the specified underlying
     * states.
     *
     * @param states the underlying {@code AsyncDataState} instances in the
     *   order of significance. The content of the passed array will be copied,
     *   so modifying the passed array after this constructor returns will have
     *   no effect on the newly created instance. This argument cannot be
     *   {@code null} but its elements are allowed to be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataState} array is {@code null}
     */
    public MultiAsyncDataState(AsyncDataState... states) {
        this.subStates = states.clone();
    }

    /**
     * Initializes the {@code MultiAsyncDataState} with the specified underlying
     * states.
     *
     * @param states the underlying {@code AsyncDataState} instances in the
     *   order of significance. The content of the passed list will be copied,
     *   so modifying the passed list after this constructor returns will have
     *   no effect on the newly created instance. This argument cannot be
     *   {@code null} but its elements are allowed to be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataState} list is {@code null}
     */
    public MultiAsyncDataState(List<? extends AsyncDataState> states) {
        this.subStates = states.toArray(new AsyncDataState[states.size()]);
    }

    // The returned array is read by LinkedAsyncDataLink and a reference to the
    // array is returned for better performance.
    AsyncDataState[] getSubStates() {
        return subStates;
    }

    /**
     * Returns a read-only list of the underlying {@code AsyncDataState}
     * instances in the order they were specified at construction time.
     * <P>
     * This method returns a list with the same element as if they were returned
     * by the {@link #getSubState(int) getSubState(int)} method and will have
     * the same number of elements as returned by the
     * {@link #getSubStateCount() getSubStateCount()} method.
     *
     * @return a read-only list of the underlying {@code AsyncDataState}
     *   instances in the order they were specified at construction time. This
     *   method never returns {@code null} but the returned list may contain
     *   {@code null} elements at positions where {@code null} was specified
     *   at construction time.
     *
     * @see #getSubStateCount()
     * @see #getSubState(int)
     */
    public List<AsyncDataState> getSubStateList() {
        return ArraysEx.viewAsList(subStates);
    }

    /**
     * Returns the number of underlying {@code AsyncDataState} instances. That
     * is, the number of {@code AsyncDataState} instances specified at
     * construction time.
     * <P>
     * The returned integer is the exclusive upper bound for the argument of the
     * {@link #getSubState(int) getSubState(int)} method. The return value of
     * this method is constant throughout the life time of this
     * {@code MultiAsyncDataState}.
     *
     * @return the number of underlying {@code AsyncDataState} instances. This
     *   method always returns an integer greater than or equal to zero.
     *
     * @see #getSubState(int)
     */
    public int getSubStateCount() {
        return subStates.length;
    }

    /**
     * Returns the underlying state at the given index. That is, the
     * {@code AsyncDataState} instance specified at the given index at
     * construction time. The inclusive lower bound for the index is zero and
     * the exclusive upper bound is the value returned by the
     * {@link #getSubStateCount() getSubStateCount()} method.
     * <P>
     * Note that this method is effectively the same as invoking:
     * {@code getSubStateList().get(index)} but more efficient.
     *
     * @param index the index of the underlying state to be returned. This
     *   argument must be greater than or equal to zero and lower than the value
     *   returned by the {@code getSubStateCount()} method.
     * @return the underlying state at the given index. This method may return
     *   {@code null} if such state was specified at the given index at
     *   construction time.
     *
     * @throws IndexOutOfBoundsException thrown if the specified index is
     *   not within its valid range
     *
     * @see #getSubStateCount()
     */
    public AsyncDataState getSubState(int index) {
        return subStates[index];
    }

    /**
     * Returns the {@link AsyncDataState#getProgress() progress value} of the
     * underlying state at the given index. The progress value for {@code null}
     * states is defined to be zero.
     * <P>
     * This method is effectively the same as the
     * {@code getSubState(index) != null ? getSubState(index).getProgress() : 0.0}
     * expression.
     *
     * @param index the index of the underlying state whose progress value is
     *   to be returned. This
     *   argument must be greater than or equal to zero and lower than the value
     *   returned by the {@link #getSubStateCount() getSubStateCount()} method.
     * @return the progress value of the state at the given index
     *
     * @throws IndexOutOfBoundsException thrown if the specified index is
     *   not within its valid range
     *
     * @see #getSubStateCount()
     */
    public double getSubProgress(int index) {
        AsyncDataState state = subStates[index];
        return state != null ? state.getProgress() : 0.0;
    }

    /**
     * Returns an aggregated progress value of the underlying states.
     * <P>
     * How this aggregated progress value is calculated is not defined but the
     * following condition holds for the resulting progress:
     * If for two different invocation, the first N state has the same progress
     * value but the progress value at {@code N + 1} position is different, then
     * this method will return greater or equal value for the invocation where
     * the progress value at the {@code N + 1} position is greater. When the
     * underlying progress values are all the same, this method will return the
     * same value for both invocation.
     * <P>
     * In case all the underlying {@code AsyncDataState} instances return a
     * value within the range {@code [0.0, 1.0]}, this method will also return
     * a value within this range.
     *
     * @return the aggregated progress value of the underlying states
     */
    @Override
    public double getProgress() {
        final AsyncDataState[] localSubStates = subStates;
        double result = 0.0;

        for (int i = 0; i < localSubStates.length; i++) {
            AsyncDataState state = localSubStates[i];
            if (state != null) {
                result = result + (1.0 - result) * state.getProgress();
            }
        }
        return result;
    }

    /**
     * Returns the string representation of this {@code MultiAsyncDataState} in
     * no particular format. The string representation contains the string
     * representation of all the underlying states.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "MultiAsyncDataState " + Arrays.toString(subStates);
    }
}
