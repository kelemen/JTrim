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
 * Defines an {@code AsyncDataQuery} which is able to retrieve images based on
 * an {@code URI}. The {@code URI} is specified as an input to the query. To
 * actually retrieve the image file from the external source, the {@code URL}
 * class is used, therefore {@code UriImageQuery} is able to retrieve any image
 * which the {@code URL} class can. To load the image, the {@code ImageIO}
 * library of Java is used and therefore the meta data of the retrieved image is
 * a {@link org.jtrim.image.JavaIIOMetaData}.
 * <P>
 * This implementation completely relies on the {@link InputStreamImageLink} to
 * retrieve images.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see InputStreamImageLink
 *
 * @author Kelemen Attila
 */
public final class UriImageQuery implements AsyncDataQuery<URI, ImageResult> {
    private final TaskExecutor executor;
    private final double allowedIntermediateRatio;

    /**
     * Creates the {@code UriImageQuery} with the given properties.
     *
     * @param executor the executor used to actually retrieve the image. That
     *   is, the image is retrieved in a task submitted to this executor.
     *   This argument cannot be {@code null}.
     * @param allowedIntermediateRatio defines the approximate ratio
     *   (percentage divided by 100) of the overhead to be tolerated in order to
     *   provide intermediate images. Providing too high value for this argument
     *   might decrease performance considerably. That is, the overhead defined
     *   by this argument is likely to come true. For example: Specifying 0.5
     *   for this argument will likely increase the time needed to read the
     *   complete image to twice the needed amount. This argument should be
     *   between {@code 0.0} and {@code 1.0} but the actual value of this
     *   argument is not verified. Specifying less than zero is
     *   equivalent to specifying zero, while specifying more than one, is
     *   equivalent to specifying one.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified
     *   {@code minUpdateTime} is less than zero
     */
    public UriImageQuery(TaskExecutor executor, double allowedIntermediateRatio) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
        this.allowedIntermediateRatio = allowedIntermediateRatio;
    }

    /**
     * {@inheritDoc }
     *
     * @throws IllegalArgumentException thrown if the specified {@code URI} is
     *   not absolute
     * @throws NullPointerException thrown if the specified {@code URI} is
     *   {@code null}
     */
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
