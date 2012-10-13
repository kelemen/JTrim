package org.jtrim.image.transform;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.*;

public final class ImageTransformerQuery
implements
        AsyncDataQuery<ImageTransformerData, TransformedImageData> {

    private final List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> imageTransformers;

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

    @Override
    public AsyncDataLink<TransformedImageData> createDataLink(ImageTransformerData arg) {
        return ImageTransformerLink.createFromDataTransformers(
                arg, imageTransformers);
    }

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
