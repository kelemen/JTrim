package org.jtrim2.swing.component;

import org.jtrim2.concurrent.async.AsyncDataLink;
import org.jtrim2.image.ImageMetaData;

/**
 * @deprecated This interface is only used by deprecated classes. There is no
 *   replacement for this interface.
 *
 * The interface for the listeners which are to be notified by the
 * {@link AsyncImageDisplay} when its underlying image changes or successfully
 * receives the meta-data of the image.
 *
 * <h3>Thread safety</h3>
 * Instances of this listener interface can only be notified from the
 * <I>AWT Event Dispatch Thread</I> and therefore does not need to be
 * thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface does not need to be
 * <I>synchronization transparent</I> but they must be aware that they are
 * called from the <I>AWT Event Dispatch Thread</I>.
 *
 * @author Kelemen Attila
 */
@Deprecated
public interface ImageListener {
    /**
     * Invoked when the source of the image of an {@link AsyncImageDisplay}
     * changes. This might either be due to setting the
     * {@link AsyncImageDisplay#setImageQuery(org.jtrim2.concurrent.async.AsyncDataQuery, Object) image query}
     * or due to setting the {@link AsyncImageDisplay#setImageAddress(Object) image address}.
     *
     * @param imageLink the {@code AsyncDataLink} which can be used to retrieve
     *   the image. This method may start to fetch the image if it wishes to
     *   analyze the image to be displayed. This argument can be {@code null}
     *   if either the image query or the image address has been set to
     *   {@code null}.
     */
    public void onChangeImage(AsyncDataLink<org.jtrim2.image.ImageData> imageLink);

    /**
     * Invoked when the {@link AsyncImageDisplay} successfully retrieves the
     * meta-data of the image to be displayed. This method might be called
     * multiple times for a single image retrieval as more and more data becomes
     * available. If the meta-data is not available, this method will never be
     * called.
     *
     * @param metaData the meta-data of the image to be displayed. This argument
     *   cannot be {@code null}.
     */
    public void onReceiveMetaData(ImageMetaData metaData);
}
