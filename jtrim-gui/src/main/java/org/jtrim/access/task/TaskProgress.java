package org.jtrim.access.task;

import java.text.DecimalFormat;

/**
 * @deprecated This class is deprecated since {@link RewTask} was deprecated
 *   as no longer being useful
 *
 * Defines the state of the progress of a certain task.
 * The state is defined by a progress value within the range [0; 1] and
 * a user defined state.
 * <P>
 * The progress can be specified with a double value between 0.0 and 1.0
 * where 0.0 means the beginning of the task and 1.0 means that the task has
 * been completed. By default linear interpolation should be assumed for the
 * values in-between. That is a progress value 0.5 should indicate that the
 * task is halfway to completion.
 * <P>
 * The user defined state has no special meaning to it but is recommended to
 * be immutable.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such are thread-safe even in
 * the face of unsynchronized concurrent access. Note that although instances
 * are immutable, the user specific data may not and it must be
 * safely published (with no race condition) to be safely used in multithreaded
 * environment if it is not known to be immutable. Note that it is recommended
 * to use immutable types for the user specific object.
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I> unless
 * otherwise noted.
 *
 * @param <UserDataType> the type of the user specific object
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class TaskProgress<UserDataType> {
    private final double progress;
    private final UserDataType userData;

    /**
     * Initializes this task progress with the specified progress value
     * and a user specific object.
     *
     * @param progress the value which defines the current progress of the
     *   execution of the task. This value should be within the range [0; 1].
     *   In case the value specified is lower than 0, 0 is assumed; if it is
     *   larger than 1, 1 is assumed and no exception is thrown. If the value
     *   specified is not a number (NaN), 0 is assumed.
     * @param userData the user specific object that defines the state of
     *   execution. This argument can be {@code null}.
     */
    public TaskProgress(double progress, UserDataType userData) {
        // even if it is -0.0 we will adjust it.
        if (progress <= 0.0) {
            this.progress = 0.0;
        }
        else if (progress > 1.0) {
            this.progress = 1.0;
        }
        else if (Double.isNaN(progress)) {
            this.progress = 0.0;
        }
        else {
            this.progress = progress;
        }

        this.userData = userData;
    }

    /**
     * Returns the current progress of the task. This method always returns
     * a double value within the range [0.0; 1.0].
     *
     * @return the current progress of the task
     */
    public double getProgress() {
        return progress;
    }

    /**
     * Returns the user specific state of the progress of the task.
     *
     * @return the user specific state of the progress of the task. This method
     *   may return {@code null}.
     */
    public UserDataType getUserData() {
        return userData;
    }

    /**
     * Returns the string representation of this task progress state in no
     * particular format. The string representation will contain both the
     * {@code double} progress value and the user specific object (relying on
     * its {@code toString()} method).
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "TaskProgress{"
                + "progress = " + new DecimalFormat("#.##").format(100.0 * progress) + "%"
                + ", data = " + userData + '}';
    }
}
