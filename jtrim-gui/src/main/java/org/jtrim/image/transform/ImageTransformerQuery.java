package org.jtrim.image.transform;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.*;

/**
 * Defines an {@link AsyncDataQuery} which transforms its input image on the
 * specified {@link TaskExecutorService}. There can be multiple transformations
 * defined, each defining a more and more accurate transformation. That is,
 * {@code ImageTransformerLink} applies each transformation to its input and
 * forwards each transformed image assuming that the last transformation applied
 * is the most accurate one.
 * <P>
 * Note that essentially each transformation defines the same transformation,
 * they should only differ in the accuracy of their result.
 * <P>
 * This class is effectively a factory for {@link ImageTransformerLink}
 * instances. That is, the {@link #createDataLink(ImageTransformerData) createDataLink}
 * method creates an {@code ImageTransformerLink} with the arguments specified
 * at construction time.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see org.jtrim.swing.component.AsyncImageDisplay
 * @see ImageTransformerLink
 *
 * @author Kelemen Attila
 */
public final class ImageTransformerQuery
implements
        AsyncDataQuery<ImageTransformerData, TransformedImageData> {

    private final List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> imageTransformers;

    /**
     * Creates a new {@code ImageTransformerQuery} which will execute the image
     * transformations on the specified executor.
     *
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
    public ImageTransformerQuery(TaskExecutorService executor, ImageTransformer... imageTransformers) {
        List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> taskList;
        taskList = new ArrayList<>(imageTransformers.length);

        for (ImageTransformer transformer: imageTransformers) {
            DataConverter<ImageTransformerData, TransformedImageData> converter;
            converter = new ImageConverter(transformer);
            taskList.add(new AsyncDataConverter<>(converter, executor));
        }

        this.imageTransformers = taskList;
    }

    /**
     * Creates a new {@code ImageTransformerQuery} applying the specified image
     * transformations on input images. The transformations are executed on
     * the associated {@code TaskExecutorService}.
     *
     * @param imageTransformers the transformations (with their associated
     *   {@code TaskExecutorService}) to be applied to the input image. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   elements.
     *
     * @throws NullPointerException thrown if the specified image transformation
     *   list is {@code null} or contains {@code null} elements
     */
    public ImageTransformerQuery(
            List<AsyncDataConverter<ImageTransformerData, TransformedImage>> imageTransformers) {

        this.imageTransformers = new ArrayList<>(imageTransformers.size());
        for (AsyncDataConverter<ImageTransformerData, TransformedImage> converter: imageTransformers) {

            this.imageTransformers.add(new AsyncDataConverter<>(
                    new ImageConverter(converter.getConverter()),
                    converter.getExecutor()
                    ));
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ImageTransformerLink createDataLink(ImageTransformerData arg) {
        return ImageTransformerLink.createFromDataTransformers(
                arg, imageTransformers);
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
        String imageTransformersStr
                = AsyncFormatHelper.collectionToString(imageTransformers);

        StringBuilder result = new StringBuilder();
        result.append("Tranform images gradually using ");
        AsyncFormatHelper.appendIndented(imageTransformersStr, result);

        return result.toString();

    }
}
