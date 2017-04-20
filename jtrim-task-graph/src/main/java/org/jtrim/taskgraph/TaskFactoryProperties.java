package org.jtrim.taskgraph;

/**
 * Defines the properties of a task node factory. Currently, this only
 * defines the default values of the properties of task nodes created by
 * the task node factory.
 * <P>
 * Instances of this class can can be instantiated through its
 * {@link TaskFactoryProperties.Builder TaskFactoryProperties.Builder}.
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
 * @see TaskFactoryGroupConfigurer
 * @see TaskGraphDefConfigurer
 */
public class TaskFactoryProperties {
    private final TaskNodeProperties defaultNodeProperties;

    /**
     * Sets the properties of the {@code TaskFactoryProperties} from the current
     * value of the passed {@code Builder}.
     *
     * @param builder the builder from which a snapshot is created from. This argument
     *   cannot be {@code null}.
     */
    protected TaskFactoryProperties(Builder builder) {
        this.defaultNodeProperties = builder.defaultNodeProperties.build();
    }

    /**
     * Returns the default values of the properties used by the task nodes. The task nodes
     * themselves may override any value but every node created by the associated task factory
     * will use these values by default.
     * <P>
     * Subclasses may override this method to further specify the return type.
     *
     * @return the default values of the properties used by the task nodes. This method
     *   never returns {@code null}.
     */
    public TaskNodeProperties getDefaultNodeProperties() {
        return defaultNodeProperties;
    }

    /**
     * The {@code Builder} used to create {@link TaskFactoryProperties} instances.
     *
     * <h3>Thread safety</h3>
     * The methods of this class may not be used from multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     */
    public static class Builder {
        private final TaskNodeProperties.Builder defaultNodeProperties;

        /**
         * Initializes the {@code Builder} with the default values.
         */
        public Builder() {
            this.defaultNodeProperties = new TaskNodeProperties.Builder();
        }

        /**
         * Initializes the {@code Builder} with the values of the given {@code TaskFactoryProperties}.
         * Immediately creating a {@code TaskFactoryProperties} from this {@code Builder} will yield
         * an effectively equivalent {@code TaskFactoryProperties} as the argument.
         *
         * @param defaults the default values used to initialize the {@code Builder}.
         *   This argument cannot be {@code null}.
         */
        public Builder(TaskFactoryProperties defaults) {
            this.defaultNodeProperties = new TaskNodeProperties.Builder(defaults.getDefaultNodeProperties());
        }

        /**
         * Returns the default values of the properties used by the task nodes. These values
         * might be adjusted by the caller. However, they must be adjusted before calling {@link #build() build()}.
         * <P>
         * Subclasses may override this method to further specify the return type.
         *
         * @return the default values of the properties used by the task nodes. This method never returns
         *   {@code null}.
         */
        public TaskNodeProperties.Builder defaultNodeProperties() {
            return defaultNodeProperties;
        }

        /**
         * Creates a snapshot of the current state of this {@code Builder}. Further adjustment of
         * this {@code Builder} will not affect the returned {@code TaskFactoryProperties}.
         *
         * @return a snapshot of the current state of this {@code Builder}. This method may never
         *   return {@code null}.
         */
        public TaskFactoryProperties build() {
            return new TaskFactoryProperties(this);
        }
    }
}
