package org.jtrim.swing.component;

import java.util.Set;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.transform.BasicImageTransformations;
import org.jtrim.image.transform.ZoomToFitOption;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a convenient class for viewing the properties of a
 * {@link BasicTransformationModel} as {@link PropertySource} instances.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed by multiple threads
 * concurrently. Also, as specified by {@code PropertySource}, accessing
 * properties are safe to be accessed concurrently as well.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>. This also
 * holds for the returned properties (as required by the specification of
 * {@code PropertySource}).
 *
 * @see BasicTransformationModel
 *
 * @author Kelemen Attila
 */
public final class BasicTransformationProperty {
    private final PropertySource<Double> offsetX;
    private final PropertySource<Double> offsetY;
    private final PropertySource<Double> zoomX;
    private final PropertySource<Double> zoomY;
    private final PropertySource<Double> rotateInRadians;
    private final PropertySource<Integer> rotateInDegrees;
    private final PropertySource<Boolean> flipHorizontal;
    private final PropertySource<Boolean> flipVertical;
    private final PropertySource<Set<ZoomToFitOption>> zoomToFit;
    private final PropertySource<BasicImageTransformations> transformations;

    /**
     * Creates a {@code BasicTransformationProperty} whose properties are the
     * view of the specified {@code BasicTransformationModel}. That is,
     * modifications of the specified model will be visible from the properties
     * of this {@code BasicTransformationProperty}.
     *
     * @param model the {@code BasicTransformationModel} whose properties are
     *   viewed. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified model is
     *   {@code null}
     */
    public BasicTransformationProperty(final BasicTransformationModel model) {
        ExceptionHelper.checkNotNullArgument(model, "model");

        this.offsetX = new OffsetXView(model);
        this.offsetY = new OffsetYView(model);
        this.zoomX = new ZoomXView(model);
        this.zoomY = new ZoomYView(model);
        this.rotateInDegrees = new RotateDegView(model);
        this.rotateInRadians = new RotateRadView(model);
        this.flipHorizontal = new FlipHorizontalView(model);
        this.flipVertical = new FlipVerticalView(model);
        this.zoomToFit = new ZoomToFitView(model);
        this.transformations = new TransformationsView(model);
    }

    /**
     * Returns the {@link BasicTransformationModel#getOffsetX() offsetX}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getOffsetX() offsetX}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Double> getOffsetX() {
        return offsetX;
    }

    /**
     * Returns the {@link BasicTransformationModel#getOffsetY() offsetY}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getOffsetY() offsetY}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Double> getOffsetY() {
        return offsetY;
    }

    /**
     * Returns the {@link BasicTransformationModel#getZoomX() zoomX}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getZoomX() zoomX}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Double> getZoomX() {
        return zoomX;
    }

    /**
     * Returns the {@link BasicTransformationModel#getZoomY() zoomY}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getZoomY() zoomY}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Double> getZoomY() {
        return zoomY;
    }

    /**
     * Returns the {@link BasicTransformationModel#getRotateInRadians() rotate property in radians}
     * of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getRotateInRadians() rotate property in radians}
     *   of the underlying {@link BasicTransformationModel}. This method never
     *   returns {@code null}.
     */
    public PropertySource<Double> getRotateInRadians() {
        return rotateInRadians;
    }

    /**
     * Returns the {@link BasicTransformationModel#getRotateInDegrees() rotate property in degrees}
     * of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getRotateInDegrees() rotate property in degrees}
     *   of the underlying {@link BasicTransformationModel}. This method never
     *   returns {@code null}.
     */
    public PropertySource<Integer> getRotateInDegrees() {
        return rotateInDegrees;
    }

    /**
     * Returns the {@link BasicTransformationModel#isFlipHorizontal() flipHorizontal}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#isFlipHorizontal() flipHorizontal}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Boolean> getFlipHorizontal() {
        return flipHorizontal;
    }

    /**
     * Returns the {@link BasicTransformationModel#isFlipVertical() flipVertical}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#isFlipVertical() flipVertical}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Boolean> getFlipVertical() {
        return flipVertical;
    }

    /**
     * Returns the {@link BasicTransformationModel#getZoomToFitOptions() zoomToFitOptions}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is can be {@code null}, if the underlying
     * model is not currently in zoom to fit mode.
     *
     * @return the {@link BasicTransformationModel#getZoomToFitOptions() zoomToFitOptions}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<Set<ZoomToFitOption>> getZoomToFit() {
        return zoomToFit;
    }

    /**
     * Returns the {@link BasicTransformationModel#getTransformations() transformations}
     * property of the underlying {@link BasicTransformationModel}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @return the {@link BasicTransformationModel#getTransformations() transformations}
     *   property of the underlying {@link BasicTransformationModel}. This
     *   method never returns {@code null}.
     */
    public PropertySource<BasicImageTransformations> getTransformations() {
        return transformations;
    }

    private static final class OffsetXView implements PropertySource<Double> {
        private final BasicTransformationModel model;

        public OffsetXView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Double getValue() {
            return model.getOffsetX();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void offsetChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class OffsetYView implements PropertySource<Double> {
        private final BasicTransformationModel model;

        public OffsetYView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Double getValue() {
            return model.getOffsetY();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void offsetChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class ZoomXView implements PropertySource<Double> {
        private final BasicTransformationModel model;

        public ZoomXView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Double getValue() {
            return model.getZoomX();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void zoomChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class ZoomYView implements PropertySource<Double> {
        private final BasicTransformationModel model;

        public ZoomYView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Double getValue() {
            return model.getZoomY();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void zoomChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class RotateDegView implements PropertySource<Integer> {
        private final BasicTransformationModel model;

        public RotateDegView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Integer getValue() {
            return model.getRotateInDegrees();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void rotateChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class RotateRadView implements PropertySource<Double> {
        private final BasicTransformationModel model;

        public RotateRadView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Double getValue() {
            return model.getRotateInRadians();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void rotateChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class FlipHorizontalView implements PropertySource<Boolean> {
        private final BasicTransformationModel model;

        public FlipHorizontalView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Boolean getValue() {
            return model.isFlipHorizontal();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void flipChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class FlipVerticalView implements PropertySource<Boolean> {
        private final BasicTransformationModel model;

        public FlipVerticalView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Boolean getValue() {
            return model.isFlipVertical();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void flipChanged() {
                    listener.run();
                }
            });
        }
    }

    private static final class ZoomToFitView implements PropertySource<Set<ZoomToFitOption>> {
        private final BasicTransformationModel model;

        public ZoomToFitView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public Set<ZoomToFitOption> getValue() {
            return model.getZoomToFitOptions();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void enterZoomToFitMode(Set<ZoomToFitOption> options) {
                    listener.run();
                }

                @Override
                public void leaveZoomToFitMode() {
                    listener.run();
                }
            });
        }
    }

    private static final class TransformationsView implements PropertySource<BasicImageTransformations> {
        private final BasicTransformationModel model;

        public TransformationsView(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public BasicImageTransformations getValue() {
            return model.getTransformations();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            return model.addTransformationListener(new TransformationAdapter() {
                @Override
                public void zoomChanged() {
                    listener.run();
                }

                @Override
                public void offsetChanged() {
                    listener.run();
                }

                @Override
                public void flipChanged() {
                    listener.run();
                }

                @Override
                public void rotateChanged() {
                    listener.run();
                }
            });
        }
    }
}
