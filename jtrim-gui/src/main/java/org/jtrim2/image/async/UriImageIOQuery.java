package org.jtrim2.image.async;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.TaskExecutor;
import org.jtrim2.concurrent.async.AsyncDataLink;
import org.jtrim2.concurrent.async.AsyncDataQuery;
import org.jtrim2.concurrent.async.AsyncFormatHelper;
import org.jtrim2.concurrent.async.io.InputStreamOpener;
import org.jtrim2.image.ImageResult;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines an {@code AsyncDataQuery} which is able to retrieve images based on
 * an {@code URI}. The {@code URI} is specified as an input to the query. To
 * actually retrieve the image file from the external source, the {@code URL}
 * class is used, therefore {@code UriImageIOQuery} is able to retrieve any image
 * which the {@code URL} class can. To load the image, the {@code ImageIO}
 * library of Java is used and therefore the meta data of the retrieved image is
 * a {@link org.jtrim2.image.JavaIIOMetaData}.
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
public final class UriImageIOQuery implements AsyncDataQuery<URI, ImageResult> {
    private final TaskExecutor executor;
    private final double allowedIntermediateRatio;

    /**
     * Creates the {@code UriImageIOQuery} with the given properties.
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
     */
    public UriImageIOQuery(TaskExecutor executor, double allowedIntermediateRatio) {
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

    /**
     * Returns the string representation of this {@code AsyncDataQuery} in no
     * particular format
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Query images by URI. Allowed time to be spent on intermediate values: ");
        result.append(100.0 * allowedIntermediateRatio);
        result.append("%");
        result.append("\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);

        return result.toString();
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
