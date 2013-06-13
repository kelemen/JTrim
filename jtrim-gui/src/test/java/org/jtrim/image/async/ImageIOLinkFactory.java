package org.jtrim.image.async;

import java.nio.file.Path;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.image.ImageResult;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageIOLinkFactory {
    public AsyncDataLink<ImageResult> createLink(Path file, TaskExecutor executor);
}
