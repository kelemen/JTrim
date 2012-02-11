package org.jtrim.concurrent.async;

/**
 * The {@code AsyncDataController} used to control the way the data is provided
 * by an {@link AsyncDataLink} and the state of providing the data.
 *
 * <h3>Controlling the data providing process</h3> Although {@code AsyncDataLink}
 * instances are assumed to be linked to a particular data, they may provide
 * that data in multiple steps by providing more and more accurate version of
 * the data. This can be controlled by the
 * {@link #controlData(Object) controlData} method. <P> Many times it may occur
 * that the requested data is no longer needed, in this case the {@link #cancel() cancel}
 * method can be called to stop providing anymore data. Note however, that
 * canceling a task is only a request and in the extreme case, implementations
 * of {@code AsyncDataLink} may completely ignore this request. However, it is
 * recommended for every implementations of
 * {@code AsyncDataLink} to stop providing data as soon as possible when
 * cancellation was request.
 *
 * <h3>Checking the state of the data providing process</h3> Sometimes it is
 * good to know what actually is the {@code AsyncDataLink} providing the data is
 * doing. This knowledge can be used for various purposes like visually
 * displaying the progress to the user or tracking the cause of bugs when a data
 * is never loaded (e.g.: due to a deadlock). For this purpose implementations
 * must provide this state through the
 * {@link #getDataState() getDataState} method. This state as a bare minimum
 * must provide a double value between 0.0 and 1.0 of the state of the current
 * progress.
 *
 * <h3>Thread safety</h3> Implementations of this interface are required to be
 * safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4> Implementations of this interface are
 * not required to be <I>synchronization transparent</I> except for the
 * {@link #getDataState() getDataState} method which must be implemented so that
 * its synchronization is invisible to users of this interface in terms of
 * correctness.
 *
 * @see AsyncDataLink
 * @see AsyncDataLink#getData(AsyncDataListener)
 * @see InitLaterDataController
 */
public interface AsyncDataController {
    /**
     * Notifies the {@code AsyncDataLink} providing the data, that the data
     * should be provided in a specific way. This may affect the intermediate
     * incomplete datas only and regardless how and when this method was called
     * it may not affect the final and complete data provided by the
     * {@code AsyncDataLink}.
     * <P>
     * How exactly the data providing can be affected is entirely implementation
     * dependent. For example an implementation may allow requests like
     * "I don't need intermediate data, only the final one" or set up some
     * priorities, that some part of the data should be provided as soon as
     * possible and others are less important. Also implementations may choose
     * to completely ignore this method call.
     * <P>
     * In general the order of the control arguments of multiple
     * {@code controlData} method call is considered to be important.
     * <P>
     * Note however, that this method only hints the {@code AsyncDataLink} and
     * usually implementations cannot make any hard guarantees that they will
     * honor the request.
     *
     * @param controlArg the control arguments defining the way how the data
     *   should be provided. Implementations may make various restrictions on
     *   what this argument is allowed to be (e.g.: its class). Note that the
     *   control argument in general is expected to be safely sharable across
     *   multiple threads concurrently and to be a relatively lightweight
     *   object. That is, it is highly recommended that control arguments be
     *   simple immutable objects only containing a minimal number of hints.
     *
     * @throws IllegalArgumentException implementations may choose to throw
     *   this exception if they find that the passed argument is inappropriate
     * @throws NullPointerException implementations may choose to throw this
     *   exception if they do not support {@code null} as a control object
     * @throws ClassCastException implementations may choose to throw this
     *   exception if the passed control object is not an instance of a required
     *   class
     */
    public void controlData(Object controlArg);

    /**
     * Attempts to cancel the providing of the data provided by the
     * {@code AsyncDataLink}.
     * <P>
     * Canceling must be interpreted so that the requested data is no longer
     * needed and if provided, it will be discarded. {@code AsyncDataLink}
     * implementations should terminate sending the data as soon as possible.
     * That is, they should finish forwarding the data by invoking the
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} method
     * of the listener. They may also do so synchronously in this method call.
     * <P>
     * Note however, that this method call is only a request and implementations
     * may ignore it.
     * <P>
     * This method must be implemented in a way, so that it never blocks for an
     * extended period of time. For example: It must not do any kind of IO
     * operations or wait for any external events.
     * <P>
     * <B>Thread safety reminder</B>: This method must not be considered
     * <I>synchronization transparent</I>, so for example: Holding a lock while
     * calling this method is explicitly forbidden.
     */
    public void cancel();

    /**
     * Returns the current progress of the data providing process. The progress
     * as a minimum must contain a double value between 0.0 and 1.0 which
     * defines the rough progress where 0.0 means "just started" and 1.0 means
     * (almost) completed.
     * <P>
     * This method must be implemented to return as quickly as possible. That
     * is, this method should do little more than accessing a {@code volatile}
     * field or similar actions.
     * <P>
     * In case actual providing of the data has not yet been started and the
     * implementation knows little or nothing about what kind of data will be
     * provided, it should return a {@code null} {@code AsyncDataState} rather
     * than a state with an arbitrary chosen class.
     *
     * @return the current state of progress of the data providing process. This
     *   method may return {@code null} if the state is not yet available.
     */
    public AsyncDataState getDataState();
}
