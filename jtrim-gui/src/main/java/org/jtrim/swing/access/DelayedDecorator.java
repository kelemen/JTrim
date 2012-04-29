package org.jtrim.swing.access;

import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
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

    public long getDecoratorPatience(TimeUnit timeUnit) {
        return timeUnit.convert(decoratorPatienceNanos, TimeUnit.NANOSECONDS);
    }

    public DecoratorPanelFactory getImmediateDecorator() {
        return immediateDecorator;
    }

    public DecoratorPanelFactory getMainDecorator() {
        return mainDecorator;
    }
}
