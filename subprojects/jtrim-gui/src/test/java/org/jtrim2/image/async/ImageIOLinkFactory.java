package org.jtrim2.image.async;

import java.nio.file.Path;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.image.ImageResult;

public interface ImageIOLinkFactory {
    public AsyncDataLink<ImageResult> createLink(Path file, TaskExecutor executor);
}
