package org.jtrim2.image.async;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.jtrim2.concurrent.async.AsyncDataQuery;
import org.jtrim2.concurrent.async.AsyncFormatHelper;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @deprecated Use {@link UriImageIOQuery} instead.
 *
 * Defines an {@code AsyncDataQuery} which is able to retrieve images based on
 * an {@code URI}. The {@code URI} is specified as an input for the query. To
 * actually retrieve the image file from the external source, the {@code URL}
 * class is used, therefore {@code SimpleUriImageLink} is able to retrieve any
 * image which the {@code URL} class can. To load the image, the {@code ImageIO}
 * library of Java is used and therefore the meta data of the retrieved image is
 * a {@link org.jtrim2.image.JavaIIOMetaData}.
 * <P>
 * This implementation completely relies on the {@link SimpleUriImageLink} to
 * retrieve images.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see SimpleUriImageLink
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class SimpleUriImageQuery
implements
        AsyncDataQuery<URI, org.jtrim2.image.ImageData> {

    private final TaskExecutor executor;
    private final long minUpdateTime; // nanoseconds

    /**
     * Creates the {@code SimpleUriImageQuery} with the given properties.
     *
     * @param executor the executor used to actually retrieve the image. That
     *   is, the image is retrieved in a task submitted to this executor.
     *   This argument cannot be {@code null}.
     * @param minUpdateTime the minimum time in nanoseconds which must elapse
     *   between providing partially complete images to the
     *   {@code AsyncDataListener}. Note that to actually forward an image to
     *   the {@code AsyncDataListener} requires the {@code BufferedImage} to be
     *   copied (and while copying the loading of the image is suspended).
     *   Therefore providing an intermediate image is a considerable overhead,
     *   so it is important not to set this value too low. This argument must be
     *   greater than or equal to zero.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified
     *   {@code minUpdateTime} is less than zero
     */
    public SimpleUriImageQuery(TaskExecutor executor, long minUpdateTime) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkArgumentInRange(minUpdateTime, 0, Long.MAX_VALUE, "minUpdateTime");

        this.executor = executor;
        this.minUpdateTime = minUpdateTime;
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This implementation only creates and returns
     * a new {@link SimpleUriImageLink} instance.
     */
    @Override
    public SimpleUriImageLink createDataLink(URI arg) {
        return new SimpleUriImageLink(arg, executor, minUpdateTime);
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
        result.append("Query images by URI. MinUpdateTime: ");
        result.append(TimeUnit.NANOSECONDS.toMillis(minUpdateTime));
        result.append(" ms\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);

        return result.toString();
    }


}
