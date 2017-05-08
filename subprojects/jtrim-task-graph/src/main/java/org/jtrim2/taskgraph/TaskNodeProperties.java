package org.jtrim2.taskgraph;

import java.util.Objects;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;

/**
 * Defines the properties of a task node. Currently, this only
 * defines the executor used to execute the action of the task node.
 * <P>
 * Instances of this class can can be instantiated through its
 * {@link TaskNodeProperties.Builder TaskNodeProperties.Builder}.
 * <P>
 * This class maybe extended by more specific implementations of the task execution framework.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safely callable from multiple threads concurrently and
 * the properties of {@code TaskFactoryProperties} cannot be changed.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>..
 *
 * @see TaskFactory
 * @see TaskNodeCreateArgs#properties()
 */
public class TaskNodeProperties {
    private final TaskExecutor executor;

    /**
     * Sets the properties of the {@code TaskNodeProperties} from the current
     * value of the passed {@code Builder}.
     *
     * @param builder the builder from which a snapshot is created from. This argument
     *   cannot be {@code null}.
     */
    protected TaskNodeProperties(Builder builder) {
        this.executor = builder.executor;
    }

    /**
     * Returns the executor used to execute action of the associated task node. If not
     * specified anywhere, the default executor simply executes task synchronously on the
     * calling thread.
     *
     * @return the executor used to execute action of the associated task node. This
     *   method never returns {@code null}.
     */
    public final TaskExecutor getExecutor() {
        return executor;
    }

    /**
     * The {@code Builder} used to create {@link TaskNodeProperties} instances.
     *
     * <h3>Thread safety</h3>
     * The methods of this class may not be used from multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     */
    public static class Builder {
        private TaskExecutor executor;

        /**
         * Initializes the {@code Builder} with the default values:
         * <ul>
         *  <li><B>executor</B>: An executor synchronously executing tasks on the calling thread.</li>
         * </ul>
         */
        public Builder() {
            this.executor = SyncTaskExecutor.getSimpleExecutor();
        }

        /**
         * Initializes the {@code Builder} with the values of the given {@code TaskNodeProperties}.
         * Immediately creating a {@code TaskNodeProperties} from this {@code Builder} will yield
         * an effectively equivalent {@code TaskFactoryProperties} as the argument.
         *
         * @param defaults the default values used to initialize the {@code Builder}.
         *   This argument cannot be {@code null}.
         */
        public Builder(TaskNodeProperties defaults) {
            this.executor = defaults.getExecutor();
        }

        /**
         * Sets the executor used to execute the actions of the task nodes. Setting
         * the executor will override any previously set value.
         *
         * @param executor the executor used to execute the actions of the task nodes.
         *   This argument cannot be {@code null}.
         */
        public final void setExecutor(TaskExecutor executor) {
            Objects.requireNonNull(executor, "executor");
            this.executor = executor;
        }

        /**
         * Creates a snapshot of the current state of this {@code Builder}. Further adjustment of
         * this {@code Builder} will not affect the returned {@code TaskNodeProperties}.
         *
         * @return a snapshot of the current state of this {@code Builder}. This method may never
         *   return {@code null}.
         */
        public TaskNodeProperties build() {
            return new TaskNodeProperties(this);
        }
    }
}
