/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.image.transform;

import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.concurrent.async.DataConverter;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
class ImageConverter
implements
        DataConverter<ImageTransformerData, TransformedImageData> {

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
        StringBuilder result = new StringBuilder(256);
        result.append("Convert image using ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }
}
