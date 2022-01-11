package org.jtrim2.utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a duration between two instants. This class helps when a duration
 * is given with an associated {@code TimeUnit}.
 *
 * <h2>Thread safety</h2>
 * This class is immutable and as such, its methods can be safely accessed from
 * multiple concurrent threads.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are <I>synchronization transparent</I>.
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
        Objects.requireNonNull(unit, "unit");

        this.duration = time;
        this.durationUnit = unit;
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * nanoseconds.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.NANOSECONDS}.
     *
     * @param nanos the number of nanoseconds the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   nanoseconds. This method never returns {@code null}.
     */
    public static TimeDuration nanos(long nanos) {
        return new TimeDuration(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * microseconds.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.MICROSECONDS}.
     *
     * @param micros the number of microseconds the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   microseconds. This method never returns {@code null}.
     */
    public static TimeDuration micros(long micros) {
        return new TimeDuration(micros, TimeUnit.MICROSECONDS);
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * milliseconds.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.MILLISECONDS}.
     *
     * @param millis the number of milliseconds the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   milliseconds. This method never returns {@code null}.
     */
    public static TimeDuration millis(long millis) {
        return new TimeDuration(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * seconds.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.SECONDS}.
     *
     * @param seconds the number of seconds the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   seconds. This method never returns {@code null}.
     */
    public static TimeDuration seconds(long seconds) {
        return new TimeDuration(seconds, TimeUnit.SECONDS);
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * minutes.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.MINUTES}.
     *
     * @param minutes the number of minutes the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   minutes. This method never returns {@code null}.
     */
    public static TimeDuration minutes(long minutes) {
        return new TimeDuration(minutes, TimeUnit.MINUTES);
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * hours.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.HOURS}.
     *
     * @param hours the number of hours the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   hours. This method never returns {@code null}.
     */
    public static TimeDuration hours(long hours) {
        return new TimeDuration(hours, TimeUnit.HOURS);
    }

    /**
     * Returns a {@code TimeDuration} representing the specified number of
     * days.
     * <P>
     * The unit of the returned {@code TimeDuration} is {@code TimeUnit.DAYS}.
     *
     * @param days the number of days the returned {@code TimeDuration}
     *   represents. This value can be any {@code long} value.
     * @return the {@code TimeDuration} representing the specified number of
     *   days. This method never returns {@code null}.
     */
    public static TimeDuration days(long days) {
        return new TimeDuration(days, TimeUnit.DAYS);
    }

    /**
     * Returns the {@code TimeUnit} this duration natively represents. That is,
     * converting to this time unit will not cause loss of information compared
     * to what was specified at construction time.
     *
     * @return the {@code TimeUnit} this duration natively represents. This
     *   method never returns {@code null}.
     */
    public TimeUnit getNativeTimeUnit() {
        return durationUnit;
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
