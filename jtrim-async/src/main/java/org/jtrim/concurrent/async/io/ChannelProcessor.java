package org.jtrim.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.Channel;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataState;

/**
 *
 * @author Kelemen Attila
 */
public interface ChannelProcessor<DataType, ChannelType extends Channel> {
    public static interface StateListener {
        public void setState(AsyncDataState state);
    }

    public void processChannel(
            ChannelType channel,
            AsyncDataListener<DataType> listener,
            StateListener stateListener) throws IOException;
}
