package org.jtrim.image.transform;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@link AsyncDataLink} which transforms an image on the specified
 * {@link TaskExecutorService}. There can be multiple transformations defined,
 * each defining a more and more accurate transformation. That is,
 * {@code ImageTransformerLink} applies each transformation to its input and
 * forwards each transformed image assuming that the last transformation applied
 * is the most accurate one.
 * <P>
 * Note that essentially each transformation defines the same transformation,
 * they should only differ in the accuracy of their result.
 * <P>
 * This class works similar to the {@code AsyncDataLink} instances created by
 * the {@link AsyncLinks#convertGradually(Object, List) AsyncLinks.convertGradually}
 * method. The only difference is that {@code ImageTransformerLink} will convert
 * the result of the transformation ({@link TransformedImage}) to
 * {@link TransformedImageData}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see org.jtrim.swing.component.AsyncImageDisplay
 * @see ImageTransformerQuery
 */
public final class ImageTransformerLink implements AsyncDataLink<TransformedImageData> {
    private final AsyncDataLink<TransformedImageData> wrappedLink;

    static ImageTransformerLink createFromDataTransformers(
            ImageTransformerData input,
            List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> imageTransformers) {

        return new ImageTransformerLink(
                AsyncLinks.convertGradually(input, imageTransformers));
    }

    /**
     * Creates a new {@code ImageTransformerLink} which will execute the image
     * transformations on the specified {@link TaskExecutorService}.
     *
     * @param input the input to be transformed by {@code ImageTransformerLink}.
     *   This argument can be {@code null} if the image transformers accept
     *   {@code null} values.
     * @param executor the {@code TaskExecutorService} on which the image
     *   transformers are called. Each image transformation is called in a
     *   separate task submitted to this executor. This argument cannot be
     *   {@code null}.
     * @param imageTransformers the transformations to be applied to the input
     *   image. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     *
     * @throws NullPointerException throw if the specified executor or the image
     *   transformation array or any of its transformation is {@code null}
     */
    public ImageTransformerLink(ImageTransformerData input,
            TaskExecutorService executor, ImageTransformer... imageTransformers) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> taskList;
        taskList = new ArrayList<>(imageTransformers.length);

        for (ImageTransformer transformer: imageTransformers) {
            DataConverter<ImageTransformerData, TransformedImageData> converter;
            converter = new ImageConverter(transformer);
            taskList.add(new AsyncDataConverter<>(converter, executor));
        }

        wrappedLink = AsyncLinks.convertGradually(input, taskList);
    }

    /**
     * Creates a new {@code ImageTransformerLink} applying the specified image
     * transformations on the input image. The transformations are executed on
     * the associated {@code TaskExecutorService}.
     *
     * @param input the input to be transformed by {@code ImageTransformerLink}.
     *   This argument can be {@code null} if the image transformers accept
     *   {@code null} values.
     * @param imageTransformers the transformations (with their associated
     *   {@code TaskExecutorService}) to be applied to the input image. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   elements.
     *
     * @throws NullPointerException thrown if the specified image transformation
     *   list is {@code null} or contains {@code null} elements
     */
    public ImageTransformerLink(ImageTransformerData input,
            List<AsyncDataConverter<ImageTransformerData, TransformedImage>> imageTransformers) {

        List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> imageDataTransformers;
        imageDataTransformers = new ArrayList<>(imageTransformers.size());
        for (AsyncDataConverter<ImageTransformerData, TransformedImage> converter: imageTransformers) {

            imageDataTransformers.add(new AsyncDataConverter<>(
                    new ImageConverter(converter.getConverter()),
                    converter.getExecutor()
                    ));
        }

        wrappedLink = AsyncLinks.convertGradually(input, imageDataTransformers);
    }

    private ImageTransformerLink(AsyncDataLink<TransformedImageData> wrappedLink) {
        this.wrappedLink = wrappedLink;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super TransformedImageData> dataListener) {
        return wrappedLink.getData(cancelToken, dataListener);
    }

    /**
     * Returns the string representation of this {@code AsyncDataLink} in no
     * particular format
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return wrappedLink.toString();
    }
}
