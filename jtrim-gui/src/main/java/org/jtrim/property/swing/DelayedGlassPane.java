package org.jtrim.property.swing;

import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines the factories for glass panes where one glass pane is to be set
 * immediately and the other is to replace it after the specified timeout, if
 * the glass pane is still needed.
 * <P>
 * This class is used by the
 * {@link AutoDisplayState#glassPaneSwitcher(javax.swing.RootPaneContainer, DelayedGlassPane) AutoDisplayState.glassPaneSwitcher}
 * methods to deter
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently. Not however that this is not true for the
 * {@code DecoratorPanelFactory} instances.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see AutoDisplayState#glassPaneSwitcher(javax.swing.JLayer, DelayedGlassPane)
 * @see AutoDisplayState#glassPaneSwitcher(javax.swing.RootPaneContainer, DelayedGlassPane)
 *
 * @author Kelemen Attila
 */
public final class DelayedGlassPane {
    private final GlassPaneFactory immediateDecorator;
    private final GlassPaneFactory mainDecorator;
    private final long decoratorPatienceNanos;

    /**
     * Creates the {@code DelayedDecorator} with the given panel factory and
     * timeout to replace the panel applied immediately. Using this constructor
     * will have an immediate decorator which is invisible but blocks all user
     * access to the decorated component.
     *
     * @param mainDecorator the panel factory whose panels are used to
     *   replace the glass pane of the decorated component after the given
     *   timeout elapses and the group of rights associated with
     *   {@code ComponentDecorator} are still unavailable. This argument cannot
     *   be {@code null}.
     * @param decoratorPatience the time in the given time unit to wait to use
     *   the panels created by the {@code mainDecorator} instead of the ones
     *   created by {@code immediateDecorator}. This argument must be greater
     *   than or equal to zero.
     * @param timeUnit the time unit of the {@code decoratorPatience} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public DelayedGlassPane(
            GlassPaneFactory mainDecorator,
            long decoratorPatience,
            TimeUnit timeUnit) {
        this(InvisibleGlassPaneFactory.INSTANCE, mainDecorator, decoratorPatience, timeUnit);
    }

    /**
     * Creates the {@code DelayedDecorator} with the given panel factories and
     * timeout to replace the panel applied immediately.
     *
     * @param immediateDecorator the panel factory whose panels are used to
     *   replace the glass pane of the decorated component immediately as the
     *   group of rights associated with {@code ComponentDecorator} becomes
     *   unavailable. This argument cannot be {@code null}.
     * @param mainDecorator the panel factory whose panels are used to
     *   replace the glass pane of the decorated component after the given
     *   timeout elapses and the group of rights associated with
     *   {@code ComponentDecorator} are still unavailable. This argument cannot
     *   be {@code null}.
     * @param decoratorPatience the time in the given time unit to wait to use
     *   the panels created by the {@code mainDecorator} instead of the ones
     *   created by {@code immediateDecorator}. This argument must be greater
     *   than or equal to zero.
     * @param timeUnit the time unit of the {@code decoratorPatience} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public DelayedGlassPane(
            GlassPaneFactory immediateDecorator,
            GlassPaneFactory mainDecorator,
            long decoratorPatience,
            TimeUnit timeUnit) {
        ExceptionHelper.checkNotNullArgument(immediateDecorator, "immediateDecorator");
        ExceptionHelper.checkNotNullArgument(mainDecorator, "mainDecorator");
        ExceptionHelper.checkArgumentInRange(decoratorPatience, 0, Long.MAX_VALUE, "decoratorPatience");
        ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");

        this.immediateDecorator = immediateDecorator;
        this.mainDecorator = mainDecorator;
        this.decoratorPatienceNanos = timeUnit.toNanos(decoratorPatience);
    }

    /**
     * Returns the time to wait to use the panels created by the
     * {@link #getMainDecorator() MainDecorator} instead of the ones created by
     * the {@link #getImmediateDecorator() ImmediateDecorator} in the given
     * time unit.
     *
     * @param timeUnit the time unit in which the the timeout value is to be
     *   returned. This argument cannot be {@code null}.
     * @return the time to wait until using the
     *   {@link #getMainDecorator() MainDecorator} in the given time unit. This
     *   method always returns a value greater than or equal to zero.
     */
    public long getDecoratorPatience(TimeUnit timeUnit) {
        return timeUnit.convert(decoratorPatienceNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the panel factory whose panels are used to replace the glass pane
     * of the decorated component immediately as the group of rights associated
     * with {@code ComponentDecorator} becomes unavailable.
     *
     * @return the panel factory whose panels are used to replace the glass pane
     *   of the decorated component immediately as the group of rights
     *   associated with {@code ComponentDecorator} becomes unavailable. This
     *   method never returns {@code null}.
     */
    public GlassPaneFactory getImmediateDecorator() {
        return immediateDecorator;
    }

    /**
     * Returns the panel factory whose panels are used to replace the glass pane
     * of the decorated component after the given timeout elapses and the group
     * of rights associated with {@code ComponentDecorator} are still
     * unavailable.
     *
     * @return the panel factory whose panels are used to replace the glass pane
     *   of the decorated component after the given timeout elapses and the
     *   group of rights associated with {@code ComponentDecorator} are still
     *   unavailable. This method never returns {@code null}.
     */
    public GlassPaneFactory getMainDecorator() {
        return mainDecorator;
    }
}
