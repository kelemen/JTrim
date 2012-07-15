package org.jtrim.image.transform;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.*;

public final class ImageTransformerLink implements AsyncDataLink<TransformedImageData> {

    private final AsyncDataLink<TransformedImageData> wrappedLink;

    static ImageTransformerLink createFromDataTransformers(
            ImageTransformerData input,
            List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> imageTransformers) {

        return new ImageTransformerLink(
                AsyncLinks.convertGradually(input, imageTransformers));
    }

    public ImageTransformerLink(ImageTransformerData input,
            TaskExecutorService executor, ImageTransformer... imageTransformers) {

        List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> taskList;
        taskList = new ArrayList<>(imageTransformers.length);

        for (ImageTransformer transformer: imageTransformers) {
            DataConverter<ImageTransformerData, TransformedImageData> converter;
            converter = new ImageConverter(transformer);
            taskList.add(new AsyncDataConverter<>(converter, executor));
        }

        wrappedLink = AsyncLinks.convertGradually(input, taskList);
    }

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

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super TransformedImageData> dataListener) {
        return wrappedLink.getData(cancelToken, dataListener);
    }

    @Override
    public String toString() {
        return wrappedLink.toString();
    }
}
