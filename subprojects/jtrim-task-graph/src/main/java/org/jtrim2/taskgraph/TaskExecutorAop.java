package org.jtrim2.taskgraph;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;

/**
 * Contains utility methods to do task before or after a task node.
 */
public final class TaskExecutorAop {
    /**
     * Returns a {@code TaskFactoryDefiner} delegating all calls to the given {@code TaskFactoryDefiner}
     * but wrapping its task nodes using the given {@code TaskNodeWrapper}. The {@code TaskNodeWrapper}
     * can also replace a task node call with something else.
     *
     * @param wrapped the {@code TaskFactoryDefiner} to be wrapped. This argument cannot be
     *   {@code null}.
     * @param aopAction the action defining what to do before and after a task node. This argument
     *   cannot be {@code null}.
     * @return a {@code TaskFactoryDefiner} delegating all calls to the given {@code TaskFactoryDefiner}
     *   but wrapping its task nodes using the given {@code TaskNodeWrapper}. This method never returns
     *   {@code null}.
     */
    public static TaskFactoryDefiner wrapNode(TaskFactoryDefiner wrapped, TaskNodeWrapper aopAction) {
        return wrapFactory(wrapped, new TaskNodeWrapperImpl(aopAction));
    }

    /**
     * Returns a {@code TaskFactoryDefiner} delegating all calls to the given {@code TaskFactoryDefiner}
     * but wrapping its task node factories using the given {@code TaskFactoryWrapper}. The
     * {@code TaskFactoryWrapper} can also replace a task node factory call with something else.
     *
     * @param wrapped the {@code TaskFactoryDefiner} to be wrapped. This argument cannot be
     *   {@code null}.
     * @param aopAction the action defining what to do before and after a task node. This argument
     *   cannot be {@code null}.
     * @return a {@code TaskFactoryDefiner} delegating all calls to the given {@code TaskFactoryDefiner}
     *   but wrapping its task node factories using the given {@code TaskFactoryWrapper}. This method
     *   never returns {@code null}.
     */
    public static TaskFactoryDefiner wrapFactory(TaskFactoryDefiner wrapped, TaskFactoryWrapper aopAction) {
        return new WrapperTaskFactoryDefiner(wrapped, aopAction);
    }

    private static <R, I> TaskFactorySetup<R, I> wrapFactorySetup(
            TaskFactorySetup<R, I> wrapped,
            TaskFactoryKey<R, I> factoryKey,
            TaskFactoryWrapper aopAction) {
        Objects.requireNonNull(wrapped, "wrapped");
        Objects.requireNonNull(factoryKey, "factoryKey");
        Objects.requireNonNull(aopAction, "aopAction");

        return (TaskFactoryProperties properties) -> {
            return aopAction.createTaskNode(properties, factoryKey, wrapped);
        };
    }

    private static final class WrapperTaskFactoryDefiner implements TaskFactoryDefiner {
        private final TaskFactoryDefiner wrapped;
        private final TaskFactoryWrapper aopAction;

        public WrapperTaskFactoryDefiner(TaskFactoryDefiner wrapped, TaskFactoryWrapper aopAction) {
            this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
            this.aopAction = Objects.requireNonNull(aopAction, "aopAction");
        }

        @Override
        public <R, I> TaskFactoryConfig<R, I> defineFactory(
                TaskFactoryKey<R, I> factoryKey,
                TaskFactorySetup<R, I> setup) {

            TaskFactorySetup<R, I> wrappedSetup = wrapFactorySetup(setup, factoryKey, aopAction);
            return wrapped.defineFactory(factoryKey, wrappedSetup);
        }
    }

    private static final class TaskNodeWrapperImpl implements TaskFactoryWrapper {
        private final TaskNodeWrapper aopAction;

        public TaskNodeWrapperImpl(TaskNodeWrapper aopAction) {
            this.aopAction = Objects.requireNonNull(aopAction, "aopAction");
        }

        @Override
        public <R, I> TaskFactory<R, I> createTaskNode(
                TaskFactoryProperties properties,
                TaskFactoryKey<R, I> factoryKey,
                TaskFactorySetup<R, I> wrapped) throws Exception {

            TaskFactory<R, I> wrappedFactory = wrapped.setup(properties);
            return (CancellationToken cancelToken, TaskNodeCreateArgs<R, I> nodeDef) -> {
                return aopAction.createTaskNode(cancelToken, nodeDef, factoryKey, wrappedFactory);
            };
        }
    }

    private TaskExecutorAop() {
        throw new AssertionError();
    }
}
