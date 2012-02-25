/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.async;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.jtrim.concurrent.async.*;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageReceiveException;
import org.jtrim.image.JavaIIOMetaData;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class SimpleUriImageLink implements AsyncDataLink<ImageData> {
    private static final AsyncDataState FIRST_STATE
            = new SimpleDataState("Image loading is in progress.", 0.0);

    private static final AsyncDataState DONE_STATE
            = new SimpleDataState("Image was successfully loaded.", 1.0);

    private final ExecutorService executor;
    private final URI imageUri;
    private final long minUpdateTime; // nanoseconds

    public SimpleUriImageLink(URI imageUri,
            ExecutorService executor, long minUpdateTime) {

        ExceptionHelper.checkNotNullArgument(imageUri, "imageUri");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
        this.imageUri = imageUri;
        this.minUpdateTime = minUpdateTime;
    }

    @Override
    public AsyncDataController getData(
            AsyncDataListener<? super ImageData> dataListener) {

        DataStateHolder dataState = new DataStateHolder(FIRST_STATE);
        AtomicBoolean abortedState = new AtomicBoolean(false);
        AsyncDataListener<ImageData> safeListener
                = AsyncDatas.makeSafeListener(dataListener);

        Future<?> taskFuture = executor.submit(new ImageReaderTask(
                imageUri, minUpdateTime, dataState, abortedState, safeListener));

        return new ImageReaderController(
                dataState, taskFuture, abortedState, safeListener);
    }

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

    private static class ImageReaderTask implements Runnable {
        private final URI imageUri;
        private final long minUpdateTime;
        private final DataStateHolder dataState;
        private final AtomicBoolean abortedState;
        private final AsyncDataListener<ImageData> safeListener;

        public ImageReaderTask(
                URI imageUri,
                long minUpdateTime,
                DataStateHolder dataState,
                AtomicBoolean abortedState,
                AsyncDataListener<ImageData> safeListener) {

            this.imageUri = imageUri;
            this.minUpdateTime = minUpdateTime;
            this.dataState = dataState;
            this.abortedState = abortedState;
            this.safeListener = safeListener;
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

        @Override
        public void run() {
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
    }

    private static class ImageReaderController implements AsyncDataController {
        private final DataStateHolder dataState;
        private final Future<?> taskFuture;
        private final AtomicBoolean abortedState;
        private final AsyncDataListener<ImageData> safeListener;

        public ImageReaderController(DataStateHolder dataState,
                Future<?> taskFuture,
                AtomicBoolean abortedState,
                AsyncDataListener<ImageData> safeListener) {

            this.dataState = dataState;
            this.taskFuture = taskFuture;
            this.abortedState = abortedState;
            this.safeListener = safeListener;
        }

        @Override
        public AsyncDataState getDataState() {
            return dataState.getDataState();
        }

        @Override
        public void cancel() {
            taskFuture.cancel(true);
            abortedState.set(true);
            safeListener.onDoneReceive(AsyncReport.CANCELED);
        }

        @Override
        public void controlData(Object controlArg) {
        }
    }
}
