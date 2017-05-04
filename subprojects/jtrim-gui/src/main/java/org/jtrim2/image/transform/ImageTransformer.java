package org.jtrim2.image.transform;

import org.jtrim2.concurrent.async.DataConverter;

/**
 * @deprecated Use {@link ImageTransformationStep} instead.
 *
 * Defines an image transformation from {@link ImageTransformerData} to
 * {@link TransformedImage}.
 * <P>
 * When applying a transformation on the input, the conversion must specify
 * the coordinate transformation which transforms the coordinates of the source
 * image to the coordinates of the resulting image. That is, where a pixel of
 * the original image can be found on the resulting image. The coordinate
 * transformation must support coordinates laying outside the bounds of the
 * image.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be used by
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @see ImageTransformerLink
 * @see ImageTransformerQuery
 * @see org.jtrim2.swing.component.AsyncImageDisplay
 *
 * @author Kelemen Attila
 */
@Deprecated
public interface ImageTransformer
extends
        DataConverter<ImageTransformerData, TransformedImage> {
}
