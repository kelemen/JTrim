package org.jtrim2.property.swing;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jtrim2.utils.ExceptionHelper;

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
 * {@code GlassPaneFactory} instances.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see AutoDisplayState#glassPaneSwitcher(javax.swing.JLayer, DelayedGlassPane)
 * @see AutoDisplayState#glassPaneSwitcher(javax.swing.RootPaneContainer, DelayedGlassPane)
 */
public final class DelayedGlassPane {
    private final GlassPaneFactory immediateGlassPane;
    private final GlassPaneFactory mainGlassPane;
    private final long glassPanePatienceNanos;

    /**
     * Creates the {@code DelayedGlassPane} with the given panel factory and
     * timeout to replace the panel applied immediately. Using this constructor
     * will have an immediate glass pane which is invisible but blocks all user
     * access to the decorated component.
     *
     * @param mainGlassPane the panel factory whose panels are used to
     *   replace the glass pane of the decorated component immediately when the
     *   glass pane is to be applied to the associated component. This argument
     *   cannot be {@code null}.
     * @param glassPanePatience the time in the given time unit to wait to use
     *   the panels created by the {@code mainGlassPane} instead of the
     *   invisible glass pane. This argument must be greater than or equal to
     *   zero.
     * @param timeUnit the time unit of the {@code glassPanePatience} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public DelayedGlassPane(
            GlassPaneFactory mainGlassPane,
            long glassPanePatience,
            TimeUnit timeUnit) {
        this(AutoDisplayState.invisibleGlassPane(), mainGlassPane, glassPanePatience, timeUnit);
    }

    /**
     * Creates the {@code DelayedGlassPane} with the given panel factories and
     * timeout to replace the panel applied immediately.
     *
     * @param immediateGlassPane the panel factory whose panels are used to
     *   replace the glass pane of the decorated component immediately when the
     *   glass pane is to be applied to the associated component. This argument
     *   cannot be {@code null}.
     * @param mainGlassPane the panel factory whose panels are used to
     *   replace the glass pane of the decorated component after the given
     *   timeout elapses and if the glass pane is still needed to be applied to
     *   the associated component. This argument cannot be {@code null}.
     * @param glassPanePatience the time in the given time unit to wait to use
     *   the panels created by the {@code mainGlassPane} instead of the ones
     *   created by {@code immediateGlassPane}. This argument must be greater
     *   than or equal to zero.
     * @param timeUnit the time unit of the {@code glassPanePatience} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public DelayedGlassPane(
            GlassPaneFactory immediateGlassPane,
            GlassPaneFactory mainGlassPane,
            long glassPanePatience,
            TimeUnit timeUnit) {
        Objects.requireNonNull(immediateGlassPane, "immediateGlassPane");
        Objects.requireNonNull(mainGlassPane, "mainGlassPane");
        ExceptionHelper.checkArgumentInRange(glassPanePatience, 0, Long.MAX_VALUE, "glassPanePatience");
        Objects.requireNonNull(timeUnit, "timeUnit");

        this.immediateGlassPane = immediateGlassPane;
        this.mainGlassPane = mainGlassPane;
        this.glassPanePatienceNanos = timeUnit.toNanos(glassPanePatience);
    }

    /**
     * Returns the time to wait to use the panels created by the
     * {@link #getMainGlassPane() MainGlassPane} instead of the ones created by
     * the {@link #getImmediateGlassPane() ImmediateGlassPane} in the given
     * time unit.
     *
     * @param timeUnit the time unit in which the the timeout value is to be
     *   returned. This argument cannot be {@code null}.
     * @return the time to wait until using the
     *   {@link #getMainGlassPane() MainGlassPane} in the given time unit. This
     *   method always returns a value greater than or equal to zero.
     */
    public long getGlassPanePatience(TimeUnit timeUnit) {
        return timeUnit.convert(glassPanePatienceNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the glass pane factory whose panels are used to replace the glass
     * pane of the decorated component immediately when the glass pane is to be
     * applied to the associated component.
     *
     * @return the glass pane factory whose panels are used to replace the glass
     *   pane of the decorated component immediately when the glass pane is to
     *   be applied to the associated component. This method never returns
     *   {@code null}.
     */
    public GlassPaneFactory getImmediateGlassPane() {
        return immediateGlassPane;
    }

    /**
     * Returns the glass pane factory whose panels are used to replace the glass
     * pane of the decorated component after the given timeout elapses and if
     * the glass pane is still needed to be applied to the associated component.
     *
     * @return the glass pane factory whose panels are used to replace the glass
     *   pane of the decorated component after the given timeout elapses and if
     *   the glass pane is still needed to be applied to the associated
     *   component. This method never returns {@code null}.
     */
    public GlassPaneFactory getMainGlassPane() {
        return mainGlassPane;
    }
}
