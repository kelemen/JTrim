package org.jtrim.taskgraph;

import org.jtrim.utils.ExceptionHelper;

/**
 * Defines the properties used to create a task node. Instances of this
 * class are passed to task node factories.
 *
 * <h3>Thread safety</h3>
 * Although methods of this class are safely accessible from multiple threads concurrently,
 * the returned properties are not.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   a node to be created
 *
 * @see TaskFactory
 */
public final class TaskNodeCreateArgs<I> {
    private final I factoryArg;
    private final TaskInputBinder inputs;
    private final TaskNodeProperties.Builder properties;

    /**
     * Creates a new {@code TaskNodeCreateArgs} with the given properties.
     *
     * @param argument the argument passed to the task node factory. This argument
     *   with the {@link TaskFactoryKey task node factory key} uniquely identify
     *   a task node. This argument can be {@code null}, if the factory accepts
     *   {@code null} arguments.
     * @param defaults the default values for the {@link #properties() node properties}.
     *   This argument cannot be {@code null}.
     * @param inputs the {@code TaskInputBinder} used to bind inputs for the created
     *   task node function. This argument cannot be {@code null}.
     */
    public TaskNodeCreateArgs(I argument, TaskNodeProperties defaults, TaskInputBinder inputs) {
        ExceptionHelper.checkNotNullArgument(inputs, "inputs");

        this.factoryArg = argument;
        this.inputs = inputs;
        this.properties = new TaskNodeProperties.Builder(defaults);
    }

    /**
     * Returns the argument passed to the task node factory. This argument
     * with the {@link TaskFactoryKey task node factory key} uniquely identify
     * a task node.
     *
     * @return the argument passed to the task node factory. This method may return
     *   {@code null}, if {@code null} was specified for the factory.
     */
    public I factoryArg() {
        return factoryArg;
    }

    /**
     * Returns the {@code TaskInputBinder} used to bind inputs for the created
     * task node function.
     *
     * @return the {@code TaskInputBinder} used to bind inputs for the created
     *   task node function. This method never returns {@code null}.
     */
    public TaskInputBinder inputs() {
        return inputs;
    }

    /**
     * Returns the properties to be set for the associated task node to be created.
     *
     * @return the properties to be set for the associated task node to be created.
     *   This method may never return {@code null}.
     */
    public TaskNodeProperties.Builder properties() {
        return properties;
    }
}
