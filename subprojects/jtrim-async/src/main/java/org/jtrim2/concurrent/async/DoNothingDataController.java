package org.jtrim2.concurrent.async;

/**
 * Defines an {@code AsyncDataController} which does nothing and always returns
 * {@code null} as the {@link #getDataState() state of progress}.
 * <P>
 * This class is a singleton and its one and only instance is:
 * {@link #INSTANCE}.
 *
 * <h3>Thread safety</h3>
 * The {@link #INSTANCE instance} of this class are immutable and as such is
 * safe to be accessed by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public enum DoNothingDataController implements AsyncDataController {
    /**
     * The one and only instance of {@code DoNothingDataController}.
     */
    INSTANCE;

    /**
     * Does nothing and returns immediately to the caller.
     *
     * @param controlArg this argument is ignored.
     */
    @Override
    public void controlData(Object controlArg) {
    }

    /**
     * Does nothing and returns {@code null}.
     *
     * @return {@code null} always
     */
    @Override
    public AsyncDataState getDataState() {
        return null;
    }

    /**
     * Returns the string representation of this {@code DoNothingDataController}
     * in no particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "DoNothingController";
    }
}
