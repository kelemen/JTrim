package org.jtrim.utils;

import java.util.concurrent.TimeUnit;

/**
 * Represents a duration between two instants. This class helps when a duration
 * is given with an associated {@code TimeUnit}.
 *
 * <h3>Thread safety</h3>
 * This class is immutable and as such, its methods can be safely accessed from
 * multiple concurrent threads.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class TimeDuration {
    private final long duration;
    private final TimeUnit durationUnit;

    /**
     * Creates a {@code TimeDuration} representing the given duration.
     *
     * @param time the duration of time in the given time unit. This argument
     *   can be a negative value.
     * @param unit the unit of the given duration. When the {@code TimeDuration}
     *   instance is converted to this time unit, there will be no rounding
     *   error. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public TimeDuration(long time, TimeUnit unit) {
        ExceptionHelper.checkNotNullArgument(unit, "unit");

        this.duration = time;
        this.durationUnit = unit;
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in the given
     * time unit.
     *
     * @param resultUnit the time unit in which the duration is returned. This
     *   argument cannot be {@code null}.
     * @return the duration represented by the {@code TimeDuration} in the given
     *   time unit. When you specify the same time unit as was specified at
     *   construction time, there will be no rounding error.
     *
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public long getDuration(TimeUnit resultUnit) {
        return resultUnit.convert(duration, durationUnit);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * nanoseconds.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   nanoseconds. If you specified {@code TimeUnit.NANOSECONDS} at
     *   construction time, there will be no rounding error.
     */
    public long toNanos() {
        return durationUnit.toNanos(duration);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * microseconds.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   microseconds. If you specified {@code TimeUnit.MICROSECONDS} at
     *   construction time, there will be no rounding error.
     */
    public long toMicros() {
        return durationUnit.toMicros(duration);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * milliseconds.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   milliseconds. If you specified {@code TimeUnit.MILLISECONDS} at
     *   construction time, there will be no rounding error.
     */
    public long toMillis() {
        return durationUnit.toMillis(duration);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * seconds.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   seconds. If you specified {@code TimeUnit.SECONDS} at
     *   construction time, there will be no rounding error.
     */
    public long toSeconds() {
        return durationUnit.toSeconds(duration);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * minutes.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   minutes. If you specified {@code TimeUnit.MINUTES} at
     *   construction time, there will be no rounding error.
     */
    public long toMinutes() {
        return durationUnit.toMinutes(duration);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * hours.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   hours. If you specified {@code TimeUnit.HOURS} at
     *   construction time, there will be no rounding error.
     */
    public long toHours() {
        return durationUnit.toHours(duration);
    }

    /**
     * Returns the duration represented by the {@code TimeDuration} in
     * days.
     *
     * @return the duration represented by the {@code TimeDuration} in
     *   days. If you specified {@code TimeUnit.DAYS} at
     *   construction time, there will be no rounding error.
     */
    public long toDays() {
        return durationUnit.toDays(duration);
    }

    /**
     * Returns the string representation of this time duration in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return duration + " " + durationUnit;
    }
}
