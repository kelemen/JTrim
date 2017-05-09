package org.jtrim2.concurrent.query;

import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Defines an {@code AsyncDataState} with a specific double value as the
 * state of progress and a string describing the current state.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are completely immutable and as such, safe to be
 * accessed by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 */
public final class SimpleDataState implements AsyncDataState {
    private final String state;
    private final double progress;

    /**
     * Initializes the {@code SimpleDataState} with the specified string
     * describing the current state of progress and a double value within
     * the range [0.0, 1.0] describing the estimated numerical state of
     * progress.
     *
     * @param state the string describing the current state of progress. This
     *   argument cannot be {@code null}.
     * @param progress the estimated numerical state of progress within the
     *   range [0.0, 1.0]. The value of this argument is not verified by this
     *   constructor.
     *
     * @throws NullPointerException thrown if the specified string state is
     *   {@code null}
     */
    public SimpleDataState(String state, double progress) {
        Objects.requireNonNull(state, "state");

        this.state = state;
        this.progress = progress;
    }

    /**
     * Returns the string describing the current state of progress. This method
     * returns the value specified at construction time.
     *
     * @return the string describing the current state of progress. This method
     *   never returns {@code null}.
     */
    public String getState() {
        return state;
    }

    /**
     * {@inheritDoc }
     * <P>
     * This implementation simply returns the value specified at construction
     * time.
     */
    @Override
    public double getProgress() {
        return progress;
    }

    /**
     * Returns the string representation of this {@code SimpleDataState} in no
     * particular format. The string representation contains both estimated
     * {@link #getProgress() numerical state of progress} and the
     * {@link #getState() string} describing the state of progress.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return new DecimalFormat("#.##").format(100.0 * progress)
                + "%: " + state;
    }
}
