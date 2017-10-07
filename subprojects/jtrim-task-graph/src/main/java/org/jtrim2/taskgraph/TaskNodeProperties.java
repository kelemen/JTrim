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
    private final DependencyErrorHandler dependencyErrorHandler;

    /**
     * Sets the properties of the {@code TaskNodeProperties} from the current
     * value of the passed {@code Builder}.
     *
     * @param builder the builder from which a snapshot is created from. This argument
     *   cannot be {@code null}.
     */
    protected TaskNodeProperties(Builder builder) {
        this.executor = builder.executor;
        this.dependencyErrorHandler = builder.dependencyErrorHandler;
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
     * Returns the handler to be called when the associated task node cannot due to
     * a failure in one of its dependencies. The handler is called in the same context
     * as the computation of the task node would have been.
     *
     * @return the handler to be called when the associated task node cannot due to
     *   a failure in one of its dependencies, or {@code null} if there is nothing to
     *   do with the failure.
     */
    public DependencyErrorHandler tryGetDependencyErrorHandler() {
        return dependencyErrorHandler;
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
        private DependencyErrorHandler dependencyErrorHandler;

        /**
         * Initializes the {@code Builder} with the default values:
         * <ul>
         *  <li><B>executor</B>: An executor synchronously executing tasks on the calling thread.</li>
         * </ul>
         */
        public Builder() {
            this.executor = SyncTaskExecutor.getSimpleExecutor();
            this.dependencyErrorHandler = null;
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
            this.dependencyErrorHandler = defaults.tryGetDependencyErrorHandler();
        }

        /**
         * Sets an error handler to be called if the associated node could not be
         * executed due to a dependency error. The handler is called in the same context
         * as the computation of the task node would have been.
         * <P>
         * Setting this property will override any previously set value for this property.
         *
         * @param dependencyErrorHandler the error handler to be called if the associated node could not be
         *   executed due to a dependency error. This argument can be {@code null} if there is nothing
         *   to do in case of a dependency error.
         */
        public void setDependencyErrorHandler(DependencyErrorHandler dependencyErrorHandler) {
            this.dependencyErrorHandler = dependencyErrorHandler;
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
