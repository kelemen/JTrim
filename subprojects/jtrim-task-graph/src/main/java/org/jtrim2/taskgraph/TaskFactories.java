package org.jtrim2.taskgraph;

import java.util.Objects;
import java.util.function.Function;

/**
 * Defines utility methods to implement task node factories.
 */
public final class TaskFactories {
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
    public static <R, I> TaskFactory<R, I> delegateToCustomKey(Function<? super I, ?> customKeySelector) {
        Objects.requireNonNull(customKeySelector, "customKeySelector");

        return forwardResult((nodeKey) -> {
            Object newCustomKey = customKeySelector.apply(nodeKey.getFactoryArg());
            return nodeKey.withCustomKey(newCustomKey);
        });
    }

    /**
     * Creates a task factory delegating its call to another already declared task factory with
     * a different {@link TaskFactoryKey#getFactoryArgType() factory argument type}. The conversion
     * between the factory argument must be provided.
     *
     * @param <R> the return type of the computation created by the factories
     * @param <I> the factory argument type of the returned task factory
     * @param <I2> the factory argument type of the dependency
     * @param newArgType the factory argument type of the dependency. This argument
     *   cannot be {@code null}.
     * @param argTransformer the function transforming the factory argument to
     *   the factory argument of the dependency. This argument cannot be {@code null}.
     * @return a task factory delegating its call to another already declared task factory with
     *   a different {@link TaskFactoryKey#getFactoryArgType() factory argument type}. This
     *   method never returns {@code null}.
     */
    public static <R, I, I2> TaskFactory<R, I> forwardResultOfInput(
            Class<I2> newArgType,
            Function<? super I, ? extends I2> argTransformer) {
        Objects.requireNonNull(newArgType, "newArgType");
        Objects.requireNonNull(argTransformer, "argTransformer");

        return forwardResult(src -> {
            I2 newArg = argTransformer.apply(src.getFactoryArg());
            TaskFactoryKey<R, I2> newFactoryKey = src.getFactoryKey().withInputType(newArgType);
            return new TaskNodeKey<>(newFactoryKey, newArg);
        });
    }

    /**
     * Creates a task factory delegating its call to another already declared task factory. That is,
     * when this factory is invoked with a particular task node key, the factory will create node which
     * depends on another based on the given dependency selector function.
     * <P>
     * The returned factory will always set its executor to a cheap synchronous task executor
     * because the node it creates will simply return its input (i.e., the output of the
     * selected node).
     *
     * @param <R> the return type of the computation created by the factories
     * @param <I> the factory argument type of the returned factory
     * @param <I2> the factory argument type of the dependency
     * @param dependencyFactory the function selecting the dependency node whose result is
     *   to be forwarded by the returned factory. The argument of the function is the node key
     *   identifying the node to be created by the returned factory (which cannot be {@code null}.
     *   This argument cannot be {@code null} and the function may not return a {@code null} value.
     * @return the task factory delegating its call to another already declared task factory.
     *   This method never returns {@code null}.
     */
    public static <R, I, I2> TaskFactory<R, I> forwardResult(
            Function<TaskNodeKey<R, I>, TaskNodeKey<R, I2>> dependencyFactory) {
        return new ResultForwarderFactory<>(dependencyFactory);
    }

    /**
     * Creates a task factory creating nodes depending on a node with the same key except with
     * the result type changed. The result of the dependency is converted using the given transformation.
     *
     * @param <R> the return type of the computation created by the return factory
     * @param <I> the factory argument type of the factories
     * @param <R2> the return type of the computation created by the dependency
     * @param dependencyResultType the type of the return type of the dependency. This
     *   argument cannot be {@code null}.
     * @param resultTransformer the transformation converting the result of the dependency.
     *   This argument cannot be {@code null}.
     * @return the task factory creating nodes depending on a node with the same key except with
     *   the result type changed. This method never returns {@code null}.
     */
    public static <R, I, R2> TaskFactory<R, I> transformResult(
            Class<R2> dependencyResultType,
            Function<? super R2, ? extends R> resultTransformer) {
        return new ResultTransformerFactory<>(dependencyResultType, resultTransformer);
    }

    private TaskFactories() {
        throw new AssertionError();
    }
}
