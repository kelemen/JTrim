package org.jtrim2.swing.component;

import java.util.Objects;
import java.util.Set;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.image.transform.BasicImageTransformations;
import org.jtrim2.image.transform.ZoomToFitOption;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;

/**
 * Defines a convenient class for viewing the properties of a
 * {@link BasicTransformationModel} as {@link MutableProperty} instances.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed by multiple threads
 * concurrently. Also, the returned properties respect the general contract of
 * {@link MutableProperty} and {@link org.jtrim2.property.PropertySource}.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see BasicTransformationModel
 *
 * @author Kelemen Attila
 */
public final class BasicTransformationProperty {
    private final MutableProperty<Double> offsetX;
    private final MutableProperty<Double> offsetY;
    private final MutableProperty<Double> zoomX;
    private final MutableProperty<Double> zoomY;
    private final MutableProperty<Double> rotateInRadians;
    private final MutableProperty<Integer> rotateInDegrees;
    private final MutableProperty<Boolean> flipHorizontal;
    private final MutableProperty<Boolean> flipVertical;
    private final MutableProperty<Set<ZoomToFitOption>> zoomToFit;
    private final MutableProperty<BasicImageTransformations> transformations;

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
        Objects.requireNonNull(model, "model");

        this.offsetX = PropertyFactory.lazilyNotifiedProperty(new OffsetXProperty(model));
        this.offsetY = PropertyFactory.lazilyNotifiedProperty(new OffsetYProperty(model));
        this.zoomX = PropertyFactory.lazilyNotifiedProperty(new ZoomXProperty(model));
        this.zoomY = PropertyFactory.lazilyNotifiedProperty(new ZoomYProperty(model));
        this.rotateInDegrees = PropertyFactory.lazilyNotifiedProperty(new RotateDegProperty(model));
        this.rotateInRadians = PropertyFactory.lazilyNotifiedProperty(new RotateRadProperty(model));
        this.flipHorizontal = PropertyFactory.lazilyNotifiedProperty(new FlipHorizontalProperty(model));
        this.flipVertical = PropertyFactory.lazilyNotifiedProperty(new FlipVerticalProperty(model));
        this.zoomToFit = PropertyFactory.lazilyNotifiedProperty(new ZoomToFitProperty(model));
        this.transformations = new TransformationsProperty(model);
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
    public MutableProperty<Double> offsetX() {
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
    public MutableProperty<Double> offsetY() {
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
    public MutableProperty<Double> zoomX() {
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
    public MutableProperty<Double> zoomY() {
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
    public MutableProperty<Double> rotateInRadians() {
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
    public MutableProperty<Integer> rotateInDegrees() {
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
    public MutableProperty<Boolean> flipHorizontal() {
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
    public MutableProperty<Boolean> flipVertical() {
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
    public MutableProperty<Set<ZoomToFitOption>> zoomToFit() {
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
    public MutableProperty<BasicImageTransformations> transformations() {
        return transformations;
    }

    private static final class OffsetXProperty implements MutableProperty<Double> {
        private final BasicTransformationModel model;

        public OffsetXProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Double value) {
            model.setOffset(value, model.getOffsetX());
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

    private static final class OffsetYProperty implements MutableProperty<Double> {
        private final BasicTransformationModel model;

        public OffsetYProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Double value) {
            model.setOffset(model.getOffsetX(), value);
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

    private static final class ZoomXProperty implements MutableProperty<Double> {
        private final BasicTransformationModel model;

        public ZoomXProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Double value) {
            model.setZoomX(value);
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

    private static final class ZoomYProperty implements MutableProperty<Double> {
        private final BasicTransformationModel model;

        public ZoomYProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Double value) {
            model.setZoomY(value);
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

    private static final class RotateDegProperty implements MutableProperty<Integer> {
        private final BasicTransformationModel model;

        public RotateDegProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Integer value) {
            model.setRotateInDegrees(value);
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

    private static final class RotateRadProperty implements MutableProperty<Double> {
        private final BasicTransformationModel model;

        public RotateRadProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Double value) {
            model.setRotateInRadians(value);
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

    private static final class FlipHorizontalProperty implements MutableProperty<Boolean> {
        private final BasicTransformationModel model;

        public FlipHorizontalProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Boolean value) {
            model.setFlipHorizontal(value);
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

    private static final class FlipVerticalProperty implements MutableProperty<Boolean> {
        private final BasicTransformationModel model;

        public FlipVerticalProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Boolean value) {
            model.setFlipVertical(value);
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

    private static final class ZoomToFitProperty implements MutableProperty<Set<ZoomToFitOption>> {
        private final BasicTransformationModel model;

        public ZoomToFitProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(Set<ZoomToFitOption> value) {
            if (value == null) {
                model.clearZoomToFit();
            }
            else {
                model.setZoomToFit(value);
            }
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

    private static final class TransformationsProperty implements MutableProperty<BasicImageTransformations> {
        private final BasicTransformationModel model;

        public TransformationsProperty(BasicTransformationModel model) {
            this.model = model;
        }

        @Override
        public void setValue(BasicImageTransformations value) {
            model.setTransformations(value);
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
