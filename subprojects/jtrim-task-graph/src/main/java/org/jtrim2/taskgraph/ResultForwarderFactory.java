package org.jtrim2.taskgraph;

import java.util.Objects;
import java.util.function.Function;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.SyncTaskExecutor;

final class ResultForwarderFactory<R, I> implements TaskFactory<R, I> {
    private final NodeForwarder<R, I, ?> nodeForwarder;

    public <I2> ResultForwarderFactory(Function<TaskNodeKey<R, I>, TaskNodeKey<R, I2>> dependencyFactory) {
        this.nodeForwarder = new NodeForwarder<>(dependencyFactory);
    }

    @Override
    public CancelableFunction<R> createTaskNode(
            CancellationToken cancelToken,
            TaskNodeCreateArgs<R, I> nodeDef) throws Exception {
        nodeDef.properties().setExecutor(SyncTaskExecutor.getSimpleExecutor());

        TaskInputRef<R> resultRef = nodeForwarder.bindInput(nodeDef.nodeKey(), nodeDef.inputs());
        return taskCancelToken -> resultRef.consumeInput();
    }

    private static final class NodeForwarder<R, I, I2> {
        private final Function<TaskNodeKey<R, I>, TaskNodeKey<R, I2>> dependencyFactory;

        public NodeForwarder(Function<TaskNodeKey<R, I>, TaskNodeKey<R, I2>> dependencyFactory) {
            this.dependencyFactory = Objects.requireNonNull(dependencyFactory, "dependencyFactory");
        }

        public TaskInputRef<R> bindInput(TaskNodeKey<R, I> nodeKey, TaskInputBinder inputs) {
            TaskNodeKey<R, I2> dependencyKey = dependencyFactory.apply(nodeKey);
            return inputs.bindInput(dependencyKey);
        }
    }
}
