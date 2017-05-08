package org.jtrim2.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.Channel;
import org.jtrim2.concurrent.async.AsyncDataListener;
import org.jtrim2.concurrent.async.AsyncDataState;

/**
 * Defines an interface for processing the data of an open {@link Channel} and
 * to forward the processed data to an {@link AsyncDataListener}.
 * <P>
 * This interface was designed for the {@link AsyncChannelLink} which relies
 * on a {@code ChannelProcessor} to actually process the content of the channel
 * opened and provide the requested data.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface must be safe to be accessed by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the data provided by the
 *   {@code ChannelProcessor} to the {@code AsyncDataListener}
 * @param <ChannelType> the type of the channel which can be handled and
 *   processed by the {@code ChannelProcessor}
 */
public interface ChannelProcessor<DataType, ChannelType extends Channel> {
    /**
     * The interface through which the {@link ChannelProcessor} may set the
     * current state of progress of the processing of the channel.
     * <P>
     * The {@code processChannel} method of the {@code ChannelProcessor} may
     * invoke the {@link #setState(AsyncDataState) setState} method of this
     * interface to notify the caller that it progressed in processing the
     * channel.
     */
    public static interface StateListener {
        /**
         * Sets the current state of progress of the processing of the channel.
         * Invoking this method overwrites the state value set by previous
         * invocations to this method.
         * <P>
         * The state of progress set by this method is intended to be retrievable
         * through the {@link org.jtrim2.concurrent.async.AsyncDataController#getDataState() getDataState}
         * method of the {@link org.jtrim2.concurrent.async.AsyncDataController AsyncDataController}
         * for the threads inspecting the progress of the data retrieval process
         * backed by a {@code ChannelProcessor}.
         *
         * @param state the new state of progress of the processing of the
         *   channel. This argument can be {@code null} since
         *   {@code AsyncDataController} supports {@code null} states but it is
         *   not recommended to specify {@code null} for this argument.
         */
        public void setState(AsyncDataState state);
    }

    /**
     * Processes the content of specified open channel and forwards the
     * processed content to the specified {@link AsyncDataListener}. This method
     * may or may not call the
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} method
     * of the specified listener but if it does, it must honor the contract of
     * the {@code AsyncDataListener} and stop forwarding anymore data.
     *
     * @param channel the channel whose content is to be processed and be
     *   forwarded to the specified listener. This channel may only be accessed
     *   in this method call and does not need to be closed in this method call.
     *   This argument cannot be {@code null}.
     * @param listener the listener to which processed data is to be forwarded.
     *   The data maybe forwarded only during this method call. The
     *   {@code onDoneReceive} method of this listener does not need to be
     *   called by this method but otherwise, this method must honor the
     *   contract of the listener. This argument cannot be {@code null}.
     * @param stateListener the {@code StateListener} through which this method
     *   call may show its current progress of processing the channel. This
     *   argument cannot be {@code null}.
     *
     * @throws IOException thrown if there was some error, while processing the
     *   channel. Throwing this exception, of course, means the termination of
     *   the data providing.
     */
    public void processChannel(
            ChannelType channel,
            AsyncDataListener<DataType> listener,
            StateListener stateListener) throws IOException;
}
