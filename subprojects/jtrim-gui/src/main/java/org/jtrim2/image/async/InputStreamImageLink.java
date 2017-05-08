package org.jtrim2.image.async;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.async.AsyncDataController;
import org.jtrim2.concurrent.async.AsyncDataLink;
import org.jtrim2.concurrent.async.AsyncDataListener;
import org.jtrim2.concurrent.async.AsyncDataState;
import org.jtrim2.concurrent.async.AsyncFormatHelper;
import org.jtrim2.concurrent.async.AsyncReport;
import org.jtrim2.concurrent.async.DelegatedAsyncDataController;
import org.jtrim2.concurrent.async.SimpleDataController;
import org.jtrim2.concurrent.async.SimpleDataState;
import org.jtrim2.concurrent.async.io.InputStreamOpener;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.image.BufferedImages;
import org.jtrim2.image.ImageMetaData;
import org.jtrim2.image.ImageResult;
import org.jtrim2.image.JavaIIOMetaData;

/**
 * Defines an {@code AsyncDataLink} providing images read from an
 * {@link InputStream}. The {@code InputStream} is provided by a
 * {@link InputStreamOpener}. To load the image, the {@code ImageIO} library of
 * Java is used and therefore the meta data of the retrieved image is a
 * {@link JavaIIOMetaData}.
 * <P>
 * The {@code InputStreamImageLink} is able to retrieve partially retrieved
 * image and even the meta data without the image until the complete image is
 * available. This however carries a certain amount of overhead because the
 * partial image needs to be copied before being published. Therefore, users
 * of {@code InputStreamImageLink} has to specify how much overhead they can
 * tolerate, in order to display partially read images. The overhead is
 * specified as a percentage of the time of the whole image reading process.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see UriImageIOQuery
 */
public final class InputStreamImageLink implements AsyncDataLink<ImageResult> {
    private static final AsyncDataState FIRST_STATE
            = new SimpleDataState("Image loading is in progress.", 0.0);

    private static final AsyncDataState DONE_STATE
            = new SimpleDataState("Image was successfully loaded.", 1.0);

    private final TaskExecutor executor;
    private final InputStreamOpener streamOpener;
    private final double allowedIntermediateRatio;

    /**
     * Creates a new {@code InputStreamImageLink} which reads the image read
     * from the specified {@code InputStreamOpener} on the given
     * {@code TaskExecutor}.
     *
     * @param executor the {@code TaskExecutor} on which the image will be read
     *   from the input stream. This argument cannot be {@code null}.
     * @param streamOpener the {@code InputStreamOpener} providing the image to
     *   be loaded. This argument cannot be {@code null}.
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
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public InputStreamImageLink(
            TaskExecutor executor,
            InputStreamOpener streamOpener,
            double allowedIntermediateRatio) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(streamOpener, "streamOpener");

        this.executor = executor;
        this.streamOpener = streamOpener;
        this.allowedIntermediateRatio = allowedIntermediateRatio;
    }

    private void fetchImage(
            final CancellationToken cancelToken,
            final ImageReader reader,
            final AsyncDataListener<? super ImageResult> listener,
            final SimpleDataController controller) throws IOException  {

        final AtomicBoolean aborted = new AtomicBoolean(false);
        reader.addIIOReadProgressListener(new IIOReadProgressAdapter() {
            private float lastProgressPercentage = 0.0f;

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                if (cancelToken.isCanceled()) {
                    aborted.set(true);
                    source.abort();
                }

                if (lastProgressPercentage < percentageDone) {
                    lastProgressPercentage = percentageDone;
                    controller.setDataState(new SimpleDataState("Retrieving", percentageDone / 100.0f));
                }
            }
        });

        int width = reader.getWidth(0);
        int height = reader.getHeight(0);

        cancelToken.checkCanceled();

        final IIOMetadata rawMetaData = reader.getImageMetadata(0);
        final ImageMetaData incompleteMetaData
                = new JavaIIOMetaData(width, height, rawMetaData, false);

        cancelToken.checkCanceled();
        listener.onDataArrive(new ImageResult(null, incompleteMetaData));

        reader.addIIOReadUpdateListener(new PartialImageForwarder(incompleteMetaData, listener));

        ImageReadParam readParam = reader.getDefaultReadParam();
        BufferedImage rawImage = reader.read(0, readParam);
        if (aborted.get()) {
            throw new OperationCanceledException();
        }
        if (rawImage == null) {
            throw new IOException("ImageIO read null for the image.");
        }

        rawImage = BufferedImages.createAcceleratedBuffer(rawImage);
        final ImageMetaData completeMetaData = new JavaIIOMetaData(width, height, rawMetaData, true);
        ImageResult finalResult = new ImageResult(rawImage, completeMetaData);
        listener.onDataArrive(finalResult);
        controller.setDataState(DONE_STATE);
    }

    private void fetchImage(
            CancellationToken cancelToken,
            ImageInputStream stream,
            AsyncDataListener<? super ImageResult> dataListener,
            SimpleDataController controller) throws IOException  {

        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
        if (!readers.hasNext()) {
            throw new IOException("Unsupported image format");
        }

        ImageReader reader = readers.next();
        try {
            reader.setInput(stream, true, false);
            fetchImage(cancelToken, reader, dataListener, controller);
        } finally {
            reader.dispose();
        }
    }

    private void fetchImage(
            CancellationToken cancelToken,
            AsyncDataListener<? super ImageResult> dataListener,
            SimpleDataController controller) throws IOException  {

        try (InputStream stream = streamOpener.openStream(cancelToken);
                ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            Objects.requireNonNull(imageStream, "imageStream");
            fetchImage(cancelToken, imageStream, dataListener, controller);
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method will submit a task to retrieve
     * the image to the executor specified at construction time.
     */
    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            final AsyncDataListener<? super ImageResult> dataListener) {

        final SimpleDataController controller = new SimpleDataController(FIRST_STATE);

        executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            fetchImage(taskCancelToken, dataListener, controller);
        }, (boolean canceled, Throwable error) -> {
            dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
        });

        return new DelegatedAsyncDataController(controller);
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
        StringBuilder result = new StringBuilder();
        result.append("Image[");
        result.append(streamOpener);
        result.append("]\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);
        result.append(")");

        return result.toString();
    }

    private class PartialImageForwarder extends IIOReadUpdateAdapter {
        private final ImageMetaData incompleteMetaData;
        private final AsyncDataListener<? super ImageResult> listener;
        private final long startTime;
        private long nanosSpentOnIntermediate;

        public PartialImageForwarder(
                ImageMetaData incompleteMetaData,
                AsyncDataListener<? super ImageResult> listener) {
            this.incompleteMetaData = incompleteMetaData;
            this.listener = listener;
            this.startTime = System.nanoTime();
            this.nanosSpentOnIntermediate = 0;
        }

        private void imageUpdate(BufferedImage image) {
            long currentTime = System.nanoTime();
            long elapsedNanos = currentTime - startTime;
            long maxIntermediateNanos = Math.round(elapsedNanos * allowedIntermediateRatio);
            if (maxIntermediateNanos > nanosSpentOnIntermediate) {
                try {
                    BufferedImage newImage = BufferedImages.createNewAcceleratedBuffer(image);
                    ImageResult data = new ImageResult(newImage, incompleteMetaData);
                    listener.onDataArrive(data);
                } finally {
                    nanosSpentOnIntermediate += System.nanoTime() - currentTime;
                }
            }
        }

        @Override
        public void imageUpdate(ImageReader source,
                BufferedImage theImage,
                int minX, int minY, int width, int height,
                int periodX, int periodY, int[] bands) {

            imageUpdate(theImage);
        }

        @Override
        public void passComplete(ImageReader source,
                BufferedImage theImage) {

            imageUpdate(theImage);
        }
    }
}
