package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jtrim.cache.ReferenceType;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataConverter;
import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.transform.*;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a <I>Swing</I> component which is able to display an image, applying
 * a series of user defined transformations and convenience methods to apply
 * basic image transformations. This component supports every transformation
 * of the {@link BasicTransformationModel}. These basic transformations are
 * applied on transformation index: 0. Other transformations might be applied
 * before and after these transformations by calling any of the
 * {@link #setImageTransformer(int, ReferenceType, ObjectCache, AsyncDataQuery) setImageTransformer}
 * method with the appropriate transformation index.
 * <P>
 * Additionally this component is able to display the image using various
 * {@link #setInterpolationTypes(InterpolationType[]) interpolations} and even
 * using multiple interpolations at once. If multiple interpolations are used,
 * they will be used to render the image in the requested order. This can be
 * beneficial when using a fast interpolation first and a slow but better
 * interpolation last. With this settings when rapidly changing the
 * transformation properties of this component, the fast interpolation will be
 * displayed to the user while it calculates the better interpolated image.
 * <P>
 * This component defines the {@link #setOffset(double, double) offset} of the
 * image so that if it is (0.0, 0.0), the center of the image will be in the
 * center of the component.
 * <P>
 * <B>Note</B>: You should set the {@link TaskExecutorService} used to render
 * the image by calling {@link #setDefaultExecutor(TaskExecutorService) setDefaultExecutor}.
 * You may also use different {@code TaskExecutorService} instances for
 * different interpolation types by calling the
 * {@link #setExecutor(InterpolationType, TaskExecutorService) setExecutor}
 * method. Using distinct executors for different interpolation type is
 * advantageous in most cases because it prevents slow interpolations from
 * blocking fast interpolations.
 * <P>
 * About other properties of this component see the description of
 * {@link AsyncImageDisplay}.
 * <P>
 * <B>This class is highly experimental and may see significant changes in
 * the future.</B>
 * <P>
 * The thread-safety property of this component is the same as with any other
 * <I>Swing</I> components. That is, instances of this class can be accessed
 * only from the AWT Event Dispatch Thread after made displayable.
 *
 * @param <ImageAddressType> the type of the address of the image to be
 *   displayed. That is, the input of the
 *   {@link #setImageQuery(AsyncDataQuery, Object) image query}.
 *
 * @see AsyncImageDisplay
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial") // Not serializable
public class SimpleAsyncImageDisplay<ImageAddressType> extends AsyncImageDisplay<ImageAddressType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private TaskExecutorService defaultExecutor;
    private final BasicTransformationModel transformations;
    private boolean alwaysClearZoomToFit;
    // Tasks to set arguments affecting painting of this component
    // should be executed by this executor for better performance.
    private final UpdateTaskExecutor argUpdater;

    private int lastTransformationIndex;
    private int lastTransformationCount;
    private InterpolationType[] interpolationTypes;
    private final Map<InterpolationType, TaskExecutorService> executors;

    /**
     * Creates a {@link SimpleAsyncImageDisplay} with identity transformation
     * applying bilinear transformations (the interpolation type only matters
     * if the transformation is changed to something else than identity).
     * <P>
     * The {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer}
     * method must be called before displaying the component.
     */
    public SimpleAsyncImageDisplay() {
        this(null);
    }

    /**
     * Creates a {@link SimpleAsyncImageDisplay} with identity transformation
     * applying bilinear transformations (the interpolation type only matters
     * if the transformation is changed to something else than identity).
     *
     * @param asyncRenderer the {@code AsyncRendererFactory} to be used to
     *   render this component. This argument can be {@code null}, in which
     *   case, the {@code AsyncRendererFactory} must be set later by the
     *   {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} method.
     */
    public SimpleAsyncImageDisplay(AsyncRendererFactory asyncRenderer) {
        super(asyncRenderer);

        this.alwaysClearZoomToFit = false;
        this.transformations = new BasicTransformationModel();
        this.defaultExecutor = SyncTaskExecutor.getDefaultInstance();
        this.argUpdater = new SwingUpdateTaskExecutor(true);

        this.lastTransformationIndex = 0;
        this.lastTransformationCount = 0;
        this.interpolationTypes = new InterpolationType[]{
            InterpolationType.BILINEAR
        };
        this.executors = new EnumMap<>(InterpolationType.class);
        this.defaultExecutor = SyncTaskExecutor.getDefaultInstance();

        this.transformations.addChangeListener(new Runnable() {
            @Override
            public void run() {
                setTransformations();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setTransformations();
            }
        });

        this.transformations.addTransformationListener(new TransformationListener() {
            @Override
            public void zoomChanged() {
            }

            @Override
            public void offsetChanged() {
            }

            @Override
            public void flipChanged() {
            }

            @Override
            public void rotateChanged() {
            }

            @Override
            public void enterZoomToFitMode(Set<ZoomToFitOption> options) {
                ImageMetaData imageMetaData = getImageMetaData();
                if (imageMetaData != null) {
                    int imageWidth = imageMetaData.getWidth();
                    int imageHeight = imageMetaData.getHeight();

                    if (imageWidth > 0 && imageHeight > 0) {
                        int currentWidth = getWidth();
                        int currentHeight = getHeight();
                        BasicImageTransformations newTransformations;
                        newTransformations = ZoomToFitTransformer.getBasicTransformations(
                                imageWidth,
                                imageHeight,
                                currentWidth,
                                currentHeight,
                                options,
                                getTransformations());

                        transformations.setTransformations(newTransformations);
                    }
                }
            }

            @Override
            public void leaveZoomToFitMode() {
            }
        });
    }

    /**
     * Returns the {@code TaskExecutorService} last set by the
     * {@link #setDefaultExecutor(TaskExecutorService) setDefaultExecutor}
     * method.
     * <P>
     * If {@code setDefaultExecutor} has not been called yet, this method
     * returns an executor which executes tasks synchronously on the calling
     * thread.
     *
     * @return the {@code TaskExecutorService} last set by the
     *   {@link #setDefaultExecutor(TaskExecutorService) setDefaultExecutor}
     *   method. This method never returns {@code null}.
     */
    public final TaskExecutorService getDefaultExecutor() {
        return defaultExecutor;
    }

    /**
     * Sets the {@code TaskExecutorService} to render the image unless there
     * was another {@code TaskExecutorService} specified for the currently
     * applied interpolation type.
     * <P>
     * Note: You should always consider using different executors for different
     * interpolation types.
     *
     * @param executor the {@code TaskExecutorService} to render the image
     *   unless there was another {@code TaskExecutorService} specified for the
     *   currently applied interpolation type. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument was
     *   {@code null}
     */
    public final void setDefaultExecutor(TaskExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        defaultExecutor = executor;
    }

    /**
     * Sets the executor used to render the image when applying the specified
     * interpolation type.
     * <P>
     * Note: You should always consider using different executors for different
     * interpolation types.
     *
     * @param interpolationType the interpolation type when the specified
     *   executor need to be used. This argument cannot be {@code null}.
     * @param executor the {@code TaskExecutorService} to be used when applying
     *   the specified interpolation type. This argument can be {@code null},
     *   in which case the {@link #getDefaultExecutor() default executor} will
     *   be used.
     *
     * @throws NullPointerException thrown if the specified interpolation type
     *   is {@code null}
     */
    public final void setExecutor(InterpolationType interpolationType,
            TaskExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");

        if (executor == null) {
            removeExecutor(interpolationType);
        }
        else {
            executors.put(interpolationType, executor);
        }
    }

    /**
     * Specifies that the {@link #getDefaultExecutor() default executor} need
     * to be used to render the image when applying the specified interpolation
     * type.
     *
     * @param interpolationType the interpolation type for which the default
     *   executor is to be used. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified interpolation type
     *   is {@code null}
     */
    public final void removeExecutor(InterpolationType interpolationType) {
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");
        executors.remove(interpolationType);
    }

    /**
     * Sets the interpolation types to be applied to the transformed image.
     * Multiple interpolation types can be specified and they will be applied
     * in the specified order. When specifying multiple interpolations, you
     * should specify the fastest one first and the most accurate one last.
     * <P>
     * Eventually only the image rendered with the last specified interpolation
     * type will be displayed to the user but until the last interpolation is
     * applied, the image with the previous interpolation is displayed to user.
     *
     * @param interpolationTypes the interpolation types in the order they need
     *   to be applied. This argument cannot be {@code null} an cannot contain
     *   {@code null} elements.
     *
     * @throws NullPointerException thrown if the specified array or any of its
     *   element is {@code null}
     */
    public final void setInterpolationTypes(InterpolationType... interpolationTypes) {
        ExceptionHelper.checkArgumentInRange(interpolationTypes.length,
                1, Integer.MAX_VALUE, "interpolationTypes.length");
        ExceptionHelper.checkNotNullElements(interpolationTypes, "interpolationTypes");

        InterpolationType prevLastType
                = this.interpolationTypes[this.interpolationTypes.length - 1];

        this.interpolationTypes = interpolationTypes.clone();

        if (prevLastType != interpolationTypes[interpolationTypes.length - 1]) {
            setTransformations();
        }
    }

    /**
     * Returns {@code true} if the image will be scaled to fit this component.
     * That is, this method returns the same value as the following expression:
     * {@code getZoomToFitOptions() != null}.
     * <P>
     * If the zoom to fit requirement conflicts with any of the properties
     * of this component, the zoom to fit mode will take precedence.
     *
     * @return {@code true} if the image will be scaled to fit the display in
     *   which it is displayed, {@code false} if it should be displayed
     *   according to the other properties of this component
     */
    public final boolean isInZoomToFitMode() {
        return transformations.isInZoomToFitMode();
    }

    /**
     * Returns the set of rules to be used when displaying the image
     * in zoom to fit mode or {@code null} if other properties of this
     * component will be used instead.
     *
     * @return the set of rules which will be used when displaying the image
     *   in zoom to fit mode or {@code null} if other properties of this
     *   component will be used instead
     */
    public final Set<ZoomToFitOption> getZoomToFitOptions() {
        return transformations.getZoomToFitOptions();
    }

    /**
     * Returns {@code true} if this component will leave the zoom to fit mode
     * when a transformation property of this component is modified even if
     * setting the property does not conflict with the zoom to fit mode (such as
     * rotate).
     *
     * @return {@code true} if this component will leave the zoom to fit mode
     *   when a transformation property of this component is modified even if
     *   setting the property does not conflict with the zoom to fit mode,
     *   {@code false} otherwise
     */
    public final boolean isAlwaysClearZoomToFit() {
        return alwaysClearZoomToFit;
    }

    /**
     * Sets if this component need to leave the zoom to fit mode
     * when a transformation property of this component is modified even if
     * setting the property does not conflict with the zoom to fit mode (such as
     * rotate) or not.
     *
     * @param alwaysClearZoomToFit if {@code true}, this component will leave
     *   the zoom to fit mode whenever an image transformation property of
     *   this component is changed. If {@code false}, this component will only
     *   leave the zoom to fit mode if a property is changed which conflicts
     *   with the zoom to fit mode.
     */
    public final void setAlwaysClearZoomToFit(boolean alwaysClearZoomToFit) {
        this.alwaysClearZoomToFit = alwaysClearZoomToFit;
    }

    /**
     * Adds a listener whose appropriate method will be called when a property
     * related to transforming the image displayed in this component changes.
     *
     * @param listener the listener whose appropriate method will be called
     *   when a property changes. This argument cannot be {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public final ListenerRef addTransformationListener(TransformationListener listener) {
        return transformations.addTransformationListener(listener);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method is able to return the coordinate
     * transformations according to the currently set properties if the
     * meta-data of the image to be displayed is already available. Otherwise,
     * this method will fall-back to the
     * {@link #getDisplayedPointTransformer() getDisplayedPointTransformer()}
     * method.
     */
    @Override
    public ImagePointTransformer getPointTransformer() {
        ImageMetaData metaData = getImageMetaData();
        if (metaData != null) {
            int srcWidth = metaData.getWidth();
            int srcHeight = metaData.getHeight();

            int destWidth = getWidth();
            int destHeight = getHeight();

            BasicImageTransformations transf = transformations.getTransformations();
            AffineTransform transfMatrix = AffineImageTransformer.getTransformationMatrix(
                    transf, srcWidth, srcHeight, destWidth, destHeight);
            return new AffineImagePointTransformer(transfMatrix);
        }
        else {
            return getDisplayedPointTransformer();
        }
    }

    /**
     * Returns where the pixel of the image is displayed in this component.
     * The result of (0, 0) means the top-left corner of this component. Note
     * that this method may return a coordinate which lies outside this
     * component and does not need to return integer coordinates.
     * <P>
     * This is simply a convenience method which relies on the
     * {@link #getPointTransformer() getPointTransformer()} method except that
     * if the coordinate transformation is not yet available this method will
     * return the same coordinates as specified in the argument.
     *
     * @param imagePoint the coordinates of the pixel on the source image to be
     *   transformed to display coordinates. This argument cannot be
     *   {@code null} but the coordinates may lay outside the boundaries of the
     *   image.
     * @return the coordinates where the specified pixel of the image is
     *   displayed. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified coordinate is
     *   {@code null}
     */
    public final Point2D getDisplayPoint(Point2D imagePoint) {
        ExceptionHelper.checkNotNullArgument(imagePoint, "imagePoint");

        ImagePointTransformer pointTransformer = getPointTransformer();

        Point2D result = new Point2D.Double();

        if (pointTransformer != null) {
            pointTransformer.transformSrcToDest(imagePoint, result);
        }
        else {
            result.setLocation(imagePoint);
        }

        return result;
    }

    /**
     * Returns which pixel of the source image is displayed at the given
     * coordinate. The result of (0, 0) means the top-left corner of the image.
     * Note that this method may return a coordinate which lies outside the
     * boundaries of the image.
     * <P>
     * This is simply a convenience method which relies on the
     * {@link #getPointTransformer() getPointTransformer()} method except that
     * if the coordinate transformation is not yet available this method will
     * return the same coordinates as specified in the argument.
     *
     * @param displayPoint the coordinates of the display to be converted to
     *   image coordinates. This argument cannot be {@code null} but the
     *   coordinates may lay outside the boundaries of the display. The
     *   coordinates (0, 0) specifies the top-left corner of this component.
     * @return the coordinates of the pixel of the source image at the given
     *   display coordinate. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified coordinate is
     *   {@code null}
     */
    public final Point2D getImagePoint(Point2D displayPoint) {
        ImagePointTransformer pointTransformer = getPointTransformer();

        Point2D result = new Point2D.Double();

        if (pointTransformer != null) {
            try {
                pointTransformer.transformDestToSrc(displayPoint, result);
            } catch (NoninvertibleTransformException ex) {
                result.setLocation(displayPoint);
            }
        }
        else {
            result.setLocation(displayPoint);
        }

        return result;
    }

    /**
     * Sets the {@link #setOffset(double, double) offset} of the displayed
     * image, so that the given pixel of the image will be displayed above
     * the given coordinates of this component.
     * <P>
     * If the coordinate transformation is not ye available (i.e.,
     * {@link #getPointTransformer() getPointTransformer()} returns {@code null}),
     * this method is a no-op.
     * <P>
     * Note that if this method ends up setting the offset of the displayed
     * image, it will clear the zoom to fit mode (i.e.: calls
     * {@code clearZoomToFit()}).
     *
     * @param imagePoint the coordinates of the image to be displayed at the
     *   specified position. This argument cannot be {@code null}. These
     *   coordinates may lay outside the boundaries of the image.
     * @param displayPoint the coordinates of this component where the specified
     *   pixel of the source image need to be displayed. These coordinates
     *   may lay outside the boundaries of this component.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public final void moveImagePointToDisplayPoint(Point2D imagePoint, Point2D displayPoint) {
        ExceptionHelper.checkNotNullArgument(imagePoint, "imagePoint");
        ExceptionHelper.checkNotNullArgument(displayPoint, "displayPoint");

        BasicImageTransformations transf = getTransformations();
        ImagePointTransformer pointTransformer = getPointTransformer();

        if (pointTransformer != null) {
            double offsetErrX;
            double offsetErrY;

            Point2D currentDisplayPoint = new Point2D.Double();
            pointTransformer.transformSrcToDest(imagePoint, currentDisplayPoint);

            offsetErrX = displayPoint.getX() - currentDisplayPoint.getX();
            offsetErrY = displayPoint.getY() - currentDisplayPoint.getY();

            double newOffsetX = transf.getOffsetX() + offsetErrX;
            double newOffsetY = transf.getOffsetY() + offsetErrY;

            setOffset(newOffsetX, newOffsetY);
        }
    }

    /**
     * Returns the {@code TaskExecutorService} which will currently be used
     * for transforming using the given interpolation type. If there was
     * no executor set for the specified interpolation type, this method
     * returns the {@link #getDefaultExecutor() default executor}.
     *
     * @param interpolationType the interpolation type whose executor is to be
     *   returned. This argument cannot be {@code null}.
     * @return the {@code TaskExecutorService} which will currently be used
     *   for transforming using the given interpolation type. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified interpolation type
     *   is {@code null}
     */
    public final TaskExecutorService getExecutor(InterpolationType interpolationType) {
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");

        TaskExecutorService executor = executors.get(interpolationType);
        return executor != null ? executor : defaultExecutor;
    }

    private void setTransformations() {
        argUpdater.execute(new Runnable() {
            @Override
            public void run() {
                prepareTransformations(0, getBackground());
            }
        });
    }

    private void clearLastTransformations() {
        int endIndex = lastTransformationIndex + lastTransformationCount;

        for (int i = lastTransformationIndex; i < endIndex; i++) {
            removeImageTransformer(i);
        }

        lastTransformationCount = 0;
    }

    private void prepareTransformations(int index, Color bckgColor) {
        if (lastTransformationIndex != index || lastTransformationCount != 1) {
            clearLastTransformations();
        }

        setCurrentTransformations(index, bckgColor);

        lastTransformationIndex = index;
        lastTransformationCount = 1;
    }

    private void setCurrentTransformations(int index, Color bckgColor) {
        final BasicImageTransformations currentTransf = transformations.getTransformations();

        Set<ZoomToFitOption> zoomToFit = transformations.getZoomToFitOptions();
        if (zoomToFit == null) {
            if (!AffineImageTransformer.isSimpleTransformation(currentTransf)) {
                List<AsyncDataConverter<ImageTransformerData, TransformedImage>> imageTransformers;
                imageTransformers = new ArrayList<>(interpolationTypes.length);

                for (InterpolationType interpolationType: interpolationTypes) {
                    TaskExecutorService executor = getExecutor(interpolationType);
                    ImageTransformer imageTransformer;
                    imageTransformer = new AffineImageTransformer(
                            currentTransf, bckgColor, interpolationType);

                    imageTransformers.add(new AsyncDataConverter<>(
                            imageTransformer, executor));
                }

                ImageTransformerQuery query;
                query = new ImageTransformerQuery(imageTransformers);

                setImageTransformer(index, ReferenceType.NoRefType, query);
            }
            else {
                ImageTransformer imageTransformer = new AffineImageTransformer(
                        currentTransf, bckgColor, InterpolationType.NEAREST_NEIGHBOR);

                TaskExecutorService executor;
                executor = getExecutor(InterpolationType.NEAREST_NEIGHBOR);

                ImageTransformerQuery query;
                query = new ImageTransformerQuery(executor, imageTransformer);

                setImageTransformer(index, ReferenceType.NoRefType, query);
            }
        }
        else {
            List<AsyncDataConverter<ImageTransformerData, TransformedImage>> imageTransformers;
            imageTransformers = new ArrayList<>(interpolationTypes.length);

            for (InterpolationType interpolationType: interpolationTypes) {
                TaskExecutorService executor = getExecutor(interpolationType);
                ImageTransformer imageTransformer;
                imageTransformer = new ZoomToFitTransformer(
                        currentTransf, zoomToFit, bckgColor, interpolationType);

                AsyncDataConverter<ImageTransformerData, TransformedImageData> asyncTransformer;

                if (imageTransformers.isEmpty()) {
                    imageTransformer = new ZoomToFitDataGatherer(
                            currentTransf, imageTransformer, zoomToFit);
                }

                imageTransformers.add(new AsyncDataConverter<>(
                        imageTransformer, executor));
            }

            ImageTransformerQuery query;
            query = new ImageTransformerQuery(imageTransformers);

            setImageTransformer(index, ReferenceType.NoRefType, query);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        renderAgain();
    }

    /**
     * Returns the snapshot of the currently set transformations. The return
     * value does not include to currently set zoom to fit mode.
     *
     * @return the snapshot of the currently set transformations. This method
     *   never returns {@code null}.
     */
    public final BasicImageTransformations getTransformations() {
        return transformations.getTransformations();
    }

    /**
     * Sets the applied transformations to the identity transformations. This
     * method will also remove the zoom to fit mode if set.
     */
    public final void setDefaultTransformations() {
        clearZoomToFit();
        transformations.setTransformations(BasicImageTransformations.identityTransformation());
    }

    /**
     * Sets all the transformation properties of this component from the
     * specified {@link BasicImageTransformations}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners. Note that this method does not necessarily invoke the
     * listeners only after all properties have been set. That is, the effect of
     * this method cannot be considered atomic in any way.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the transformations.
     *
     * @param transformations the {@code BasicImageTransformations} from
     *   which the properties are to be extracted. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final void setTransformations(BasicImageTransformations transformations) {
        clearZoomToFit();
        this.transformations.setTransformations(transformations);
    }

    /**
     * Sets the scaling factor used to scale the image vertically. The scaling
     * must be interpreted in the coordinate system of the original image.
     * That is, {@code zoomY} adjusts the height of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param zoomY the scaling factor used to scale the image vertically
     *
     * @see TransformationListener#zoomChanged()
     */
    public final void setZoomY(double zoomY) {
        clearZoomToFit();
        transformations.setZoomY(zoomY);
    }

    /**
     * Sets the scaling factor used to scale the image horizontally. The scaling
     * must be interpreted in the coordinate system of the original image.
     * That is, {@code zoomX} adjusts the width of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param zoomX the scaling factor used to scale the image horizontally
     *
     * @see TransformationListener#zoomChanged()
     */
    public final void setZoomX(double zoomX) {
        clearZoomToFit();
        transformations.setZoomX(zoomX);
    }

    /**
     * Sets the scaling factors used to scale the image horizontally and
     * vertically to the same value. The scaling must be interpreted in the
     * coordinate system of the original image. That is, {@code zoom} adjusts
     * both the width and the height of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param zoom the scaling factor used to scale the image both horizontally
     *   and vertically
     *
     * @see TransformationListener#zoomChanged()
     */
    public final void setZoom(double zoom) {
        clearZoomToFit();
        transformations.setZoom(zoom);
    }

    /**
     * Sets the angle meaning how much the image need to be rotated around
     * its center in radians. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Note: Settings this property also changes the value returned by the
     * {@code getTransformations().getRotateInDegrees()} method.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if the
     * {@link #setAlwaysClearZoomToFit(boolean) AlwaysClearZoomToFit} has been
     * set to {@code true}.
     *
     * @param radians the angle meaning how much the image need to be rotated
     *   around its center in radians. Note that this property is stored in a
     *   normalized form. So this property will be set to value between
     *   0 and {@code 2*pi} or {@code NaN}.
     *
     * @see TransformationListener#rotateChanged()
     */
    public final void setRotateInRadians(double radians) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setRotateInRadians(radians);
    }

    /**
     * Sets the angle meaning how much the image need to be rotated around
     * its center in degrees. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Notice that this property is an {@code int}, if you need better
     * precision, use the {@link #setRotateInRadians(double) setRotateInRadians}
     * method.
     * <P>
     * Note: Settings this property also changes the value returned by the
     * {@code getTransformations().getRotateInRadians()} method.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if the
     * {@link #setAlwaysClearZoomToFit(boolean) AlwaysClearZoomToFit} has been
     * set to {@code true}.
     *
     * @param degrees the angle meaning how much the image need to be rotated
     *   around its center in degrees. Note that this property is stored in a
     *   normalized form. So this property will be set to value which is greater
     *   than or equal to zero and less than (not equal) to 360.
     *
     * @see TransformationListener#rotateChanged()
     */
    public final void setRotateInDegrees(int degrees) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setRotateInDegrees(degrees);
    }

    /**
     * Sets both the horizontal an vertical offset need to be applied on the
     * image.
     * <P>
     * Note that these offsets are to be applied after zoom has been applied.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param offsetX the offset along the horizontal axis. Subsequent
     *   {@code getTransformations().getOffsetX()} method calls will return this
     *   value.
     * @param offsetY the offset along the vertical axis. Subsequent
     *   {@code getTransformations().getOffsetY()} method calls will return this
     *   value.
     *
     * @see TransformationListener#offsetChanged()
     */
    public final void setOffset(double offsetX, double offsetY) {
        clearZoomToFit();
        transformations.setOffset(offsetX, offsetY);
    }

    /**
     * Sets if the image should be flipped vertically. That is, if the top
     * side of the image should become the bottom side of the image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if the
     * {@link #setAlwaysClearZoomToFit(boolean) AlwaysClearZoomToFit} has been
     * set to {@code true}.
     *
     * @param flipVertical if {@code true} the image should be flipped
     *   vertically, set it to {@code false} otherwise
     *
     * @see TransformationListener#flipChanged()
     */
    public final void setFlipVertical(boolean flipVertical) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setFlipVertical(flipVertical);
    }

    /**
     * Sets if the image should be flipped horizontally. That is, if the left
     * side of the image should become the right side of the image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if the
     * {@link #setAlwaysClearZoomToFit(boolean) AlwaysClearZoomToFit} has been
     * set to {@code true}.
     *
     * @param flipHorizontal if {@code true} the image should be flipped
     *   horizontally, set it to {@code false} otherwise
     *
     * @see TransformationListener#flipChanged()
     */
    public final void setFlipHorizontal(boolean flipHorizontal) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setFlipHorizontal(flipHorizontal);
    }

    /**
     * Inverts the {@link #setFlipVertical(boolean) "FlipVertical" property}.
     * Calling this method is effectively equivalent to calling:
     * {@code setFlipVertical(!getTransformations().isFlipVertical())}.
     * <P>
     * This method will notify the appropriate listeners.
     *
     * @see TransformationListener#flipChanged()
     */
    public final void flipVertical() {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.flipVertical();
    }

    /**
     * Inverts the {@link #setFlipHorizontal(boolean) "FlipHorizontal" property}.
     * Calling this method is effectively equivalent to calling:
     * {@code setFlipHorizontal(!getTransformations().isFlipHorizontal())}.
     * <P>
     * This method will notify the appropriate listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if the
     * {@link #setAlwaysClearZoomToFit(boolean) AlwaysClearZoomToFit} has been
     * set to {@code true}.
     *
     * @see TransformationListener#flipChanged()
     */
    public final void flipHorizontal() {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.flipHorizontal();
    }

    /**
     * Removes this component from zoom to fit mode. That is, the image
     * displayed is no longer required to fit the display.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @see TransformationListener#leaveZoomToFitMode()
     */
    public final void clearZoomToFit() {
        transformations.clearZoomToFit();
    }

    /**
     * Sets the zoom to fit mode with the following rules:
     * {@link ZoomToFitOption#FIT_HEIGHT}, {@link ZoomToFitOption#FIT_WIDTH} and
     * with the ones specified in the arguments.
     * <P>
     * This method call is equivalent to calling:
     * {@code setZoomToFit(keepAspectRatio, magnify, true, true)}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param keepAspectRatio if {@code true} the zoom to fit mode will also
     *   include the rule: {@link ZoomToFitOption#KEEP_ASPECT_RATIO}
     * @param magnify if {@code true} the zoom to fit mode will also
     *   include the rule: {@link ZoomToFitOption#MAY_MAGNIFY}
     *
     * @see TransformationListener#enterZoomToFitMode(Set)
     */
    public final void setZoomToFit(boolean keepAspectRatio, boolean magnify) {
        transformations.setZoomToFit(keepAspectRatio, magnify);
    }

    /**
     * Sets the zoom to fit mode with the specified rules.
     * <P>
     * Subsequent {@link #getZoomToFitOptions() getZoomToFitOptions()} method
     * calls will return these set of rules.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param keepAspectRatio if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#KEEP_ASPECT_RATIO}
     * @param magnify if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#MAY_MAGNIFY}
     * @param fitWidth if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#FIT_WIDTH}
     * @param fitHeight if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#FIT_HEIGHT}
     */
    public final void setZoomToFit(boolean keepAspectRatio, boolean magnify, boolean fitWidth, boolean fitHeight) {
        transformations.setZoomToFit(keepAspectRatio, magnify, fitWidth, fitHeight);
    }

    private final class ZoomToFitDataGatherer implements ImageTransformer {
        private final BasicImageTransformations transBase;
        private final ImageTransformer wrappedTransformer;
        private final Set<ZoomToFitOption> originalZoomToFit;
        private final Set<ZoomToFitOption> currentZoomToFit;

        public ZoomToFitDataGatherer(BasicImageTransformations transBase,
                ImageTransformer wrappedTransformer,
                Set<ZoomToFitOption> originalZoomToFit) {

            assert wrappedTransformer != null;
            assert transBase != null;

            this.transBase = transBase;
            this.wrappedTransformer = wrappedTransformer;
            this.originalZoomToFit = originalZoomToFit;
            this.currentZoomToFit = EnumSet.copyOf(originalZoomToFit);
        }

        @Override
        public TransformedImage convertData(ImageTransformerData input) {
            if (input.getSourceImage() != null) {
                final BasicImageTransformations newTransformations;
                newTransformations = ZoomToFitTransformer.getBasicTransformations(
                        input.getSrcWidth(),
                        input.getSrcHeight(),
                        input.getDestWidth(),
                        input.getDestHeight(),
                        currentZoomToFit,
                        transBase);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (Objects.equals(originalZoomToFit, transformations.getZoomToFitOptions())
                                && Objects.equals(transBase, transformations.getTransformations())) {
                            transformations.setTransformations(newTransformations);
                        }
                    }
                });
            }

            return wrappedTransformer.convertData(input);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
            result.append("Collect ZoomToFit transformation data and ");
            AsyncFormatHelper.appendIndented(wrappedTransformer, result);

            return result.toString();
        }
    }
}
