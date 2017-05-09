package org.jtrim2.concurrent.query.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.SimpleDataState;

/**
 * Defines a {@code ChannelProcessor} which reads and deserializes the first
 * object from the specified channel. This {@code ChannelProcessor} requires a
 * {@code ReadableByteChannel} to read the bytes from. The object read from the
 * channel will be forwarded to the specified {@code AsyncDataListener}.
 *
 * <h3>Thread safety</h3>
 * As required by the {@code ChannelProcessor}, this class is safe to be
 * accessed by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @see AsyncChannelLink
 */
public final class DeserializerChannelProcessor
implements
        ChannelProcessor<Object, ReadableByteChannel> {

    /**
     * Creates a new {@code DeserializerChannelProcessor} which is ready to
     * read the objects from the provided channel.
     */
    public DeserializerChannelProcessor() {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method may read more bytes from the
     * specified channel than necessary to read the first object.
     */
    @Override
    public void processChannel(ReadableByteChannel channel,
            AsyncDataListener<Object> listener,
            StateListener stateListener) throws IOException {
        ObjectInputStream input;
        input = new ObjectInputStream(Channels.newInputStream(channel));

        try {
            Object data = input.readObject();
            listener.onDataArrive(data);
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex);
        }

        stateListener.setState(new SimpleDataState("Finished deserialzing the object.", 1.0));
    }
}
