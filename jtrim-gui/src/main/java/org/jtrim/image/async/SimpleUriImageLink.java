package org.jtrim.image.async;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.*;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageReceiveException;
import org.jtrim.image.JavaIIOMetaData;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@code AsyncDataLink} which is able to retrieve an image based on
 * an {@link URI}. To actually retrieve the image file from the external source,
 * the {@link URL} class is used, therefore {@code SimpleUriImageLink} is able
 * to retrieve any image which the {@code URL} class can. To load the image, the
 * {@code ImageIO} library of Java is used and therefore the meta data of the
 * retrieved image is a {@link JavaIIOMetaData}.
 * <P>
 * The {@code SimpleUriImageLink} is able to retrieve partially retrieved image
 * and even the meta data without the image until the complete image is
 * available.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see SimpleUriImageQuery
 *
 * @author Kelemen Attila
 */
public final class SimpleUriImageLink implements AsyncDataLink<ImageData> {
    private static final AsyncDataState FIRST_STATE
            = new SimpleDataState("Image loading is in progress.", 0.0);

    private static final AsyncDataState DONE_STATE
            = new SimpleDataState("Image was successfully loaded.", 1.0);

    private final TaskExecutor executor;
    private final URI imageUri;
    private final long minUpdateTime; // nanoseconds

    /**
     * Creates the {@code SimpleUriImageLink} with the specified {@code URI}
     * defining the image to be retrieved.
     *
     * @param imageUri the {@code URI} of the image to be retrieved. This
     *   argument cannot be {@code null}.
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
     * @throws NullPointerException thrown if either the {@code URI} or the
     *   executor is {@code null}
     * @throws IllegalArgumentException thrown if the specified
     *   {@code minUpdateTime} is less than zero
     */
    public SimpleUriImageLink(URI imageUri,
            TaskExecutor executor, long minUpdateTime) {

        ExceptionHelper.checkNotNullArgument(imageUri, "imageUri");
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkArgumentInRange(minUpdateTime, 0, Long.MAX_VALUE, "minUpdateTime");

        this.executor = executor;
        this.imageUri = imageUri;
        this.minUpdateTime = minUpdateTime;
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
            AsyncDataListener<? super ImageData> dataListener) {

        DataStateHolder dataState = new DataStateHolder(FIRST_STATE);

        ImageReaderTask task = new ImageReaderTask(imageUri, minUpdateTime,
                dataState, dataListener);

        executor.execute(cancelToken, task, null);

        return new ImageReaderController(dataState);
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
        result.append(imageUri);
        result.append("]\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);
        result.append(")");

        return result.toString();
    }

    private static class PartialImageUpdater extends IIOReadUpdateAdapter {
        private final AsyncDataListener<ImageData> safeListener;
        private final ImageMetaData metaData;
        private final long minUpdateTime;

        private volatile boolean wasUpdated;
        private volatile long lastUpdateTime;

        public PartialImageUpdater(
                AsyncDataListener<ImageData> safeListener,
                ImageMetaData metaData,
                long minUpdateTime) {

            this.safeListener = safeListener;
            this.metaData = metaData;
            this.minUpdateTime = minUpdateTime;

            this.wasUpdated = false;
            this.lastUpdateTime = 0;
        }

        private void imageUpdate(BufferedImage theImage) {
            boolean needUpdate;
            long currentTime = System.nanoTime();

            if (wasUpdated) {
                long timeSinceUpdate;
                timeSinceUpdate = currentTime - lastUpdateTime;
                needUpdate = timeSinceUpdate >= minUpdateTime;
            }
            else {
                needUpdate = true;
            }


            if (needUpdate && safeListener.requireData()) {
                BufferedImage imageCopy;
                imageCopy = ImageData.createNewAcceleratedBuffer(theImage);

                ImageData data = new ImageData(imageCopy, metaData, null);
                safeListener.onDataArrive(data);

                lastUpdateTime = currentTime;
                wasUpdated = true;
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

    private static class DataStateHolder {
        private AtomicReference<AsyncDataState> dataState;

        public DataStateHolder(AsyncDataState dataState) {
            assert dataState != null;

            this.dataState = new AtomicReference<>(dataState);
        }

        public AsyncDataState getDataState() {
            return dataState.get();
        }

        public void setDataState(AsyncDataState newState) {
            assert newState != null;

            AsyncDataState oldState;
            do {
                oldState = dataState.get();
                if (oldState.getProgress() > newState.getProgress()) {
                    return;
                }
            } while (!dataState.compareAndSet(oldState, newState));
        }
    }

    private static class ImageProgressListener extends IIOReadProgressAdapter {

        private final AtomicBoolean abortedState;
        private final DataStateHolder dataState;

        private boolean aborted;
        private double lastProgressPercent;

        public ImageProgressListener(AtomicBoolean abortedState, DataStateHolder dataState) {
            this.abortedState = abortedState;
            this.dataState = dataState;
            this.aborted = false;
            this.lastProgressPercent = 0.0;
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            if (aborted || abortedState.get()) {
                source.abort();
            }

            if (Math.floor(lastProgressPercent) < Math.floor(percentageDone)) {
                dataState.setDataState(new SimpleDataState("Image is loading.", percentageDone / 100.0));
            }
        }

        @Override
        public void readAborted(ImageReader source) {
            aborted = true;
        }
    }

    private static class ImageWithMetaData {
        public final BufferedImage image;
        public final ImageMetaData metaData;

        public ImageWithMetaData(BufferedImage image, ImageMetaData metaData) {
            this.image = image;
            this.metaData = metaData;
        }
    }

    private static class ImageReaderTask implements CancelableTask {
        private final URI imageUri;
        private final long minUpdateTime;
        private final DataStateHolder dataState;
        private final AtomicBoolean abortedState;
        private final AsyncDataListener<ImageData> safeListener;

        public ImageReaderTask(
                URI imageUri,
                long minUpdateTime,
                DataStateHolder dataState,
                AsyncDataListener<? super ImageData> dataListener) {

            this.imageUri = imageUri;
            this.minUpdateTime = minUpdateTime;
            this.dataState = dataState;
            this.abortedState = new AtomicBoolean(false);
            this.safeListener = AsyncHelper.makeSafeListener(dataListener);
        }

        private ImageWithMetaData readImage(
                ImageInputStream stream,
                DataStateHolder dataState,
                AtomicBoolean abortedState,
                AsyncDataListener<ImageData> safeListener) throws IOException {

            BufferedImage rawImage;
            ImageMetaData lastMetaData = null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }

            ImageReader reader = readers.next();
            try {
                reader.addIIOReadProgressListener(
                        new ImageProgressListener(abortedState, dataState));

                reader.setInput(stream, true, false);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width > 0 && height > 0) {
                    lastMetaData = new JavaIIOMetaData(width, height, null, true);
                    safeListener.onDataArrive(new ImageData(null, lastMetaData, null));
                }

                if (width > 0 && height > 0) {
                    IIOMetadata rawMetadata = reader.getImageMetadata(0);
                    lastMetaData = new JavaIIOMetaData(width, height, rawMetadata, true);
                    safeListener.onDataArrive(new ImageData(null, lastMetaData, null));
                }

                if (lastMetaData != null && minUpdateTime >= 0) {
                    final ImageMetaData metaData = lastMetaData;

                    reader.addIIOReadUpdateListener(
                            new PartialImageUpdater(safeListener, metaData, minUpdateTime));
                }

                if (abortedState.get()) {
                    reader.abort();
                }

                ImageReadParam readParam = reader.getDefaultReadParam();
                rawImage = reader.read(0, readParam);
            } finally {
                reader.dispose();
            }

            return new ImageWithMetaData(rawImage, lastMetaData);
        }

        private void retrieveImage() {
            AsyncReport report = null;
            BufferedImage lastImage = null;
            ImageMetaData lastMetaData = null;

            try {
                BufferedImage rawImage;
                URL imageUrl = imageUri.toURL();
                try (InputStream urlStream = imageUrl.openStream()) {
                    try (ImageInputStream stream
                            = ImageIO.createImageInputStream(urlStream)) {

                        ImageWithMetaData readResult;
                        readResult = readImage(stream,
                                dataState,
                                abortedState,
                                safeListener);

                        rawImage = readResult.image;
                        lastMetaData = readResult.metaData;
                    }
                }

                lastImage = !abortedState.get()
                        ? ImageData.createAcceleratedBuffer(rawImage)
                        : null;

                // The JVM would think this to be reachable
                // until the method returns or some other argument
                // overwrites it (but the latter case is unreliable).
                rawImage = null;

                ImageData result;

                if (lastImage != null) {
                    dataState.setDataState(DONE_STATE);
                    if (lastMetaData == null) {
                        lastMetaData = new JavaIIOMetaData(
                                lastImage.getWidth(), lastImage.getHeight(),
                                null, true);
                    }

                    result = new ImageData(lastImage, lastMetaData, null);
                }
                else {
                    ImageReceiveException ex = new ImageReceiveException(
                            "Image could not be retrieved.");

                    result = new ImageData(null, null, ex);
                }

                safeListener.onDataArrive(result);
                report = AsyncReport.getReport(null, abortedState.get());
            } catch (IOException ex) {
                ImageData imageData = new ImageData(lastImage, lastMetaData,
                        new ImageReceiveException(ex));

                safeListener.onDataArrive(imageData);
                report = AsyncReport.getReport(ex, abortedState.get());
            } catch (Throwable ex) {
                report = AsyncReport.getReport(ex, abortedState.get());
                throw ex;
            } finally {
                assert report != null;
                safeListener.onDoneReceive(report);
            }
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            ListenerRef cancelRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    abortedState.set(true);
                    safeListener.onDoneReceive(AsyncReport.CANCELED);
                }
            });
            try {
                retrieveImage();
            } finally {
                cancelRef.unregister();
            }
        }
    }

    private static class ImageReaderController implements AsyncDataController {
        private final DataStateHolder dataState;

        public ImageReaderController(DataStateHolder dataState) {
            this.dataState = dataState;
        }

        @Override
        public AsyncDataState getDataState() {
            return dataState.getDataState();
        }

        @Override
        public void controlData(Object controlArg) {
        }
    }
}
