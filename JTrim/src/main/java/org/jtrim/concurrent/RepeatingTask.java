package org.jtrim.concurrent;

import java.util.concurrent.*;
import org.jtrim.utils.*;

/**
 * Defines a task which can repeatedly be executed by a
 * {@link ScheduledExecutorService} and this task controls if it is needed
 * to be rescheduled.
 * <P>
 * Note that as of <I>Java 5</I> {@link ScheduledThreadPoolExecutor} is
 * preferred over {@link java.util.Timer} (because of its caveats). Most tasks
 * can be  relatively easily ported to use the more robust
 * {@code ScheduledThreadPoolExecutor}. However there are certain tasks which
 * can be easily done using a {@code Timer} but not a
 * {@code ScheduledExecutorService}. One such particular task is when the task
 * itself needs to make sure that it never runs again. This is not so easy to
 * do with a {@code ScheduledExecutorService} because a task submitted to it
 * can be canceled by the future returned by the {@code schedule} method. This
 * class is intended to fill this gap.
 * <P>
 * To use this class subclass this abstract class and implement the
 * {@link #runAndTest() runAndTest()} method. This method returns a
 * {@code boolean} to determine if the task can be scheduled again to run.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently
 * but the {@code runAndTest()} method is not required to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I> and
 * the {@code runAndTest()} method is not required to be
 * <I>synchronization transparent</I> either.
 *
 * @author Kelemen Attila
 */
public abstract class RepeatingTask implements Runnable {
    private final ScheduledExecutorService executor;
    private final long period;
    private final TimeUnit periodUnit;
    private final boolean scheduleOnFailure;

    /**
     * Initializes a {@code RepeatingTask} with a
     * {@code ScheduledExecutorService} and the time to wait between consecutive
     * execution of this task. The task will be be rescheduled to execute again
     * even if it throws an exception.
     * <P>
     * To actually start execution this task periodically: Call the
     * {@link #schedule(long, TimeUnit) schedule(long, TimeUnit)} method or
     * call the {@link #execute() execute()} method to submit this task for
     * execution for a single run (in this case it will be executed without
     * delay).
     *
     * @param executor the {@code ScheduledExecutorService} to which this task
     *   will be submitted to. This argument cannot be {@code null}.
     * @param period the time to wait between consecutive
     *   execution of this task in the given time unit. This argument must be
     *   greater than or equal to zero.
     * @param periodUnit the time unit of the {@code period} argument. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code period &lt 0}
     * @throws NullPointerException thrown if either {@code executor} or
     *   {@code periodUnit} is {@code null}
     */
    public RepeatingTask(ScheduledExecutorService executor,
            long period, TimeUnit periodUnit) {
        this(executor, period, periodUnit, true);
    }

    /**
     * Initializes a {@code RepeatingTask} with a
     * {@code ScheduledExecutorService}, the time to wait between consecutive
     * execution of this task and if it need to be rescheduled in case this
     * task throws an unchecked exception.
     * <P>
     * To actually start execution this task periodically: Call the
     * {@link #schedule(long, TimeUnit) schedule(long, TimeUnit)} method or
     * call the {@link #execute() execute()} method to submit this task for
     * execution for a single run (in this case it will be executed without
     * delay).
     *
     * @param executor the {@code ScheduledExecutorService} to which this task
     *   will be submitted to. This argument cannot be {@code null}.
     * @param period the time to wait between consecutive
     *   execution of this task in the
     *   given time unit. This argument must be greater than or equal to zero.
     * @param periodUnit the time unit of the {@code period} argument. This
     *   argument cannot be {@code null}.
     * @param scheduleOnFailure {@code true} if the task needs to be rescheduled
     *   in case it throws an unchecked exception, {@code false} if the task
     *   must not be executed again in case of such an exception
     *
     * @throws IllegalArgumentException thrown if {@code period &lt 0}
     * @throws NullPointerException thrown if either {@code executor} or
     *   {@code periodUnit} is {@code null}
     */
    public RepeatingTask(ScheduledExecutorService executor,
            long period, TimeUnit periodUnit, boolean scheduleOnFailure) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(periodUnit, "periodUnit");
        ExceptionHelper.checkArgumentInRange(period, 0, Long.MAX_VALUE, "period");

        this.executor = executor;
        this.period = period;
        this.periodUnit = periodUnit;
        this.scheduleOnFailure = scheduleOnFailure;
    }

    /**
     * Submits this task to be executed a single time as soon as possible by
     * the {@code ScheduledExecutorService} specified at construction time.
     *
     * @see ScheduledExecutorService#execute(Runnable)
     */
    public final void execute() {
        executor.execute(this);
    }

    /**
     * Submits this task to be executed a periodically after the given initial
     * delay by the {@code ScheduledExecutorService} specified at construction
     * time.
     *
     * @param delay the initial delay before the first execution of this task
     *   in the given time unit. This argument must be greater than or equal
     *   to zero.
     * @param delayUnit the time unit of the {@code delay} argument. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code delay &lt 0}
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     *
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     */
    public final void schedule(long delay, TimeUnit delayUnit) {
        ExceptionHelper.checkArgumentInRange(delay, 0, Long.MAX_VALUE, "delay");

        executor.schedule(this, delay, delayUnit);
    }

    /**
     * Implement this method to actually execute the given task. This method
     * will be invoked by the {@link #run() run()} method of this task which
     * will also reschedule this task if needed.
     *
     * @return {@code true} if this task needed to be executed again,
     *   {@code false} in case this task must not be executed again
     */
    protected abstract boolean runAndTest();

    /**
     * Invokes the {@link #runAndTest() runAndTest()} method and reschedules
     * it to the {@code ScheduledExecutorService} specified at construction
     * time according to the construction time definitions.
     * <P>
     * Note that this method is only intended to be called by the underlying
     * {@code ScheduledExecutorService}.
     */
    @Override
    public final void run() {
        boolean reschedule = scheduleOnFailure;

        try {
            reschedule = runAndTest();
        } finally {
            if (reschedule) {
                executor.schedule(this, period, periodUnit);
            }
        }
    }
}
