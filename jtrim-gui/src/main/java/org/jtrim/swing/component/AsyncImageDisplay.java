package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.concurrent.async.AsyncLinks;
import org.jtrim.concurrent.async.AsyncQueries;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.CachedLinkRequest;
import org.jtrim.concurrent.async.DataConverter;
import org.jtrim.concurrent.async.DataWithUid;
import org.jtrim.concurrent.async.MultiAsyncDataState;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.transform.ImagePointTransformer;
import org.jtrim.image.transform.ImageTransformerData;
import org.jtrim.image.transform.SerialImagePointTransformer;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.image.transform.TransformedImageData;
import org.jtrim.swing.concurrent.async.AsyncRenderer;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.BasicRenderingArguments;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.jtrim.utils.ExceptionHelper;

/**
 * @deprecated This class is very inefficient, use
 *   {@link TransformedImageDisplay} instead.
 *
 * Defines a <I>Swing</I> component which is able to display an image applying
 * a series of user defined transformations.
 * <P>
 * There are three kind of properties you need to specify for this component:
 * <ul>
 *  <li>
 *   The address of the image to be displayed. The address will be specified
 *   for the query used to retrieve the image. So the address can be any type,
 *   for example: {@code java.net.URI}. The address can be set by calling the
 *   {@link #setImageAddress(Object) setImageAddress} method.
 *  </li>
 *  <li>
 *   The {@link AsyncDataQuery} used to retrieve the image. The image is
 *   retrieved by the address specified previously. The query can be set by
 *   calling one of the {@link #setImageQuery(AsyncDataQuery, Object) setImageQuery}
 *   methods.
 *  </li>
 *  <li>
 *   The transformations which will be used to transform the image retrieved by
 *   the image query. These transformations can be set by calling one of the
 *   {@link #setImageTransformer(int, ReferenceType, ObjectCache, AsyncDataQuery) setImageTransformer}
 *   methods. Note that multiple transformations can be applied to a given image
 *   in any order.
 *  </li>
 * </ul>
 * <P>
 * Note that this component is an {@link AsyncRenderingComponent} and relies on
 * an {@link AsyncRenderer}. Therefore it must be set before displaying this
 * component, either by passing an {@link AsyncRendererFactory} to the
 * appropriate constructor or by
 * {@link #setAsyncRenderer(AsyncRendererFactory) setting it later}.
 * <P>
 * <B>This class is highly experimental and may see significant changes in
 * the future.</B>
 * <P>
 * The thread-safety property of this component is the same as with any other
 * <I>Swing</I> components. That is, instances of this class can be accessed
 * only from the AWT Event Dispatch Thread after made displayable.
 *
 * @param <ImageAddress> the type of the address of the image to be
 *   displayed. That is, the input of the
 *   {@link #setImageQuery(AsyncDataQuery, Object) image query}.
 *
 * @see SimpleAsyncImageDisplay
 *
 * @author Kelemen Attila
 */
@Deprecated
@SuppressWarnings({ "serial", "deprecation" }) // Not serializable
public class AsyncImageDisplay<ImageAddress> extends AsyncRenderingComponent {
    private static final int RENDERING_STATE_POLL_TIME_MS = 100;
    private static final long DEFAULT_OLD_IMAGE_HIDE_MS = 1000;
    private static final int DEFAULT_STRING_BUILDER_LENGTH = 256;

    private final ListenerManager<ImageListener> imageListeners;
    private final EventDispatcher<ImageListener, Void> metaDataHandler;
    private final EventDispatcher<ImageListener, Void> imageChangeHandler;

    private AsyncDataQuery<? super ImageAddress, ? extends org.jtrim.image.ImageData> rawImageQuery;
    private AsyncDataQuery<ImageAddress, DataWithUid<org.jtrim.image.ImageData>> imageQuery;
    private ImageAddress currentImageAddress;

    private AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> imageLink;
    private final SortedMap<Integer, CachedQuery> imageTransformers;
    private final ListenerManager<Runnable> addressChangeListeners;

    private long imageReplaceTime;
    private long imageShownTime;
    private boolean imageShown;
    private long oldImageHideTime;

    private ImageMetaData imageMetaData;
    private boolean metaDataCompleted;

    private ImagePointTransformer displayedPointTransformer;

    private boolean needLongRendering;
    private long renderingPatienceNanos;

    /**
     * Creates a new {@code AsyncImageDisplay} without setting the
     * {@link AsyncRenderer} to be used. Therefore the
     * {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} must be
     * called before displaying the component.
     */
    public AsyncImageDisplay() {
        this(null);
    }

    /**
     * Creates a new {@code AsyncImageDisplay} using an {@link AsyncRenderer}
     * created by the specified {@link AsyncRendererFactory}. Note however, that
     * if you pass {@code null} for the argument of this constructor, you still
     * have to call the {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer}
     * method before displaying the component.
     *
     * @param asyncRenderer the {@code AsyncRendererFactory} to be used to
     *   render this component. This argument can be {@code null}, in which
     *   case, the {@code AsyncRendererFactory} must be set later by the
     *   {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} method.
     */
    public AsyncImageDisplay(AsyncRendererFactory asyncRenderer) {
        super(asyncRenderer);

        this.rawImageQuery = null;
        this.imageQuery = null;
        this.imageLink = null;
        this.imageListeners = new CopyOnTriggerListenerManager<>();
        this.imageTransformers = new TreeMap<>();
        this.addressChangeListeners = new CopyOnTriggerListenerManager<>();

        this.imageReplaceTime = System.nanoTime();
        this.imageShownTime = imageReplaceTime;
        this.imageShown = false;

        this.metaDataCompleted = false;
        this.imageMetaData = null;

        this.needLongRendering = false;
        this.renderingPatienceNanos = 0;

        this.oldImageHideTime = TimeUnit.MILLISECONDS.toNanos(DEFAULT_OLD_IMAGE_HIDE_MS);
        this.displayedPointTransformer = null;

        this.imageChangeHandler = new ImageChangeHandler();
        this.metaDataHandler = new MetaDataHandler();

        setRenderingArgs(null, null, new BasicRenderingArguments(this));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setupRenderingArgs();
            }
        });
    }

    /**
     * Disables additional rendering when the rendering takes too much time.
     * That is, if this method is called, the
     * {@link #displayLongRenderingState(Graphics2D, MultiAsyncDataState, ImageMetaData) displayLongRenderingState}
     * method will not be notified to do additional rendering regardless how
     * much time the rendering takes.
     * <P>
     * This method effectively invalidates any previous calls to the
     * {@link #setLongRenderingTimeout(long, TimeUnit) setLongRenderingTimeout}.
     * The long rendering timeout can be enabled again by calling the
     * {@code setLongRenderingTimeout} method.
     *
     * @see #displayLongRenderingState(Graphics2D, MultiAsyncDataState, ImageMetaData) displayLongRenderingState
     * @see #setLongRenderingTimeout(long, TimeUnit) setLongRenderingTimeout
     */
    public void setInfLongRenderTimeout() {
        if (needLongRendering) {
            needLongRendering = false;
            repaint();
        }
    }

    /**
     * Sets the timeout value after additional rendering may be applied to this
     * component if after this timeout value the rendering does not complete.
     * <P>
     * The default value for this property is infinite.
     * <P>
     * The timeout value is measured from the start of the last requested
     * rendering request and the additional rendering is applied until the
     * rendering is fully completed.
     *
     * @param time the timeout value in the given time unit to wait until the
     *   {@link #displayLongRenderingState(Graphics2D, MultiAsyncDataState, ImageMetaData) displayLongRenderingState}
     *   method is to be called. This argument must be greater than or equal to
     *   zero.
     * @param timeunit the time unit of the specified timeout value. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   not greater than or equal to zero
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public void setLongRenderingTimeout(long time, TimeUnit timeunit) {
        ExceptionHelper.checkArgumentInRange(time, 0, Long.MAX_VALUE, "time");
        ExceptionHelper.checkNotNullArgument(timeunit, "timeunit");

        this.renderingPatienceNanos = timeunit.toNanos(time);
        if (!needLongRendering) {
            this.needLongRendering = true;
            repaint();
        }
    }

    /**
     * Returns the meta-data of the last retrieved image or {@code null} if the
     * meta-data is not (yet) available. Note that if this component is never
     * displayed, no attempt will be made to fetch the image, therefore this
     * method will always return {@code null}.
     *
     * @return the meta-data of the last retrieved image or {@code null} if the
     *   meta-data is not (yet) available
     */
    public final ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    private long getOldImageHideNanos() {
        return oldImageHideTime;
    }

    /**
     * Returns the time need to elapse before clearing this component after
     * changing the image (changing the {@link #setImageAddress(Object) address}
     * or the {@link #setImageQuery(AsyncDataQuery, Object) query}.
     * <P>
     * Note that this method may not return exactly the same value as set by a
     * previous {@link #setOldImageHideTime(long, TimeUnit) setOldImageHideTime}
     * method call due to rounding errors.
     *
     * @param timeunit the time unit of the result to be returned. This argument
     *   cannot be {@code null}.
     * @return the time need to elapse before clearing this component after
     *   changing the image. This method always returns a value greater than or
     *   equal to zero.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final long getOldImageHideTimeout(TimeUnit timeunit) {
        return timeunit.convert(oldImageHideTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Sets the time need to elapse before clearing this component after
     * changing the image (changing the {@link #setImageAddress(Object) address}
     * or the {@link #setImageQuery(AsyncDataQuery, Object) query}. That is,
     * after the given amount of time elapses and no image is received by the
     * image query, this component will be cleared with the currently set
     * background color.
     * <P>
     * The default value for this property is 1000 milliseconds.
     *
     * @param oldImageHideTime the time need to elapse before clearing this
     *   component after changing the image in the given time unit. This
     *   argument must be greater than or equal to zero.
     * @param timeUnit the time unit of the specified timeout value. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   not greater than or equal to zero
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public final void setOldImageHideTime(long oldImageHideTime, TimeUnit timeUnit) {
        ExceptionHelper.checkArgumentInRange(oldImageHideTime, 0, Long.MAX_VALUE, "oldImageHideTime");
        ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");

        this.oldImageHideTime = timeUnit.toNanos(oldImageHideTime);
    }

    /**
     * Returns the time since the source image of this component has been
     * changed in the given time unit. This can either occur due to changing the
     * {@link #setImageAddress(Object) image address} or due to changing the
     * {@link #setImageQuery(AsyncDataQuery, Object) image query}.
     *
     * @param timeunit the time unit in which to result is to be returned.
     *   This argument cannot be {@code null}.
     * @return the time since the source image of this component has been
     *   changed
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final long getTimeSinceImageChange(TimeUnit timeunit) {
        return timeunit.convert(System.nanoTime() - imageReplaceTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the time since the last rendering of this component in the given
     * time unit. This method only cares about image retrieval and image
     * transformation, simply calling {@code repaint} has no effect on this
     * method.
     *
     * @param timeunit the time unit in which to result is to be returned.
     *   This argument cannot be {@code null}.
     * @return the time since the last rendering of this component
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final long getTimeSinceLastImageShow(TimeUnit timeunit) {
        return timeunit.convert(System.nanoTime() - imageShownTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns {@code true} if the currently set image has been retrieved and
     * was rendered to this component. This method will return {@code true} even
     * if the image was only partially retrieved.
     *
     * @return {@code true} if the currently set image has been retrieved and
     *   was rendered to this component, {@code false} otherwise
     */
    public final boolean isCurrentImageShown() {
        return imageShown;
    }

    /**
     * Returns the address of the image, previously set by the
     * {@link #setImageAddress(Object) setImageAddress} method.
     *
     * @return the address of the image previously set by the
     *   {@link #setImageAddress(Object) setImageAddress} method. This method
     *   may return {@code null}, if {@code null} was set for the image address.
     */
    public final ImageAddress getImageAddress() {
        return currentImageAddress;
    }

    /**
     * Returns the image query, previously set by one of the
     * {@link #setImageQuery(AsyncDataQuery, Object) setImageQuery} methods.
     *
     * @return the image query, previously set by one of the
     *   {@link #setImageQuery(AsyncDataQuery, Object) setImageQuery} methods.
     *   This method may return {@code null} if no image query was set yet or
     *   {@code null} was set.
     */
    public final AsyncDataQuery<? super ImageAddress, ? extends org.jtrim.image.ImageData> getImageQuery() {
        return rawImageQuery;
    }

    /**
     * Sets the image query to be used to retrieve the image and sets the
     * {@link #setImageAddress(Object) image address} to {@code null}.
     * <P>
     * Calling this method is equivalent to calling
     * {@code setImageQuery(imageQuery, null)}. Therefore nothing will be
     * displayed in this component, to display the image set the image address
     * in a subsequent {@link #setImageAddress(Object) setImageAddress} method
     * call.
     * <P>
     * This method will call listeners
     * {@link #addImageAddressChangeListener(Runnable) registered} for
     * notifications of changes of the image address.
     *
     * @param imageQuery the image query to be used to retrieve the image
     *   displayed by this component. This argument can be {@code null}, in
     *   which case no image is displayed and no transformation is applied.
     */
    public final void setImageQuery(
            AsyncDataQuery<? super ImageAddress, ? extends org.jtrim.image.ImageData> imageQuery) {
        setImageQuery(imageQuery, null);
    }

    /**
     * Sets the image query to be used to retrieve the image and sets the
     * {@link #setImageAddress(Object) image address} to the given value.
     * <P>
     * This method will call listeners
     * {@link #addImageAddressChangeListener(Runnable) registered} for
     * notifications of changes of the image address.
     *
     * @param imageQuery the image query to be used to retrieve the image
     *   displayed by this component. This argument can be {@code null}, in
     *   which case no image is displayed and no transformation is applied. Also
     *   note that if this argument is {@code null}. the specified image address
     *   must also be {@code null}.
     * @param imageAddress the object to be passed to the image query to
     *   retrieve the image. This argument can be {@code null} but if it is
     *   {@code null}, no attempt will be made to retrieve and display the
     *   image: This component will be simply cleared with the current
     *   background color.
     *
     * @throws IllegalStateException thrown if the specified image query is
     *   {@code null} but the image address is not {@code null}
     */
    public final void setImageQuery(
            AsyncDataQuery<? super ImageAddress, ? extends org.jtrim.image.ImageData> imageQuery,
            ImageAddress imageAddress) {

        if (imageQuery == null && imageAddress != null) {
            throw new IllegalArgumentException("null image query cannot query images.");
        }

        this.currentImageAddress = imageAddress;
        this.rawImageQuery = imageQuery;
        this.imageQuery = imageQuery != null
                ? wrapQuery(imageQuery)
                : null;

        AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> newLink = null;

        if (imageQuery != null) {
            if (imageAddress != null) {
                newLink = this.imageQuery.createDataLink(imageAddress);
            }
        }

        setImageLink(newLink);
        fireImageAddressChange();
    }

    private static final class WeakRefAndID {
        public final WeakReference<org.jtrim.image.ImageData> imageRef;
        public final Object id;

        public WeakRefAndID(org.jtrim.image.ImageData image) {
            this.imageRef = new WeakReference<>(image);
            this.id = new Object();
        }
    }

    private AsyncDataQuery<ImageAddress, DataWithUid<org.jtrim.image.ImageData>> wrapQuery(
            final AsyncDataQuery<? super ImageAddress, ? extends org.jtrim.image.ImageData> query) {
        assert query != null;

        return new AsyncDataQuery<ImageAddress, DataWithUid<org.jtrim.image.ImageData>>() {
            @Override
            public AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> createDataLink(ImageAddress arg) {
                final AsyncDataLink<? extends org.jtrim.image.ImageData> link = query.createDataLink(arg);

                return new AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>>() {
                    private final AtomicReference<WeakRefAndID> cache = new AtomicReference<>(null);

                    private boolean clearCacheIfNeeded() {
                        WeakRefAndID ref = cache.get();
                        if (ref == null) {
                            return false;
                        }

                        if (ref.imageRef.get() == null) {
                            return cache.compareAndSet(ref, null);
                        }
                        else {
                            return false;
                        }
                    }

                    @Override
                    public AsyncDataController getData(
                            CancellationToken cancelToken,
                            final AsyncDataListener<? super DataWithUid<org.jtrim.image.ImageData>> dataListener) {
                        ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

                        return link.getData(cancelToken, new AsyncDataListener<org.jtrim.image.ImageData>() {
                            private WeakRefAndID refAndID = null;

                            @Override
                            public void onDataArrive(org.jtrim.image.ImageData data) {
                                WeakRefAndID currentCache = cache.get();
                                if (currentCache != null) {
                                    if (currentCache.imageRef.get() == data) {
                                        dataListener.onDataArrive(new DataWithUid<>(data, currentCache.id));
                                        return;
                                    }
                                }

                                refAndID = new WeakRefAndID(data);
                                dataListener.onDataArrive(new DataWithUid<>(data, refAndID.id));
                            }

                            private void addToCacheIfCan(AsyncReport report) {
                                if (report.isSuccess()) {
                                    boolean repeat;
                                    do {
                                        repeat = !cache.compareAndSet(null, refAndID);
                                        if (repeat) {
                                            repeat = clearCacheIfNeeded();
                                        }
                                    } while (repeat);
                                }
                            }

                            @Override
                            public void onDoneReceive(AsyncReport report) {
                                try {
                                    dataListener.onDoneReceive(report);
                                } finally {
                                    addToCacheIfCan(report);
                                }
                            }
                        });
                    }
                };
            }
        };
    }

    /**
     * Sets the address of the image to be displayed. This address is to be
     * passed to the previously set {@link #setImageQuery(AsyncDataQuery, Object) image query}.
     * <P>
     * This method will call listeners
     * {@link #addImageAddressChangeListener(Runnable) registered} for
     * notifications of changes of the image address.
     *
     * @param imageAddress the address of the image to be displayed. This
     *   argument can be {@code null} but if it is {@code null}, no attempt
     *   will be made to retrieve and display the image (even if the query
     *   supports {@code null} inputs).
     */
    public final void setImageAddress(ImageAddress imageAddress) {
        if (imageQuery == null && imageAddress != null) {
            throw new IllegalStateException("null image query cannot query images.");
        }
        currentImageAddress = imageAddress;

        AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> newLink = null;

        if (imageQuery != null && imageAddress != null) {
            newLink = imageQuery.createDataLink(imageAddress);
        }

        setImageLink(newLink);
        fireImageAddressChange();
    }

    /**
     * Registers a listener which is to be notified whenever the
     * source of the input image chages (i.e., the
     * {@link #setImageQuery(org.jtrim.concurrent.async.AsyncDataQuery) image query})
     * or a new meta-data information is available. The meta-data will change
     * after the image is attempted to be retrieved to be displayed.
     *
     * @param listener the listener whose methods is to be called when the image
     *   source or the meta-data changes. This argument cannot be {@code null}.
     * @return the {@code ListenerRef} object which can be used to unregister
     *   the currently added listener, so that it will no longer be notified
     *   of the changes. This method never returns {@code null}.
     */
    public final ListenerRef addImageListener(ImageListener listener) {
        return imageListeners.registerListener(listener);
    }

    /**
     * Registers a listener which is to be notified whenever the
     * {@link #setImageAddress(Object) address} passed to the underlying
     * {@link #setImageQuery(org.jtrim.concurrent.async.AsyncDataQuery) image query}
     * changes.
     *
     * @param listener the listener whose {@code onChangeAddress} method is to
     *   be called when the image address property changes. This argument cannot
     *   be {@code null}.
     * @return the {@code ListenerRef} object which can be used to unregister
     *   the currently added listener, so that it will no longer be notified
     *   of the changes. This method never returns {@code null}.
     */
    public final ListenerRef addImageAddressChangeListener(Runnable listener) {
        return addressChangeListeners.registerListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        setupRenderingArgs();
    }

    private void setRenderingArgs(
            final AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> imageLinkOfRendering,
            AsyncDataLink<InternalTransformerData> resultLink,
            final BasicRenderingArguments renderingArgs) {
        setRenderingArgs(resultLink, new ImageRenderer<InternalTransformerData, InternalPaintResult>() {
            @Override
            public RenderingResult<InternalPaintResult> startRendering(
                    CancellationToken cancelToken, BufferedImage drawingSurface) {
                return RenderingResult.noRendering();
            }

            @Override
            public boolean willDoSignificantRender(InternalTransformerData data) {
                return true;
            }

            @Override
            public RenderingResult<InternalPaintResult> render(
                    CancellationToken cancelToken, InternalTransformerData data, BufferedImage drawingSurface) {
                BufferedImage image = data.getImage();

                Graphics2D g = drawingSurface.createGraphics();
                try {
                    if (image != null) {
                        g.drawImage(image, 0, 0, null);
                    }
                    else {
                        g.setColor(renderingArgs.getBackgroundColor());
                        g.fillRect(0, 0,
                                drawingSurface.getWidth(),
                                drawingSurface.getHeight());
                    }
                } finally {
                    g.dispose();
                }

                if (data.getException() != null) {
                    onRenderingError(renderingArgs, drawingSurface, data.getException());
                }

                return RenderingResult.significant(new InternalPaintResult(
                        data.isReceivedImage(),
                        data.getPointTransformer(),
                        data.getMetaData(),
                        imageLinkOfRendering));
            }

            @Override
            public RenderingResult<InternalPaintResult> finishRendering(
                    CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                return RenderingResult.noRendering();
            }
        }, new PaintHook<InternalPaintResult>() {
            @Override
            public boolean prePaintComponent(RenderingState state, Graphics2D g) {
                // We do this to fill the possible remainder of this component
                // with the background color until the asynchronous renderer
                // fills the gap on size change.
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                return true;
            }

            @Override
            public void postPaintComponent(RenderingState state, InternalPaintResult renderingResult, Graphics2D g) {
                // This means that the image address was set to null, so we must
                // simply clear this component.
                if (imageLink == null) {
                    prePaintComponent(state, g);
                }
                postRendering(state, renderingResult, g);
            }
        });
    }

    private void setupRenderingArgs() {
        final BasicRenderingArguments renderingArgs = new BasicRenderingArguments(this);
        AsyncDataLink<InternalTransformerData> resultLink = null;

        if (imageLink != null) {
            InternalRenderingData renderingData;
            renderingData = new InternalRenderingData(getWidth(), getHeight());

            AsyncDataLink<DataWithUid<InternalTransformerData>> currentLink;
            currentLink = AsyncLinks.convertResult(imageLink,
                    new ImageResultConverter(renderingData));

            for (CachedQuery transformer: imageTransformers.values()) {
                currentLink = AsyncLinks.convertResult(currentLink, transformer);
            }

            resultLink = AsyncLinks.removeUidFromResult(currentLink);
        }
        setRenderingArgs(imageLink, resultLink, renderingArgs);
    }

    /**
     * Clears the passed {@code Graphics2D} object with currently specified
     * background color and does some other bookkeeping required by this
     * component.
     *
     * @param state the state of the current asynchronous rendering, or
     *   {@code null} if there is no rendering in progress
     * @param g the {@code Graphics2D} object to be cleared with the background
     *   color. This argument cannot be {@code null}.
     */
    @Override
    protected final void paintDefault(RenderingState state, Graphics2D g) {
        super.paintDefault(state, g);

        postRendering(state, null, g);
    }

    private void postRendering(RenderingState state, InternalPaintResult renderingResult, Graphics2D g) {
        postRenderingAction(renderingResult);

        if (isLongRendering()) {
            postLongRendering(g, state);
        }

        checkLongRendering(state);
    }

    private void postLongRendering(Graphics2D g, RenderingState state) {
        if (!isCurrentImageShown()
                && getTimeSinceLastImageShow(TimeUnit.NANOSECONDS) > getOldImageHideNanos()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        g.setColor(getForeground());
        g.setFont(getFont());
        g.setBackground(getBackground());

        AsyncDataState dataState = state != null ? state.getAsyncDataState() : null;
        MultiAsyncDataState states = dataState instanceof MultiAsyncDataState
                ? (MultiAsyncDataState)dataState
                : new MultiAsyncDataState(dataState);

        displayLongRenderingState(g, states, getImageMetaData());
    }

    private void postRenderingAction(InternalPaintResult renderingResult) {
        if (renderingResult != null && renderingResult.getImageLink() == imageLink) {
            if (!imageShown) {
                imageShown = renderingResult.isImageReceived();
            }

            if (imageShown) {
                imageShownTime = System.nanoTime();
            }

            if (!metaDataCompleted) {
                ImageMetaData newMetaData = renderingResult.getMetaData();
                if (newMetaData != null) {
                    imageMetaData = newMetaData;
                    metaDataCompleted = newMetaData.isComplete();
                    imageListeners.onEvent(metaDataHandler, null);
                }
            }

            ImagePointTransformer currentPointTransformer;
            currentPointTransformer = renderingResult.getPointTransformer();
            if (currentPointTransformer != null) {
                displayedPointTransformer = currentPointTransformer;
            }
        }
    }

    private boolean isLongRendering() {
        if (!needLongRendering) {
            return false;
        }
        if (!isRendering()) {
            return false;
        }
        return getSignificantRenderingTime(TimeUnit.NANOSECONDS) >= renderingPatienceNanos;
    }

    private void checkLongRendering(RenderingState state) {
        if (state == null || state.isRenderingFinished()) {
            return;
        }

        if (!isLongRendering()) {
            startLongRenderingListener();
        }
    }

    private void startLongRenderingListener() {
        if (!needLongRendering) {
            return;
        }
        if (!isDisplayable()) {
            return;
        }

        javax.swing.Timer timer;
        timer = new javax.swing.Timer(RENDERING_STATE_POLL_TIME_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void fireImageAddressChange() {
        EventListeners.dispatchRunnable(addressChangeListeners);
    }

    private void setImageLink(AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> imageLink) {
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
            setupRenderingArgs();
        }
    }

    /**
     * Removes every image transformations previously set by any of the
     * {@link #setImageTransformer(int, ReferenceType, ObjectCache, AsyncDataQuery) setImageTransformer}
     * method calls. After this method call, no transformation will be applied
     * to the retrieved image and it will be displayed as is.
     */
    public void clearImageTransformers() {
        imageTransformers.clear();
        setupRenderingArgs();
    }

    /**
     * Removes a previously specified
     * {@link #setImageTransformer(int, ReferenceType, ObjectCache, AsyncDataQuery) image transformer}
     * from the given index. Transformations on different index will be left
     * untouched. If there is no transformation on the specified index, this
     * method does nothing.
     *
     * @param index the index from which the transformation is to be removed
     */
    public void removeImageTransformer(int index) {
        imageTransformers.remove(index);
        setupRenderingArgs();
    }

    /**
     * Sets the image transformer at the given index. This method replaces the
     * image transformer previously set on the same index.
     * <P>
     * Image transformers are defined by an {@link AsyncDataQuery} taking an
     * {@link ImageTransformerData} for inputs and produces an
     * {@code ImageTransformerData} as well. These image transformers are linked
     * one after another each transforming the result of the previous image
     * transformer. The order in which these image transformers are applied is
     * specified by the index of the image transformer.
     * <P>
     * <B>Note</B>: Do not attempt to cache the result of the passed image
     * transformer, instead rely on the built in caching mechanism which is
     * implemented in a way that it does not need to hold a hard reference to
     * the input of the transformation.
     *
     * @param index the index of the currently added image transformer. Image
     *   transformers with lesser index are applied first. This argument is
     *   allowed to be any valid {@code int} value (including negative
     *   integers).
     * @param refType the reference type used to reference the cached result
     *   of the image transformation. The cache used is
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE}. If you do not need
     *   caching pass {@link ReferenceType#NoRefType}. This argument cannot be
     *   {@code null}.
     * @param imageTransformer the image transformer to be applied. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the {@code refType} or the image
     *   transformer is {@code null}
     *
     * @see #setImageTransformer(int, ReferenceType, ObjectCache, AsyncDataQuery)
     */
    public void setImageTransformer(int index,
            ReferenceType refType,
            AsyncDataQuery<ImageTransformerData, TransformedImageData> imageTransformer) {
        setImageTransformer(index, refType, null, imageTransformer);
    }

    /**
     * Sets the image transformer at the given index using the specified cache.
     * This method replaces the image transformer previously set on the same
     * index.
     * <P>
     * Image transformers are defined by an {@link AsyncDataQuery} taking an
     * {@link ImageTransformerData} for inputs and produces an
     * {@code ImageTransformerData} as well. These image transformers are linked
     * one after another each transforming the result of the previous image
     * transformer. The order in which these image transformers are applied is
     * specified by the index of the image transformer.
     * <P>
     * <B>Note</B>: Do not attempt to cache the result of the passed image
     * transformer, instead rely on the built in caching mechanism which is
     * implemented in a way that it does not need to hold a hard reference to
     * the input of the transformation.
     *
     * @param index the index of the currently added image transformer. Image
     *   transformers with lesser index are applied first. This argument is
     *   allowed to be any valid {@code int} value (including negative
     *   integers).
     * @param refType the reference type used to reference the cached result
     *   of the image transformation. The cache used is
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE}. If you do not need
     *   caching pass {@link ReferenceType#NoRefType}. This argument cannot be
     *   {@code null}.
     * @param refCreator the {@code ObjectCache} used to cache the result of
     *   the image transformation. This argument can be {@code null}, in which
     *   case {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used.
     * @param imageTransformer the image transformer to be applied. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the {@code refType} or the image
     *   transformer is {@code null}
     *
     * @see #setImageTransformer(int, ReferenceType, ObjectCache, AsyncDataQuery)
     */
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

        setupRenderingArgs();
    }

    /**
     * Returns the coordinate transformation between image coordinates and
     * display coordinates according to the currently displayed image.
     * <P>
     * The source coordinate system of the returned transformation is the
     * coordinate system of the image and destination coordinate system is the
     * coordinate system of the display. That is, transforming a coordinate from
     * the source coordinates to the destination coordinate will transform
     * the location of a pixel on the source image to the location of that pixel
     * on this component. Note that the result may lay outside this component's
     * bounds.
     * <P>
     * This method always return the coordinate transformation according what
     * is currently displayed on this component which may differ from the one
     * which could be deduced from the currently set properties.
     *
     * @return the coordinate transformation between image coordinates and
     *   display coordinates according to the currently displayed image or
     *   {@code null} if the transformation is not (yet) available
     */
    public final ImagePointTransformer getDisplayedPointTransformer() {
        return displayedPointTransformer;
    }

    /**
     * Returns the coordinate transformation between image coordinates and
     * display coordinates according to the currently set properties or if
     * it cannot be calculated, according to the displayed image.
     * <P>
     * The source coordinate system of the returned transformation is the
     * coordinate system of the image and destination coordinate system is the
     * coordinate system of the display. That is, transforming a coordinate from
     * the source coordinates to the destination coordinate will transform
     * the location of a pixel on the source image to the location of that pixel
     * on this component. Note that the result may lay outside this component's
     * bounds.
     * <P>
     * This method is intended to be overridden in subclasses which might be
     * able to calculate the transformations based on other properties and does
     * not have to wait until the image is rendered. The default implementation
     * simply calls the {@link #getDisplayedPointTransformer()} method.
     *
     * @return the coordinate transformation between image coordinates and
     *   display coordinates according to the currently set properties or if
     *   it cannot be calculated, according to the displayed image or
     *   {@code null} if the transformation is not (yet) available
     */
    public ImagePointTransformer getPointTransformer() {
        return getDisplayedPointTransformer();
    }

    /**
     * Called when the rendering is still in progress and a given
     * {@link #setLongRenderingTimeout(long, TimeUnit) timeout} elapsed. This
     * method may update the display with addition information. Note however,
     * that this method is called on the AWT Event Dispatch Thread and as such,
     * should not do expensive computations.
     * <P>
     * This method may be overridden in subclasses. The default implementation
     * display some information of the image to be loaded and the current
     * progress.
     *
     * @param g the {@code Graphics2D} to which this method need to draw to.
     *   This argument cannot be {@code null}. This method does not need to
     *   preserve the graphic context.
     * @param dataStates the current state of the image retrieving and rendering
     *   process. This state contains the state of the image retrieval process
     *   and all the applied transformations. This argument can never be
     *   {@code null} but can contain {@code null} states.
     * @param imageMetaData the meta data of the image currently being
     *   displayed. This argument can be {@code null} if the meta data is not
     *   available.
     */
    protected void displayLongRenderingState(Graphics2D g,
            MultiAsyncDataState dataStates, ImageMetaData imageMetaData) {
        int stateCount = dataStates.getSubStateCount();

        if (stateCount > 0) {
            StringBuilder message = new StringBuilder(DEFAULT_STRING_BUILDER_LENGTH);
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

    /**
     * Called when an exception occurred while trying to retrieve the image
     * or during rendering the image. This method may update the specified
     * drawing surface.
     * <P>
     * This method is called in the context of the {@link AsyncRenderer} of this
     * component and may do some more expensive computation without blocking the
     * input of the user.
     * <P>
     * This method may be overridden in subclasses. The default implementation
     * displays the exception message in the upper left corner of the drawing
     * surface (which is the upper left corner of this component).
     *
     * @param renderingArgs the properties of this component at the time when
     *   the rendering has been requested. This argument cannot be {@code null}.
     * @param drawingSurface the {@code BufferedImage} which needs to be updated
     *   to display the error. This argument cannot be {@code null}.
     * @param exception the exception describing the reason of failure. Note
     *   that, this exception might have causes (and suppressed exceptions)
     *   which need to be inspected to fully understand the causes.
     */
    protected void onRenderingError(
            BasicRenderingArguments renderingArgs,
            BufferedImage drawingSurface,
            org.jtrim.image.ImageReceiveException exception) {

        Graphics2D g = drawingSurface.createGraphics();
        try {
            g.setColor(renderingArgs.getBackgroundColor());
            g.fillRect(0, 0, drawingSurface.getWidth(), drawingSurface.getHeight());

            g.setColor(renderingArgs.getForegroundColor());
            g.setFont(renderingArgs.getFont());

            StringBuilder errorText = new StringBuilder(DEFAULT_STRING_BUILDER_LENGTH);
            errorText.append("Error: ");
            errorText.append(exception.getMessage());

            RenderHelper.drawMessage(g, errorText.toString());
        } finally {
            g.dispose();
        }
    }

    private class CachedQuery
    implements
            AsyncDataQuery<DataWithUid<InternalTransformerData>, DataWithUid<InternalTransformerData>> {

        private final AsyncDataQuery<
                CachedLinkRequest<DataWithUid<ImageTransformerData>>,
                DataWithUid<TransformedImageData>> cachedTransformerQuery;

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

            org.jtrim.image.ImageReceiveException prevException = arg.getException();
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
            StringBuilder result = new StringBuilder(DEFAULT_STRING_BUILDER_LENGTH);
            result.append("Extract image transformer input then use ");
            AsyncFormatHelper.appendIndented(cachedTransformerQuery, result);
            result.append("\nConvert results to AsyncImageDisplay.InternalFormat");

            return result.toString();
        }
    }

    /** @deprecated  */
    @Deprecated
    private class ToInternalConverterLink
    implements
            DataConverter<DataWithUid<TransformedImageData>,
            DataWithUid<InternalTransformerData>> {

        private final ImagePointTransformer prevPointTransformer;
        private final org.jtrim.image.ImageReceiveException prevException;
        private final ImageMetaData metaData;
        private final InternalRenderingData renderingData;
        private final boolean receivedImage;

        public ToInternalConverterLink(
                ImagePointTransformer prevPointTransformer,
                org.jtrim.image.ImageReceiveException prevException,
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

            org.jtrim.image.ImageReceiveException transfException;
            transfException = resultImageData != null
                    ? resultImageData.getException()
                    : null;

            org.jtrim.image.ImageReceiveException exception;

            if (prevException == null) {
                exception = transfException;
            }
            else if (transfException == null) {
                exception = prevException;
            }
            else {
                exception = new org.jtrim.image.ImageReceiveException();
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

    /** @deprecated AsyncImageDisplay is deprecated. */
    @Deprecated
    private static class ImageResultConverter
    implements
            DataConverter<DataWithUid<org.jtrim.image.ImageData>,
            DataWithUid<InternalTransformerData>> {

        private final InternalRenderingData renderingData;

        public ImageResultConverter(InternalRenderingData renderingData) {
            this.renderingData = renderingData;
        }

        @Override
        public DataWithUid<InternalTransformerData> convertData(DataWithUid<org.jtrim.image.ImageData> data) {
            org.jtrim.image.ImageData imageData = data.getData();

            InternalTransformerData newData;
            if (imageData != null) {
                newData = new InternalTransformerData(
                        new TransformedImage(imageData.getImage(), null),
                        imageData.getMetaData(),
                        imageData.getException(),
                        renderingData,
                        imageData.getImage() != null);
            }
            else {
                newData = new InternalTransformerData(
                        null,
                        null,
                        null,
                        renderingData,
                        false);
            }
            return new DataWithUid<>(newData, data.getID());
        }

        @Override
        public String toString() {
            return "Image -> AsyncImageDisplay.InternalFormat";
        }
    }

    private static class InternalPaintResult {
        private final boolean imageReceived;
        private final ImagePointTransformer pointTransformer;
        private final ImageMetaData metaData;
        private final AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> imageLink;

        public InternalPaintResult(boolean imageReceived,
                ImagePointTransformer pointTransformer,
                ImageMetaData metaData,
                AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> imageLink) {
            this.imageReceived = imageReceived;
            this.pointTransformer = pointTransformer;
            this.metaData = metaData;
            this.imageLink = imageLink;
        }

        public AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> getImageLink() {
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

        public InternalRenderingData(int destWidth, int destHeight) {
            this.destWidth = destWidth;
            this.destHeight = destHeight;
        }

        public int getDestHeight() {
            return destHeight;
        }

        public int getDestWidth() {
            return destWidth;
        }
    }

    private static class InternalTransformerData  {
        private final InternalRenderingData renderingData;
        private final ImageMetaData metaData;
        private final boolean receivedImage;
        private final TransformedImageData transformedImageData;

        public InternalTransformerData(
                TransformedImage transformedImage,
                ImageMetaData metaData,
                org.jtrim.image.ImageReceiveException exception,
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

        public int getDestWidth() {
            return renderingData.getDestWidth();
        }

        public int getDestHeight() {
            return renderingData.getDestHeight();
        }

        public org.jtrim.image.ImageReceiveException getException() {
            return transformedImageData.getException();
        }

        public ImagePointTransformer getPointTransformer() {
            return transformedImageData.getPointTransformer();
        }

        public BufferedImage getImage() {
            return transformedImageData.getImage();
        }
    }

    private static TransformedImage changePointTransformer(
            ImagePointTransformer prevPointTransformer,
            TransformedImage image) {

        if (prevPointTransformer == null || image == null) {
            return image;
        }

        ImagePointTransformer newPointTransformer = SerialImagePointTransformer.combine(
                prevPointTransformer,
                image.getPointTransformer());

        return new TransformedImage(image.getImage(), newPointTransformer);
    }

    /** @deprecated AsyncImageDisplay is deprecated. */
    @Deprecated
    private class ImageChangeHandler implements EventDispatcher<ImageListener, Void> {
        @Override
        public void onEvent(ImageListener eventArgument, Void arg) {
            AsyncDataLink<DataWithUid<org.jtrim.image.ImageData>> currentLink = imageLink;
            eventArgument.onChangeImage(currentLink != null
                    ? AsyncLinks.removeUidFromResult(currentLink)
                    : null);
        }
    }

    /** @deprecated AsyncImageDisplay is deprecated. */
    @Deprecated
    private class MetaDataHandler implements EventDispatcher<ImageListener, Void> {
        @Override
        public void onEvent(ImageListener eventArgument, Void arg) {
            eventArgument.onReceiveMetaData(imageMetaData);
        }
    }
}
