package org.jtrim2.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * Defines an interface to open a new {@link Channel} for processing. This
 * interface must open a new channel each new request.
 * <P>
 * This interface was designed for the {@link AsyncChannelLink} and so to
 * preserve the contract of {@code AsyncDataLink} instances of
 * {@code ChannelOpener} must always open the channel to the same source
 * (e.g.: to the same file) whenever requested.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface must be safe to be accessed by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <ChannelType> the type of the channel opened by the
 *   {@code ChannelOpener}
 *
 * @see AsyncChannelLink
 *
 * @author Kelemen Attila
 */
public interface ChannelOpener<ChannelType extends Channel> {
    /**
     * Opens a new channel to the source defined by this
     * {@code ChannelOpener} instance. This method opens a new channel each
     * time called an these opened channels must be
     * {@link Channel#close() closed} separately.
     *
     * @return new channel to the source defined by this
     *   {@code ChannelOpener} instance. The returned instance must be
     *   {@link Channel#close() closed} in order to prevent resource leakage.
     *   This method never returns {@code null}.
     *
     * @throws IOException thrown if the channel could not be opened for some
     *   reasons
     */
    public ChannelType openChanel() throws IOException;
}
