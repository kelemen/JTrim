package org.jtrim.image.transform;

import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.concurrent.async.DataConverter;
import org.jtrim.utils.ExceptionHelper;

/**
 * @deprecated This class is only used by deprecated classes. There is no
 *   replacement for this class.
 *
 * @see ImageTransformerLink
 * @see ImageTransfromerQuery
 *
 * @author Kelemen Attila
 */
@Deprecated
final class ImageConverter
implements
        DataConverter<ImageTransformerData, TransformedImageData> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final DataConverter<ImageTransformerData, TransformedImage> converter;

    public ImageConverter(
            DataConverter<ImageTransformerData, TransformedImage> converter) {

        ExceptionHelper.checkNotNullArgument(converter, "converter");

        this.converter = converter;
    }

    @Override
    public TransformedImageData convertData(ImageTransformerData data) {
        TransformedImage transformed = converter.convertData(data);
        return new TransformedImageData(transformed, null);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Convert image using ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }
}
