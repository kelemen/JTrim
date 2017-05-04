package org.jtrim2.image.async;

import java.nio.file.Path;
import org.jtrim2.concurrent.async.AsyncDataLink;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.image.ImageResult;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageIOLinkFactory {
    public AsyncDataLink<ImageResult> createLink(Path file, TaskExecutor executor);
}
