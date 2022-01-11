package org.jtrim2.concurrent.query.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Defines a {@code ChannelOpener} which opens a specific file. The file is
 * always opened for reading only. The file must exists when requested to be
 * opened otherwise {@code FileChannelOpener} will fail to open the file.
 *
 * <h2>Thread safety</h2>
 * As required by the {@code ChannelOpener}, this class is safe to be accessed
 * by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @see AsyncChannelLink
 */
public final class FileChannelOpener implements ChannelOpener<FileChannel> {
    private static final Set<StandardOpenOption> OPEN_OPTIONS
            = Collections.singleton(StandardOpenOption.READ);
    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute<?>[0];

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
        Objects.requireNonNull(fileToOpen, "fileToOpen");
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
