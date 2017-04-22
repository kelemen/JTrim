package org.jtrim.taskgraph;

/**
 * Defines how to create task nodes, which is the first step in building a task execution graph.
 * To create a new definition start by calling the
 * {@link #factoryGroupDefiner(TaskFactoryGroupConfigurer) factoryGroupDefiner} method and specify the factories
 * of the task nodes. Once, you have every task node factory defined, you can move on to the second step:
 * {@link #build() Defining the initial task nodes of the graph}.
 * <P>
 * Task factories are expected to be very static, and having a task factory defined is only a possibility
 * for a node. Having a task factory does not automatically mean that it will be used to create any node.
 * <P>
 * An example usage is:
 * <PRE>
 *   TaskGraphDefConfigurer graphConfig = ...;
 *   TaskFactoryDefiner factoryGroup1 = graphConfig.factoryGroupDefiner((properties) -&gt; {
 *     // Adjust the node properties in this group here.
 *   });
 *   factoryGroup1.defineSimpleFactory(NodeOutput.class, FactoryArg.class, (cancelToken, nodeDef) -&gt; {
 *     FactoryArg factoryArg = nodeDef.factoryArg();
 *     TaskInputRef&lt;OtherNodeOutput&gt; input = nodeDef.inputs()
 *       .bindInput(OtherNodeOutput.class, OtherFactoryArg.class, otherFactoryArg);
 *
 *     return (taskCancelToken) -&gt; input.consumeInput().calculateNodeOutput();
 *   });
 * </PRE>
 *
 * <h3>Thread safety</h3>
 * The methods of this interface may not necessarily safe to be used from multiple
 * threads concurrently (though some implementations might choose to allow concurrent access).
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I> in general.
 *
 * <h4>Synchronization transparency</h4>
 *
 * @see TaskGraphExecutors
 * @see TaskGraphBuilder
 * @see TaskGraphExecutor
 */
public interface TaskGraphDefConfigurer {
    /**
     * Returns a {@code TaskFactoryDefiner} through which it is possible to define task node factories.
     * The task factories defined through the returned by this method call will share the same
     * configuration (as specified by the argument). If you need to have task factories with different configurations,
     * you will need to call this method multiple times before creating a {@code TaskGraphBuilder} via
     * the {@link #build() build} method.
     *
     * @param groupConfigurer defines the configuration for the task factories defined through the
     *   returned {@code TaskFactoryDefiner}. This argument is not allowed to be {@code null}.
     * @return a {@code TaskFactoryDefiner} through which it is possible to define task node factories.
     *   This method may never return {@code null}.
     */
    public TaskFactoryDefiner factoryGroupDefiner(TaskFactoryGroupConfigurer groupConfigurer);

    /**
     * Creates a {@code TaskGraphBuilder} through which you will be able to create the initial task nodes
     * of the execution graph. Only task factories defined before this method call are considered.
     *
     * @return a {@code TaskGraphBuilder} through which you will be able to create the initial task nodes
     *   of the execution graph. This method may never return {@code null}.
     */
    public TaskGraphBuilder build();
}
