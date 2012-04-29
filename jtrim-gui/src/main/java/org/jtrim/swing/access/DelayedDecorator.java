package org.jtrim.swing.access;

import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines the {@code JPanel} factories required for the
 * {@link ComponentDecorator}. These panels are used as the glass pane of the
 * component managed by the {@code ComponentDecorator}.
 * <P>
 * There are two panel factories used by {@code ComponentDecorator}: One is
 * to be immediately installed when the group of rights associated with the
 * {@code ComponentDecorator} becomes unavailable and one to be installed after
 * a certain time elapses and the rights are still unavailable.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently. Not however that this is not true for the
 * {@code DecoratorPanelFactory} instances.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see ComponentDecorator
 *
 * @author Kelemen Attila
 */
public final class DelayedDecorator {
    private final DecoratorPanelFactory immediateDecorator;
    private final DecoratorPanelFactory mainDecorator;
    private final long decoratorPatienceNanos;

    public DelayedDecorator(
            DecoratorPanelFactory mainDecorator,
            long decoratorPatience,
            TimeUnit timeUnit) {
        this(InvisiblePanelFactory.INSTANCE, mainDecorator, decoratorPatience, timeUnit);
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
    public DelayedDecorator(
            DecoratorPanelFactory immediateDecorator,
            DecoratorPanelFactory mainDecorator,
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
    public DecoratorPanelFactory getImmediateDecorator() {
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
    public DecoratorPanelFactory getMainDecorator() {
        return mainDecorator;
    }
}
