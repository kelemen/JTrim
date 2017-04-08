package org.jtrim.taskgraph;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;

public interface TaskFactory<R, T> {
    public CancelableFunction<R> createTaskNode(
            CancellationToken cancelToken,
            TaskNodeCreateArgs<T> nodeDef) throws Exception;
}
