package org.jtrim2.taskgraph;

/**
 * Defines the possible general outcomes of a task graph execution.
 *
 * @see TaskGraphExecutionResult
 */
public enum ExecutionResultType {
    /**
     * {@code ERRORED} means that the task graph execution failed due to
     * an exception being thrown while trying to compute the output of
     * a task node.
     */
    ERRORED,
    /**
     * {@code CANCELED} means that the task graph execution was canceled
     * before it could have been fully computed and no other unexpected
     * exceptions were thrown. Note that failing with a {@link TaskSkippedException}
     * is not considered to be a cancellation and (unless other error happens),
     * the computation will be successful.
     */
    CANCELED,

    /**
     * {@code SUCCESS} means that the task graph execution completed
     * fully without any exception being raised and without being canceled.
     */
    SUCCESS;
}
