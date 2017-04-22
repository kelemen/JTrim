package org.jtrim.taskgraph.basic;

import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a node which a {@code TaskExecutionRestrictionStrategy} can restrict from being
 * executed. Once, the {@code TaskExecutionRestrictionStrategy} decides that the associated
 * node can be executed it should execute the {@link #release() release action} of this
 * node.
 * <P>
 * The release action should not be called multiple times. Releasing a task node does
 * not imply that task will scheduled right away. Releasing only means, that the
 * {@code TaskExecutionRestrictionStrategy} no longer wants to stop it from being executed.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are allowed to be used from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@link #release() release} method of this class is not required to be
 * <I>synchronization transparent</I> but other methods are <I>synchronization transparent</I>.
 *
 * @see RestrictableTaskGraphExecutor
 * @see TaskExecutionRestrictionStrategyFactory
 */
public final class RestrictableNode {
    private final TaskNodeKey<?, ?> nodeKey;
    private final Runnable releaseAction;

    /**
     * Creates a new {@code RestrictableNode} with the given key and action used to release
     * this node.
     *
     * @param nodeKey the {@code TaskNodeKey} identifying the node which is to be restricted.
     *   This argument cannot be {@code null}.
     * @param releaseAction the action to be executed to allow the associated node to be
     *   scheduled for execution. This argument cannot be {@code null}. It is highly recommended
     *   for the task action to be idempotent.
     */
    public RestrictableNode(TaskNodeKey<?, ?> nodeKey, Runnable releaseAction) {
        ExceptionHelper.checkNotNullArgument(nodeKey, "nodeKey");
        ExceptionHelper.checkNotNullArgument(releaseAction, "releaseAction");

        this.nodeKey = nodeKey;
        this.releaseAction = releaseAction;
    }

    /**
     * Returns the {@code TaskNodeKey} identifying the node which is to be restricted.
     *
     * @return the {@code TaskNodeKey} identifying the node which is to be restricted. This
     *   method never returns {@code null}.
     */
    public TaskNodeKey<?, ?> getNodeKey() {
        return nodeKey;
    }

    /**
     * Returns the action to be called once the {@link TaskExecutionRestrictionStrategy}
     * decides that the task can be scheduled for execution.
     * <P>
     * The release action should not be called multiple times, regardless if it is done
     * directly or by calling the {@link #release() release} method.
     *
     * @return the action to be called once the {@link TaskExecutionRestrictionStrategy}
     *   decides that the task can be scheduled for execution. This method never returns
     *   {@code null}.
     */
    public Runnable getReleaseAction() {
        return releaseAction;
    }

    /**
     * Allows the associated task node to be scheduled for execution.
     * This method is simply a convenience for:
     * <pre>
     * getReleaseAction().run();
     * </pre>
     * <P>
     * The release action should not be called multiple times, regardless if it is done
     * directly or by calling the {@link #release() release} method.
     */
    public void release() {
        releaseAction.run();
    }

    /**
     * Returns the string representation of this {@code RestrictableNode} in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "RestrictableNode{" + nodeKey + '}';
    }
}
