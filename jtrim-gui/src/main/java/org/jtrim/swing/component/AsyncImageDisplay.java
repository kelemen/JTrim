/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;

import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.jtrim.cache.MemoryHeavyObject;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.concurrent.async.*;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageReceiveException;
import org.jtrim.image.transform.*;
import org.jtrim.swing.concurrent.async.*;
import org.jtrim.swing.event.ImageListener;
import org.jtrim.utils.ExceptionHelper;

/**
 * @author Kelemen Attila
 */
@SuppressWarnings("serial") // Not serializable
public class AsyncImageDisplay<ImageAddressType> extends SlowDrawingComponent {
    private final ListenerManager<ImageListener, Void> imageListeners;
    private final EventDispatcher<ImageListener, Void> metaDataHandler;
    private final EventDispatcher<ImageListener, Void> imageChangeHandler;

    private AsyncDataQuery<? super ImageAddressType, ? extends ImageData> rawImageQuery;
    private AsyncDataQuery<ImageAddressType, DataWithUid<ImageData>> imageQuery;
    private ImageAddressType currentImageAddress;

    private AsyncDataLink<DataWithUid<ImageData>> imageLink;
    private final SortedMap<Integer, CachedQuery> imageTransformers;

    private long imageReplaceTime;
    private long imageShownTime;
    private boolean imageShown;
    private long oldImageHideTime;

    private ImageMetaData imageMetaData;
    private boolean metaDataCompleted;

    private ImagePointTransformer displayedPointTransformer;

    public AsyncImageDisplay() {
        this.rawImageQuery = null;
        this.imageQuery = null;
        this.imageLink = null;
        this.imageListeners = new CopyOnTriggerListenerManager<>();
        this.imageTransformers = new TreeMap<>();

        this.imageReplaceTime = System.nanoTime();
        this.imageShownTime = imageReplaceTime;
        this.imageShown = false;

        this.metaDataCompleted = false;
        this.imageMetaData = null;

        this.oldImageHideTime = TimeUnit.MILLISECONDS.toNanos(1000);
        this.displayedPointTransformer = null;

        this.imageChangeHandler = new ImageChangeHandler();
        this.metaDataHandler = new MetaDataHandler();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setDirty();
            }
        });
    }

    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    public long getOldImageHideNanos() {
        return oldImageHideTime;
    }

    public void setOldImageHideTime(long oldImageHideTime, TimeUnit timeUnit) {
        this.oldImageHideTime = timeUnit.toNanos(oldImageHideTime);
    }

    public long getNanosSinceImageChange() {
        return System.nanoTime() - imageReplaceTime;
    }

    public long getNanosSinceLastImageShow() {
        return System.nanoTime() - imageShownTime;
    }

    public boolean isCurrentImageShown() {
        return imageShown;
    }

    public ImageAddressType getCurrentImageAddress() {
        return currentImageAddress;
    }

    public AsyncDataQuery<? super ImageAddressType, ? extends ImageData> getImageQuery() {
        return rawImageQuery;
    }

    public void setImageQuery(AsyncDataQuery<? super ImageAddressType, ? extends ImageData> imageQuery) {
        setImageQuery(imageQuery, null);
    }

    public void setImageQuery(AsyncDataQuery<? super ImageAddressType, ? extends ImageData> imageQuery, ImageAddressType imageAddress) {
        this.currentImageAddress = imageAddress;
        this.rawImageQuery = imageQuery;
        this.imageQuery = imageQuery != null
                ? AsyncQueries.markResultsWithUid(imageQuery)
                : null;

        AsyncDataLink<DataWithUid<ImageData>> newLink = null;

        if (imageQuery != null) {
            if (imageAddress != null) {
                newLink = this.imageQuery.createDataLink(imageAddress);
                setImageLink(this.imageQuery.createDataLink(imageAddress));
            }
        }
        else if (imageAddress != null) {
            throw new IllegalStateException("null image query cannot query images.");
        }

        setImageLink(newLink);
    }

    public void setImageAddress(ImageAddressType imageAddress) {
        if (imageQuery == null && imageAddress != null) {
            throw new IllegalStateException("null image query cannot query images.");
        }
        currentImageAddress = imageAddress;

        AsyncDataLink<DataWithUid<ImageData>> newLink = null;

        if (imageQuery != null && imageAddress != null) {
            newLink = imageQuery.createDataLink(imageAddress);
        }

        setImageLink(newLink);
    }

    private void setImageLink(AsyncDataLink<DataWithUid<ImageData>> imageLink) {
        if (this.imageLink != imageLink) {
            if (imageShown) {
                imageShownTime = System.nanoTime();
            }

            metaDataCompleted = false;
            imageMetaData = null;

            displayedPointTransformer = null;

            imageShown = false;
            imageReplaceTime = System.nanoTime();

            this.imageLink = imageLink;
            imageListeners.onEvent(imageChangeHandler, null);

            setDirty();
        }
    }

    public void clearImageTransformers() {
        imageTransformers.clear();
    }

    public void removeImageTransformer(int index) {
        imageTransformers.remove(index);
    }

    public void setImageTransformer(int index,
            ReferenceType refType,
            AsyncDataQuery<ImageTransformerData, TransformedImageData> imageTransformer) {
        setImageTransformer(index, refType, null, imageTransformer);
    }

    public void setImageTransformer(int index,
            ReferenceType refType, ObjectCache refCreator,
            AsyncDataQuery<ImageTransformerData, TransformedImageData> imageTransformer) {

        ExceptionHelper.checkNotNullArgument(refType, "refType");
        ExceptionHelper.checkNotNullArgument(imageTransformer, "imageTransformer");

        /*
         * InternalTransformerData
         * 0. Store query arg
         * 1. Query imageTransformer
         * 2. Cache result
         * 3. Convert to DataWithID<InternalTransformerData>
         *
         * InternalTransformerData
         */
        imageTransformers.put(index, new CachedQuery(imageTransformer, refType, refCreator));

        setDirty();
    }

    private class CachedQuery
    implements
            AsyncDataQuery<DataWithUid<InternalTransformerData>, DataWithUid<InternalTransformerData>> {

        private final AsyncDataQuery<CachedLinkRequest<DataWithUid<ImageTransformerData>>, DataWithUid<TransformedImageData>> cachedTransformerQuery;

        public CachedQuery(
                AsyncDataQuery<ImageTransformerData, TransformedImageData> imageTranformerQuery,
                ReferenceType refType, ObjectCache refCreator) {

            ExceptionHelper.checkNotNullArgument(refType, "refType");
            ExceptionHelper.checkNotNullArgument(imageTranformerQuery, "imageTranformerQuery");

            this.cachedTransformerQuery = AsyncQueries.cacheByID(
                    imageTranformerQuery, refType, refCreator, 1);
        }

        @Override
        public AsyncDataLink<DataWithUid<InternalTransformerData>> createDataLink(
                DataWithUid<InternalTransformerData> argWithID) {

            // Simply convert the cached value to the internal format.
            InternalTransformerData arg = argWithID.getData();

            ImageReceiveException prevException = arg.getException();
            ImagePointTransformer prevPointTransformer = arg.getPointTransformer();
            ImageMetaData metaData = arg.getMetaData();
            InternalRenderingData renderingData = arg.getRenderingData();
            boolean receivedImage = arg.isReceivedImage();

            CachedLinkRequest<DataWithUid<ImageTransformerData>> request;
            request = new CachedLinkRequest<>(
                    new DataWithUid<>(arg.getTransformerData(), argWithID.getID()),
                    Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            AsyncDataLink<DataWithUid<TransformedImageData>> transformerLink;
            transformerLink = cachedTransformerQuery.createDataLink(request);

            ToInternalConverterLink converter;
            converter = new ToInternalConverterLink(prevPointTransformer,
                    prevException, metaData, renderingData, receivedImage);

            return AsyncLinks.convertResult(transformerLink, converter);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(256);
            result.append("Extract image transformer input then use ");
            AsyncFormatHelper.appendIndented(cachedTransformerQuery, result);
            result.append("\nConvert results to AsyncImageDisplay.InternalFormat");

            return result.toString();
        }
    }

    private class ToInternalConverterLink
    implements
            DataConverter<DataWithUid<TransformedImageData>, DataWithUid<InternalTransformerData>> {

        private final ImagePointTransformer prevPointTransformer;
        private final ImageReceiveException prevException;
        private final ImageMetaData metaData;
        private final InternalRenderingData renderingData;
        private final boolean receivedImage;

        public ToInternalConverterLink(
                ImagePointTransformer prevPointTransformer,
                ImageReceiveException prevException,
                ImageMetaData metaData,
                InternalRenderingData renderingData,
                boolean receivedImage) {

            this.prevPointTransformer = prevPointTransformer;
            this.prevException = prevException;
            this.metaData = metaData;
            this.renderingData = renderingData;
            this.receivedImage = receivedImage;
        }

        @Override
        public DataWithUid<InternalTransformerData> convertData(DataWithUid<TransformedImageData> data) {
            TransformedImageData resultImageData = data.getData();
            TransformedImage resultImage = resultImageData != null
                    ? resultImageData.getTransformedImage()
                    : null;

            TransformedImage newImage;
            newImage = changePointTransformer(
                    prevPointTransformer, resultImage);

            ImageReceiveException transfException;
            transfException = resultImageData != null
                    ? resultImageData.getException()
                    : null;

            ImageReceiveException exception;

            if (prevException == null) {
                exception = transfException;
            }
            else if (transfException == null) {
                exception = prevException;
            }
            else {
                exception = new ImageReceiveException();
                exception.addSuppressed(prevException);
                exception.addSuppressed(transfException);
            }

            InternalTransformerData result;
            result = new InternalTransformerData(
                    newImage,
                    metaData,
                    exception,
                    renderingData,
                    receivedImage);

            return new DataWithUid<>(result, data.getID());
        }
    }

    public ImagePointTransformer getDisplayedPointTransformer() {
        return displayedPointTransformer;
    }

    public ImagePointTransformer getPointTransformer() {
        return getDisplayedPointTransformer();
    }

    protected void prepareTransformations() {
    }

    private static class ImageResultConverter
    implements
            DataConverter<DataWithUid<ImageData>, DataWithUid<InternalTransformerData>> {

        private final InternalRenderingData renderingData;

        public ImageResultConverter(InternalRenderingData renderingData) {
            this.renderingData = renderingData;
        }

        @Override
        public DataWithUid<InternalTransformerData> convertData(DataWithUid<ImageData> data) {
            ImageData imageData = data.getData();

            if (imageData != null) {
                InternalTransformerData newData;

                newData = new InternalTransformerData(
                        new TransformedImage(imageData.getImage(), null),
                        imageData.getMetaData(),
                        imageData.getException(),
                        renderingData,
                        imageData.getImage() != null);

                return new DataWithUid<>(newData, data.getID());
            }
            else {
                return new DataWithUid<>(null, data.getID());
            }
        }

        @Override
        public String toString() {
            return "Image -> AsyncImageDisplay.InternalFormat";
        }
    }

    @Override
    public RenderingParameters getCurrentRenderingParams() {
        prepareTransformations();

        BasicRenderingArguments renderingArgs = new BasicRenderingArguments(this);

        if (imageLink != null) {
            InternalRenderingData renderingData;
            renderingData = new InternalRenderingData(getWidth(), getHeight(), imageLink);

            AsyncDataLink<DataWithUid<InternalTransformerData>> currentLink;
            currentLink = AsyncLinks.convertResult(imageLink,
                    new ImageResultConverter(renderingData));

            for (CachedQuery transformer: imageTransformers.values()) {
                currentLink = AsyncLinks.convertResult(currentLink, transformer);
            }

            AsyncDataLink<InternalTransformerData> resultLink;
            resultLink = AsyncLinks.removeUidFromResult(currentLink);

            return new RenderingParameters(renderingArgs, resultLink);
        }
        else {
            return new RenderingParameters(renderingArgs);
        }
    }

    @Override
    protected void postLongRendering(Graphics2D g,
            RenderingFuture renderingFuture, GraphicsCopyResult copyResult) {

        if (!copyResult.isPainted() ||
                (!isCurrentImageShown() &&
                getNanosSinceLastImageShow() > getOldImageHideNanos())) {
            g.setBackground(getBackground());
            g.clearRect(0, 0, getWidth(), getHeight());
        }

        g.setColor(getForeground());
        g.setFont(getFont());
        g.setBackground(getBackground());

        MultiAsyncDataState states =
                (MultiAsyncDataState)renderingFuture.getAsyncDataState();

        displayLongRenderingState(g, states, getImageMetaData());
    }

    @Override
    protected void postRendering(Graphics2D g,
            RenderingFuture renderingFuture, GraphicsCopyResult copyResult) {

        InternalPaintResult paintResult = null;

        if (copyResult != null) {
            paintResult = (InternalPaintResult)copyResult.getPaintResult();
        }

        if (paintResult != null && paintResult.getImageLink() == imageLink) {
            if (!imageShown) {
                imageShown = paintResult.isImageReceived();
            }

            if (imageShown) {
                imageShownTime = System.nanoTime();
            }

            if (!metaDataCompleted) {
                ImageMetaData newMetaData = paintResult.getMetaData();
                if (newMetaData != null) {
                    imageMetaData = newMetaData;
                    metaDataCompleted = newMetaData.isComplete();
                    imageListeners.onEvent(metaDataHandler, null);
                }
            }

            ImagePointTransformer currentPointTransformer;
            currentPointTransformer = paintResult.getPointTransformer();
            if (currentPointTransformer != null) {
                displayedPointTransformer = currentPointTransformer;
            }
        }
    }

    protected void displayLongRenderingState(Graphics2D g,
            MultiAsyncDataState dataStates, ImageMetaData imageMetaData) {
        int stateCount = dataStates != null ? dataStates.getSubStateCount() : 0;

        if (stateCount > 0) {
            StringBuilder message = new StringBuilder(128);
            message.append("Rendering: ");

            for (int i = 0; i < stateCount; i++) {
                if (i > 0) {
                    message.append(", ");
                }

                message.append(Math.round(100.0 * dataStates.getSubProgress(i)));
                message.append("%");
            }

            if (imageMetaData != null) {
                message.append("\nDim: ");
                message.append(imageMetaData.getWidth());
                message.append("X");
                message.append(imageMetaData.getHeight());
            }

            RenderHelper.drawMessage(g, message.toString());
        }
    }

    protected void onRenderingError(BasicRenderingArguments renderingArgs, BufferedImage drawingSurface, ImageReceiveException exception) {
        Graphics2D g = drawingSurface.createGraphics();
        try {
            g.setColor(renderingArgs.getForegroundColor());
            g.setFont(renderingArgs.getFont());
            g.setBackground(renderingArgs.getBackgroundColor());

            g.clearRect(0, 0, drawingSurface.getWidth(), drawingSurface.getHeight());

            g.setColor(renderingArgs.getForegroundColor());
            g.setFont(renderingArgs.getFont());

            StringBuilder errorText = new StringBuilder(128);

            if (exception != null) {
                errorText.append("Error: ");
                errorText.append(exception.getMessage());
            }
            else {
                errorText.append("Unknown error while loading/rendering image.");
            }

            RenderHelper.drawMessage(g, errorText.toString());
        } finally {
            g.dispose();
        }
    }

    @Override
    public RenderingResult renderComponent(
            Object userDefRenderingParams,
            Object blockingData,
            BufferedImage drawingSurface) {

        InternalTransformerData imageData = (InternalTransformerData)blockingData;
        BasicRenderingArguments renderingArgs = (BasicRenderingArguments)userDefRenderingParams;

        if (imageData != null) {
            BufferedImage image = imageData.getImage();

            Graphics2D g = drawingSurface.createGraphics();
            try {
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
                else {
                    g.setBackground(renderingArgs.getBackgroundColor());
                    g.clearRect(0, 0,
                            drawingSurface.getWidth(),
                            drawingSurface.getHeight());
                }
            } finally {
                g.dispose();
            }

            if (imageData.getException() != null) {
                onRenderingError(renderingArgs, drawingSurface, imageData.getException());
            }

            return RenderingResult.done(new InternalPaintResult(
                    imageData.isReceivedImage(),
                    imageData.getPointTransformer(),
                    imageData.getMetaData(),
                    imageData.getImageLink()));
        }
        else {
            return RenderingResult.skipPaint();
        }
    }

    private static class InternalPaintResult {
        private final boolean imageReceived;
        private final ImagePointTransformer pointTransformer;
        private final ImageMetaData metaData;
        private final AsyncDataLink<DataWithUid<ImageData>> imageLink;

        public InternalPaintResult(boolean imageReceived,
                ImagePointTransformer pointTransformer,
                ImageMetaData metaData,
                AsyncDataLink<DataWithUid<ImageData>> imageLink) {
            this.imageReceived = imageReceived;
            this.pointTransformer = pointTransformer;
            this.metaData = metaData;
            this.imageLink = imageLink;
        }

        public AsyncDataLink<DataWithUid<ImageData>> getImageLink() {
            return imageLink;
        }

        public boolean isImageReceived() {
            return imageReceived;
        }

        public ImagePointTransformer getPointTransformer() {
            return pointTransformer;
        }

        public ImageMetaData getMetaData() {
            return metaData;
        }
    }

    private static class InternalRenderingData {
        private final int destWidth;
        private final int destHeight;
        private final AsyncDataLink<DataWithUid<ImageData>> imageLink;

        public InternalRenderingData(
                int destWidth,
                int destHeight,
                AsyncDataLink<DataWithUid<ImageData>> imageLink) {
            this.destWidth = destWidth;
            this.destHeight = destHeight;
            this.imageLink = imageLink;
        }

        public AsyncDataLink<DataWithUid<ImageData>> getImageLink() {
            return imageLink;
        }

        public int getDestHeight() {
            return destHeight;
        }

        public int getDestWidth() {
            return destWidth;
        }
    }

    private static class InternalTransformerData implements MemoryHeavyObject {

        private final InternalRenderingData renderingData;
        private final ImageMetaData metaData;
        private final boolean receivedImage;
        private final TransformedImageData transformedImageData;

        public InternalTransformerData(
                TransformedImage transformedImage,
                ImageMetaData metaData,
                ImageReceiveException exception,
                InternalRenderingData renderingData,
                boolean receivedImage) {

            this.renderingData = renderingData;
            this.metaData = metaData;
            this.receivedImage = receivedImage;
            this.transformedImageData
                    = new TransformedImageData(transformedImage, exception);
        }

        public ImageTransformerData getTransformerData() {
            return new ImageTransformerData(
                    getImage(),
                    getDestWidth(),
                    getDestHeight(),
                    getMetaData());
        }

        public ImageMetaData getMetaData() {
            return metaData;
        }

        public boolean isReceivedImage() {
            return receivedImage;
        }

        public InternalRenderingData getRenderingData() {
            return renderingData;
        }

        public AsyncDataLink<DataWithUid<ImageData>> getImageLink() {
            return renderingData.getImageLink();
        }

        public int getDestWidth() {
            return renderingData.getDestWidth();
        }

        public int getDestHeight() {
            return renderingData.getDestHeight();
        }

        public ImageReceiveException getException() {
            return transformedImageData.getException();
        }

        public TransformedImage getTransformedImage() {
            return transformedImageData.getTransformedImage();
        }

        public ImagePointTransformer getPointTransformer() {
            return transformedImageData.getPointTransformer();
        }

        public BufferedImage getImage() {
            return transformedImageData.getImage();
        }

        @Override
        public long getApproxMemorySize() {
            return transformedImageData.getApproxMemorySize();
        }
    }

    private static TransformedImage changePointTransformer(ImagePointTransformer prevPointTransformer, TransformedImage image) {
        if (prevPointTransformer == null || image == null) {
            return image;
        }

        ImagePointTransformer newPointTransformer;
        ImagePointTransformer currentPointTransformer;
        currentPointTransformer = image.getPointTransformer();

        if (currentPointTransformer != null) {
            newPointTransformer = new SerialImagePointTransformer(prevPointTransformer, currentPointTransformer);
        }
        else {
            newPointTransformer = prevPointTransformer;
        }

        return new TransformedImage(image.getImage(), newPointTransformer);
    }

    private class ImageChangeHandler implements EventDispatcher<ImageListener, Void> {
        @Override
        public void onEvent(ImageListener eventArgument, Void arg) {
            eventArgument.onChangeImage(AsyncLinks.removeUidFromResult(imageLink));
        }
    }

    private class MetaDataHandler implements EventDispatcher<ImageListener, Void> {
        @Override
        public void onEvent(ImageListener eventArgument, Void arg) {
            eventArgument.onReceiveMetaData(imageMetaData);
        }
    }
}
