package org.jtrim.taskgraph;

/**
 * A {@code TaskInputBinder} is used by task node factories to define the inputs of the
 * task node they create. An input is always an output of another task node. Inputs are declared
 * by specifying the {@link TaskNodeKey unique key identifying the node} whose output is required
 * as an input and using the returned {@link TaskInputRef} in the created task node action to retrieve
 * the input. There is no limit on how many inputs a task node can have. It is also possible to create
 * a {@code TaskInputRef} for the same task node multiple times. Each created {@code TaskInputRef}
 * are independent (regardless if they reference the same node or not) and can be consumed exactly once.
 * <P>
 * Note: When a task node factory defines an input, it will force a new node with the given key to
 * be created (if it was not already created). The node does not need to already exist.
 *
 * <h3>Thread safety</h3>
 * The methods of this task are not thread-safe and may not be accessed from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The method of this interface is not required to be <I>synchronization transparent</I>. They may only
 * be used before the associated task factory returns the task node action it creates.
 *
 * @see TaskFactory
 */
public interface TaskInputBinder {
    /**
     * Declares that the task node action created will need the output of the task node
     * specified by the given key. The returned {@code TaskInputRef} can be used to retrieve the
     * output of that node. However, the returned {@code TaskInputRef} may only be called from the
     * created task node action (during its execution), and not by the task node factory.
     * <P>
     * Calling this method will also force the execution framework to create a task node with the
     * given id, if such node does not already exist.
     *
     * @param <I> the type of the input required. This is also the type of the output of the
     *   referenced task node.
     * @param <A> the type of the input of the task factory of the node producing the input
     * @param defKey the id identifying the node whose output is to be returned by
     *   the {@code TaskInputRef}. This argument cannot be {@code null}.
     * @return the {@code TaskInputRef} which can be used by the created task node action to
     *   retrieve the output of the dependency. This method never returns {@code null}.
     */
    public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey);

    /**
     * Declares that the task node action created will need the output of the task node
     * specified by the given key. This method is simply for convenience when the
     * {@link TaskFactoryKey#getKey() custom factory key} is {@code null}.
     *
     * @param <I> the type of the input required. This is also the type of the output of the
     *   referenced task node.
     * @param <A> the type of the argument of the task factory of the node producing the input
     * @param inputType the type of the input required. This is also the type of the output of the
     *   referenced task node. This argument cannot be {@code null}.
     * @param factoryArgType the type of the argument of the task factory of the node producing the input.
     *   This argument cannot be {@code null}.
     * @param factoryArg the argument of the task factory of the node producing the input.
     *   That is the value returned by {@link TaskNodeCreateArgs#factoryArg() TaskNodeCreateArgs.factoryArg()}.
     *   This argument can be {@code null}, if the task factory accepts {@code null} arguments.
     * @return the {@code TaskInputRef} which can be used by the created task node action to
     *   retrieve the output of the dependency. This method never returns {@code null}.
     *
     * @see #bindInput(TaskNodeKey) bindInput(TaskNodeKey)
     */
    public default <I, A> TaskInputRef<I> bindInput(Class<I> inputType, Class<A> factoryArgType, A factoryArg) {
        TaskFactoryKey<I, A> factoryKey = new TaskFactoryKey<>(inputType, factoryArgType);
        return bindInput(new TaskNodeKey<>(factoryKey, factoryArg));
    }
}
