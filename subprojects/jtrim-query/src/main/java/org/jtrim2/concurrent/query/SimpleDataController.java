package org.jtrim2.concurrent.query;

/**
 * An {@code AsyncDataController} implementation which stores a volatile
 * reference to an {@code AsyncDataState} but otherwise does nothing.
 * <P>
 * The {@link #getDataState() getDataState} method returns the currently set
 * reference to the state of progress.
 * <P>
 * Note that it is safer to wrap instances of this class in a
 * {@code DelegatedAsyncDataController} before sharing it with external code,
 * so the external code will be unable to modify the state of progress.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Instances of this class cannot be directly modified, only
 * its {@link #getDataState() state of progress} if it is mutable. In case the
 * state of progress is immutable (and it is recommended to be so), then the
 * {@code SimpleDataController} instance is completely immutable.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see DelegatedAsyncDataController
 */
public final class SimpleDataController implements AsyncDataController {
    private volatile AsyncDataState state;

    /**
     * Initializes the {@code SimpleDataController} with {@code null} as
     * the {@link #getDataState() state of progress}.
     * <P>
     * Until {@link #setDataState(AsyncDataState) setDataState} has been called,
     * {@code null} will be returned by the {@link #getDataState() getDataState}
     * method.
     */
    public SimpleDataController() {
        this(null);
    }

    /**
     * Initializes the {@code SimpleDataController} with the specified
     * {@link #getDataState() state of progress}.
     *
     * @param firstState the {@code AsyncDataState} which will be returned by
     *   the {@link #getDataState() getDataState()} method until
     *   {@link #setDataState(AsyncDataState) setDataState} has been called.
     *   This argument can be {@code null}.
     */
    public SimpleDataController(AsyncDataState firstState) {
        this.state = firstState;
    }

    /**
     * Sets the volatile reference to the stored {@code AsyncDataState} to the
     * one specified. Subsequent calls the
     * {@link #getDataState() getDataState()} method will return the state
     * specified.
     *
     * @param newState the {@code AsyncDataState} which is to be returned by the
     *   {@link #getDataState() getDataState()} method. This argument can be
     *   {@code null}.
     */
    public void setDataState(AsyncDataState newState) {
        this.state = newState;
    }

    /**
     * This method does nothing and returns immediately to the caller.
     *
     * @param controlArg this argument is ignored
     */
    @Override
    public void controlData(Object controlArg) {
    }

    /**
     * Returns the {@code AsyncDataState} object which was last set by the
     * {@link #setDataState(AsyncDataState) setDataState()} method or the one
     * specified at construction time if there was no {@code setDataState()}
     * call yet.
     *
     * @return the {@code AsyncDataState} which was last set by the
     *   {@link #setDataState(AsyncDataState) setDataState()} method or the one
     *   specified at construction time if there was no {@code setDataState()}
     *   call yet. This method may return {@code null} if {@code null} was set.
     */
    @Override
    public AsyncDataState getDataState() {
        return state;
    }
}
