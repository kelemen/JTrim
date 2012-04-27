package org.jtrim.image;

import javax.imageio.metadata.IIOMetadata;

/**
 * Defines the meta data of images retrieved using the {@code ImageIO} library
 * of Java. Apart from the properties derived from {@link ImageMetaData}, this
 * class adds one more property: {@link IIOMetadata}. The {@code IIOMetadata}
 * is the object used by the {@code ImageIO} library of Java for describing the
 * meta data information of an image.
 *
 * <h3>Thread safety</h3>
 * Methods of this class can be safely accessed by multiple threads. The
 * methods of the subclasses of this class must be safe to be accessed
 * concurrently as well. {@code JavaIIOMetaData} are not completely immutable
 * because {@code IIOMetadata} objects are not immutable. Note however, that
 * the {@code IIOMetadata} property of {@code JavaIIOMetaData} must not be
 * altered.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I> and the
 * subclasses of this class must be <I>synchronization transparent</I> as well.
 *
 * @see ImageData
 *
 * @author Kelemen Attila
 */
public class JavaIIOMetaData extends ImageMetaData {
    private final IIOMetadata iioMetaData;

    /**
     * Initializes the {@code JavaIIOMetaData} with the specified properties.
     *
     * @param width the width in number of pixels of the image whose meta data
     *   is the newly created {@code ImageMetaData}. This argument must be
     *   greater than or equal to zero.
     * @param height the height in number of pixels of the image whose meta data
     *   is the newly created {@code ImageMetaData}. This argument must be
     *   greater than or equal to zero.
     * @param iioMetaData the {@code IIOMetadata} supplied by the
     *   {@code ImageIO} library of Java. This argument can be {@code null} if
     *   for some reason it is unavailable.
     * @param complete a {@code boolean} indicating if the image whose meta data
     *   is the newly created {@code ImageMetaData} is a complete or partial
     *   image. If this argument is {@code true}, the image is completely
     *   loaded. If {@coda false}, the image is only partially available.
     *
     * @throws IllegalArgumentException thrown if the width or height argument
     *   is illegal
     */
    public JavaIIOMetaData(int width, int height, IIOMetadata iioMetaData, boolean complete) {
        super(width, height, complete);
        this.iioMetaData = iioMetaData;
    }

    /**
     * Returns the {@code IIOMetadata} as provided by the {@code ImageIO}
     * library of Java. This is the same object as the one specified at
     * construction time.
     *
     * @return the {@code IIOMetadata} as provided by the {@code ImageIO}
     *   library of Java. This method may return {@code null} if {@code null}
     *   was provided at construction time.
     */
    public final IIOMetadata getIioMetaData() {
        return iioMetaData;
    }
}
