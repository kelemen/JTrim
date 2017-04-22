package org.jtrim.taskgraph;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines the properties used to control task graph building (not its execution).
 * <P>
 * Instances of this class can can be instantiated through its
 * {@link TaskGraphBuilderProperties.Builder TaskGraphBuilderProperties.Builder}.
 * <P>
 * This class maybe extended by more specific implementations of the task execution framework.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safely callable from multiple threads concurrently and
 * the properties of {@code TaskGraphBuilderProperties} cannot be changed.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see TaskGraphBuilder
 */
public class TaskGraphBuilderProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphBuilderProperties.class.getName());

    private final TaskFactoryProperties defaultFactoryProperties;

    private final TaskErrorHandler nodeCreateErrorHandler;

    /**
     * Sets the properties of the {@code TaskGraphBuilderProperties} from the current
     * value of the passed {@code Builder}.
     *
     * @param builder the builder from which a snapshot is created from. This argument
     *   cannot be {@code null}.
     */
    protected TaskGraphBuilderProperties(Builder builder) {
        this.defaultFactoryProperties = builder.defaultFactoryProperties.build();
        this.nodeCreateErrorHandler = builder.nodeCreateErrorHandler;
    }

    /**
     * Returns the default values of the properties used by the task node factories. The task node
     * factories themselves may override any value but every task node factory  will use these
     * values by default.
     * <P>
     * Subclasses may override this method to further specify the return type.
     *
     * @return the default values of the properties used by the task nodes. This method
     *   never returns {@code null}.
     */
    public TaskFactoryProperties getDefaultFactoryProperties() {
        return defaultFactoryProperties;
    }

    /**
     * Returns the callback notified whenever a failure occurs while trying to create a node
     * with the associated task factory. The default callback simply logs the error on <I>SEVERE</I>
     * level.
     *
     * @return the callback notified whenever a failure occurs while trying to create a node
     *   with the associated task factory. This method never returns {@code null}.
     */
    public final TaskErrorHandler getNodeCreateErrorHandler() {
        return nodeCreateErrorHandler;
    }

    private static void logNodeCreateError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    /**
     * The {@code Builder} used to create {@link TaskGraphBuilderProperties} instances.
     *
     * <h3>Thread safety</h3>
     * The methods of this class may not be used from multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     */
    public static class Builder {
        private final TaskFactoryProperties.Builder defaultFactoryProperties;

        private TaskErrorHandler nodeCreateErrorHandler;

        /**
         * Initializes the {@code Builder} with the default values:
         * <ul>
         *  <li>
         *   <B>nodeCreateErrorHandler</B>: A callback logging the error on <I>SEVERE</I> level.
         *  </li>
         * </ul>
         */
        public Builder() {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder();
            this.nodeCreateErrorHandler = TaskGraphBuilderProperties::logNodeCreateError;
        }

        /**
         * Initializes the {@code Builder} with the values of the given {@code TaskGraphBuilderProperties}.
         * Immediately creating a {@code TaskGraphBuilderProperties} from this {@code Builder} will yield
         * an effectively equivalent {@code TaskGraphBuilderProperties} as the argument.
         *
         * @param defaults the default values used to initialize the {@code Builder}.
         *   This argument cannot be {@code null}.
         */
        public Builder(TaskGraphBuilderProperties defaults) {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder(defaults.getDefaultFactoryProperties());
            this.nodeCreateErrorHandler = defaults.nodeCreateErrorHandler;
        }

        /**
         * Returns the default values of the properties used by the task node factories. These values
         * might be adjusted by the caller. However, they must be adjusted before calling {@link #build() build()}.
         * <P>
         * Subclasses may override this method to further specify the return type.
         *
         * @return the default values of the properties used by the task nodes. This method never returns
         *   {@code null}.
         */
        public TaskFactoryProperties.Builder defaultFactoryProperties() {
            return defaultFactoryProperties;
        }

        /**
         * Sets the callback notified whenever a failure occurs while trying to create a node
         * with the associated task factory. Setting the callback will override any previously set value.
         *
         * @param nodeCreateErrorHandler the callback notified whenever a failure occurs while trying
         *   to create a node with the associated task factory. This argument cannot be {@code null}.
         */
        public final void setNodeCreateErrorHandler(TaskErrorHandler nodeCreateErrorHandler) {
            ExceptionHelper.checkNotNullArgument(nodeCreateErrorHandler, "nodeCreateErrorHandler");
            this.nodeCreateErrorHandler = nodeCreateErrorHandler;
        }

        /**
         * Creates a snapshot of the current state of this {@code Builder}. Further adjustment of
         * this {@code Builder} will not affect the returned {@code TaskGraphBuilderProperties}.
         *
         * @return a snapshot of the current state of this {@code Builder}. This method may never
         *   return {@code null}.
         */
        public TaskGraphBuilderProperties build() {
            return new TaskGraphBuilderProperties(this);
        }
    }
}
