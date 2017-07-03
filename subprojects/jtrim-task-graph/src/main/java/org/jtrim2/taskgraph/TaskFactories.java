package org.jtrim2.taskgraph;

import java.util.Objects;
import java.util.function.Function;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.SyncTaskExecutor;

/**
 * Defines utility methods to implement task node factories.
 */
public final class TaskFactories {
    /**
     * Returns a {@code TaskFactoryKey} with the same properties as a given {@code TaskFactoryKey}
     * but with its {@link TaskFactoryKey#getKey() custom key} replaced.
     *
     * @param <R> the return type of the task factory
     * @param <I> the factory argument type of the task factory
     * @param src the factory key to copy. This argument cannot be {@code null}.
     * @param newKey the new custom key of the returned factory key. This argument can be
     *   {@code null}.
     * @return a {@code TaskFactoryKey} with the same properties as a given {@code TaskFactoryKey}
     *   but with its {@link TaskFactoryKey#getKey() custom key} replaced. This method never
     *   returns {@code null}.
     */
    public static <R, I> TaskFactoryKey<R, I> withCustomKey(TaskFactoryKey<R, I> src, Object newKey) {
        return new TaskFactoryKey<>(src.getResultType(), src.getFactoryArgType(), newKey);
    }

    /**
     * Returns a {@code TaskNodeKey} with the same properties as a given {@code TaskNodeKey}
     * but with its {@link TaskFactoryKey#getKey() custom factory key} replaced.
     *
     * @param <R> the return type of the task factory
     * @param <I> the factory argument type of the task factory
     * @param src the task node to copy. This argument cannot be {@code null}.
     * @param newKey the new custom key of the returned task node key. This argument can be
     *   {@code null}.
     * @return a {@code TaskNodeKey} with the same properties as a given {@code TaskNodeKey}
     *   but with its {@link TaskFactoryKey#getKey() custom factory key} replaced. This method
     *   never returns {@code null}.
     */
    public static <R, I> TaskNodeKey<R, I> withCustomKey(TaskNodeKey<R, I> src, Object newKey) {
        return new TaskNodeKey<>(withCustomKey(src.getFactoryKey(), newKey), src.getFactoryArg());
    }

    /**
     * Creates a task factory which delegates its call to another already declared task factory
     * with a selected {@link TaskFactoryKey#getKey() custom key}. That is, when the returned factory
     * is invoked with a particular task node key, the factory will create a node depending and
     * returning the result of a node with the same node key, except that the custom key of the
     * task factory key will be replaced with the newly selected custom key.
     * <P>
     * This method was designed to help with the case, when there are multiple ways to calculate
     * a particular value but one has to be selected based on the factory argument.
     * <P>
     * The returned factory will always set its executor to a cheap synchronous task executor
     * because the node it creates will simply return its input (i.e., the output of the
     * selected node).
     *
     * @param <R> the return type of the task factory
     * @param <I> the factory argument type of the task factory
     * @param customKeySelector the function selecting the new custom key of the task factory argument
     *   to which the returned factory will delegate its call to. This argument cannot be
     *   {@code null}.
     * @return a task factory which delegates its call to another already declared task factory
     *   with a selected {@link TaskFactoryKey#getKey() custom key}. This method never returns
     *   {@code null}.
     */
    public static <R, I> TaskFactory<R, I> delegateToCustomKey(
            Function<? super I, ?> customKeySelector) {
        Objects.requireNonNull(customKeySelector, "customKeySelector");

        return (CancellationToken cancelToken, TaskNodeCreateArgs<R, I> nodeDef) -> {
            nodeDef.properties().setExecutor(SyncTaskExecutor.getSimpleExecutor());

            TaskNodeKey<R, I> nodeKey = nodeDef.nodeKey();
            I factoryArg = nodeKey.getFactoryArg();

            Object newCustomKey = customKeySelector.apply(factoryArg);
            if (Objects.equals(newCustomKey, nodeKey.getFactoryKey().getKey())) {
                throw new IllegalStateException();
            }

            TaskInputRef<R> resultRef = nodeDef.inputs().bindInput(withCustomKey(nodeKey, newCustomKey));
            return taskCancelToken -> resultRef.consumeInput();
        };
    }

    private TaskFactories() {
        throw new AssertionError();
    }
}
