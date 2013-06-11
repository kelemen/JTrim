package org.jtrim.image.async;

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
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.DelegatedAsyncDataController;
import org.jtrim.concurrent.async.SimpleDataController;
import org.jtrim.concurrent.async.SimpleDataState;
import org.jtrim.image.BufferedImages;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageResult;
import org.jtrim.image.JavaIIOMetaData;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.TimeDuration;

/**
 *
 * @author Kelemen Attila
 */
public final class InputStreamImageLink implements AsyncDataLink<ImageResult> {
    private static final AsyncDataState FIRST_STATE
            = new SimpleDataState("Image loading is in progress.", 0.0);

    private static final AsyncDataState DONE_STATE
            = new SimpleDataState("Image was successfully loaded.", 1.0);

    private final TaskExecutor executor;
    private final InputStreamOpener streamOpener;
    private final TimeDuration minUpdateTime;

    public InputStreamImageLink(
            TaskExecutor executor,
            InputStreamOpener streamOpener,
            TimeDuration minUpdateTime) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(streamOpener, "streamOpener");
        ExceptionHelper.checkNotNullArgument(minUpdateTime, "minUpdateTime");

        this.executor = executor;
        this.streamOpener = streamOpener;
        this.minUpdateTime = minUpdateTime;
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

        listener.onDataArrive(new ImageResult(null, incompleteMetaData));

        cancelToken.checkCanceled();

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

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            final AsyncDataListener<? super ImageResult> dataListener) {

        final SimpleDataController controller = new SimpleDataController(FIRST_STATE);

        executor.execute(cancelToken, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                fetchImage(cancelToken, dataListener, controller);
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
            }
        });

        return new DelegatedAsyncDataController(controller);
    }

    private class PartialImageForwarder extends IIOReadUpdateAdapter {
        private final ImageMetaData incompleteMetaData;
        private final AsyncDataListener<? super ImageResult> listener;
        private long lastUpdateTime;

        public PartialImageForwarder(
                ImageMetaData incompleteMetaData,
                AsyncDataListener<? super ImageResult> listener) {
            this.incompleteMetaData = incompleteMetaData;
            this.listener = listener;
            this.lastUpdateTime = System.nanoTime();
        }

        private void imageUpdate(BufferedImage image) {
            long currentTime = System.nanoTime();
            long elapsed = currentTime - lastUpdateTime;
            if (elapsed >= minUpdateTime.toNanos()) {
                BufferedImage newImage = BufferedImages.createNewAcceleratedBuffer(image);
                ImageResult data = new ImageResult(newImage, incompleteMetaData);
                listener.onDataArrive(data);
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
