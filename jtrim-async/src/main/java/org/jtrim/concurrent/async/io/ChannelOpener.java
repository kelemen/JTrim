package org.jtrim.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 *
 * @author Kelemen Attila
 */
public interface ChannelOpener<ChannelType extends Channel> {
    public ChannelType openChanel() throws IOException;
}
