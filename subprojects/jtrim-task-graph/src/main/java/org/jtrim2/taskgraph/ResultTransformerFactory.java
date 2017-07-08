package org.jtrim2.taskgraph;

import java.util.Objects;
import java.util.function.Function;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;

final class ResultTransformerFactory<R, I> implements TaskFactory<R, I> {
    private final ResultTransformer<R, I, ?> resultTransformer;

    public <R2> ResultTransformerFactory(
            Class<R2> dependencyResultType,
            Function<? super R2, ? extends R> resultTransformer) {
        this.resultTransformer = new ResultTransformer<>(dependencyResultType, resultTransformer);
    }

    @Override
    public CancelableFunction<R> createTaskNode(
            CancellationToken cancelToken,
            TaskNodeCreateArgs<R, I> nodeDef) throws Exception {

        TaskInputRef<R> resultRef = resultTransformer.bindInput(nodeDef.nodeKey(), nodeDef.inputs());
        return taskCancelToken -> resultRef.consumeInput();
    }

    private static final class ResultTransformer<R, I, R2> {
        private final Class<R2> dependencyResultType;
        private final Function<? super R2, ? extends R> resultTransformer;

        public ResultTransformer(
                Class<R2> dependencyResultType,
                Function<? super R2, ? extends R> resultTransformer) {
            this.dependencyResultType = Objects.requireNonNull(dependencyResultType, "dependencyResultType");
            this.resultTransformer = Objects.requireNonNull(resultTransformer, "resultTransformer");
        }

        public TaskInputRef<R> bindInput(TaskNodeKey<R, I> srcKey, TaskInputBinder inputs) {
            TaskNodeKey<R2, I> dependencyKey = srcKey.withResultType(dependencyResultType);
            TaskInputRef<R2> resultRef = inputs.bindInput(dependencyKey);
            return convertResult(resultRef, resultTransformer);
        }

        private static <R, R2> TaskInputRef<R> convertResult(
                TaskInputRef<R2> srcRef,
                Function<? super R2, ? extends R> resultTransformer) {
            return () -> {
                R2 result = srcRef.consumeInput();
                return resultTransformer.apply(result);
            };
        }
    }
}
