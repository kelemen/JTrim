package org.jtrim.image.async;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.io.InputStreamOpener;
import org.jtrim.image.ImageResult;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class UriImageQuery implements AsyncDataQuery<URI, ImageResult> {
    private final TaskExecutor executor;
    private final double allowedIntermediateRatio;

    public UriImageQuery(TaskExecutor executor, double allowedIntermediateRatio) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
        this.allowedIntermediateRatio = allowedIntermediateRatio;
    }

    @Override
    public AsyncDataLink<ImageResult> createDataLink(URI arg) {
        ExceptionHelper.checkNotNullArgument(arg, "arg");
        if (!arg.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }

        return new InputStreamImageLink(executor, new URLStreamOpener(arg), allowedIntermediateRatio);
    }

    private static final class URLStreamOpener implements InputStreamOpener {
        private final URI uri;

        public URLStreamOpener(URI uri) {
            this.uri = uri;
        }

        @Override
        public InputStream openStream(CancellationToken cancelToken) throws IOException {
            return uri.toURL().openStream();
        }
    }
}
