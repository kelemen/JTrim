/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.concurrent.*;

/**
 *
 * @author Kelemen Attila
 */
public abstract class RepeatingTask implements Runnable {

    private final ScheduledExecutorService executor;
    private final long period;
    private final TimeUnit periodUnit;
    private final boolean scheduleOnFailure;

    public RepeatingTask(ScheduledExecutorService executor,
            long period, TimeUnit periodUnit) {
        this(executor, period, periodUnit, true);
    }

    public RepeatingTask(ScheduledExecutorService executor,
            long period, TimeUnit periodUnit, boolean scheduleOnFailure) {

        this.executor = executor;
        this.period = period;
        this.periodUnit = periodUnit;
        this.scheduleOnFailure = scheduleOnFailure;
    }

    public final void execute() {
        executor.execute(this);
    }

    public final void schedule(long delay, TimeUnit delayUnit) {
        executor.schedule(this, delay, delayUnit);
    }

    protected abstract boolean runAndTest();

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
