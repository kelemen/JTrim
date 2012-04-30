package org.jtrim.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a {@code ChannelOpener} which opens a specific file. The file is
 * always opened for reading only. The file must exists when requested to be
 * opened otherwise {@code FileChannelOpener} will fail to open the file.
 *
 * <h3>Thread safety</h3>
 * As required by the {@code ChannelOpener}, this class is safe to be accessed
 * by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @see AsyncChannelLink
 *
 * @author Kelemen Attila
 */
public final class FileChannelOpener implements ChannelOpener<FileChannel> {
    private static final Set<StandardOpenOption> OPEN_OPTIONS
            = Collections.singleton(StandardOpenOption.READ);
    private static FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute<?>[0];

    private final Path fileToOpen;

    /**
     * Creates a {@code FileChannelOpener} which will open the specified file
     * when requested.
     *
     * @param fileToOpen the file to open when requested. This argument cannot
     *   be {@code null}
     *
     * @throws NullPointerException thrown if the specified path is {@code null}
     */
    public FileChannelOpener(Path fileToOpen) {
        ExceptionHelper.checkNotNullArgument(fileToOpen, "fileToOpen");
        this.fileToOpen = fileToOpen;
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: The file channel will be opened for reading
     * only. That is, with the {@code StandardOpenOption.READ}.
     */
    @Override
    public FileChannel openChanel() throws IOException {
        return FileChannel.open(fileToOpen, OPEN_OPTIONS, NO_ATTRIBUTES);
    }
}
