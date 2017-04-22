package org.jtrim.taskgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines the properties used to control task graph execution.
 * <P>
 * Instances of this class can can be instantiated through its
 * {@link TaskGraphExecutorProperties.Builder TaskGraphExecutorProperties.Builder}.
 * <P>
 * This class maybe extended by more specific implementations of the task execution framework.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safely callable from multiple threads concurrently and
 * the properties of {@code TaskGraphExecutorProperties} cannot be changed.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see TaskGraphBuilder
 */
public class TaskGraphExecutorProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphExecutorProperties.class.getName());

    private final boolean deliverResultOnFailure;
    private final boolean stopOnFailure;
    private final TaskErrorHandler computeErrorHandler;
    private final Set<TaskNodeKey<?, ?>> resultNodeKeys;

    /**
     * Sets the properties of the {@code TaskGraphExecutorProperties} from the current
     * value of the passed {@code Builder}.
     *
     * @param builder the builder from which a snapshot is created from. This argument
     *   cannot be {@code null}.
     */
    protected TaskGraphExecutorProperties(Builder builder) {
        this.deliverResultOnFailure = builder.deliverResultOnFailure;
        this.stopOnFailure = builder.stopOnFailure;
        this.computeErrorHandler = builder.computeErrorHandler;
        this.resultNodeKeys = builder.resultNodeKeys.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(builder.resultNodeKeys));
    }

    /**
     * Returns {@code true} if results of the computation should be delivered even in case of failure.
     * That is, if this flag is set, the task graph execution never completes exceptionally, instead
     * it will always deliver a {@link TaskGraphExecutionResult} (even if canceled). So, if this flag is
     * set, partial results can be retrieved.
     * <P>
     * Note if this flag is set to {@code true}, it is recommended to set
     * {@link #isStopOnFailure() stopOnFailure} to {@code false} as well. Otherwise, computation might
     * get canceled on the first failure (and no partial results are provided).
     *
     * @return {@code true} if results of the computation should be delivered even in case of failure,
     *   {@code false} otherwise
     *
     * @see #isStopOnFailure()
     */
    public final boolean isDeliverResultOnFailure() {
        return deliverResultOnFailure;
    }

    /**
     * Returns {@code true} if execution should be canceled after encountering the first failure.
     *
     * @return {@code true} if execution should be canceled after encountering the first failure,
     *   {@code false} otherwise
     *
     * @see #isDeliverResultOnFailure()
     */
    public final boolean isStopOnFailure() {
        return stopOnFailure;
    }

    /**
     * Returns the callback notified whenever a failure occurs while trying to execute the action of
     * task node. The default callback simply logs the error on <I>SEVERE</I> level.
     *
     * @return the callback notified whenever a failure occurs while trying to execute the action of
     *   task node. This method never returns {@code null}.
     */
    public final TaskErrorHandler getComputeErrorHandler() {
        return computeErrorHandler;
    }

    /**
     * Returns the set of {@code TaskNodeKey} identifying the nodes whose result are to be delivered
     * to the {@link TaskGraphExecutionResult}. Attempting to retrieve the output via of node not specified
     * by the returned set via the {@code TaskGraphExecutionResult} is the error of the caller.
     *
     * @return the set of {@code TaskNodeKey} identifying the nodes whose result are to be delivered
     *   to the {@code TaskGraphExecutionResult}. This method never returns {@code null}.
     */
    public final Set<TaskNodeKey<?, ?>> getResultNodeKeys() {
        return resultNodeKeys;
    }

    private static void logNodeComputeError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    /**
     * The {@code Builder} used to create {@link TaskGraphExecutorProperties} instances.
     *
     * <h3>Thread safety</h3>
     * The methods of this class may not be used from multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     */
    public static class Builder {
        private boolean deliverResultOnFailure;
        private boolean stopOnFailure;
        private TaskErrorHandler computeErrorHandler;
        private final Set<TaskNodeKey<?, ?>> resultNodeKeys;

        /**
         * Initializes the {@code Builder} with the default values:
         * <ul>
         *  <li><B>stopOnFailure</B>: {@code false}</li>
         *  <li><B>deliverResultOnFailure</B>: {@code false}</li>
         *  <li><B>computeErrorHandler</B>: A callback logging the error on <I>SEVERE</I> level.</li>
         *  <li><B>resultNodeKeys</B>: an empty set</li>
         * </ul>
         */
        public Builder() {
            this.stopOnFailure = false;
            this.deliverResultOnFailure = false;
            this.computeErrorHandler = TaskGraphExecutorProperties::logNodeComputeError;
            this.resultNodeKeys = new HashSet<>();
        }

        /**
         * Initializes the {@code Builder} with the values of the given {@code TaskGraphExecutorProperties}.
         * Immediately creating a {@code TaskGraphExecutorProperties} from this {@code Builder} will yield
         * an effectively equivalent {@code TaskGraphExecutorProperties} as the argument.
         *
         * @param defaults the default values used to initialize the {@code Builder}.
         *   This argument cannot be {@code null}.
         */
        public Builder(TaskGraphExecutorProperties defaults) {
            this.deliverResultOnFailure = defaults.deliverResultOnFailure;
            this.stopOnFailure = defaults.isStopOnFailure();
            this.computeErrorHandler = defaults.getComputeErrorHandler();
            this.resultNodeKeys = new HashSet<>(defaults.getResultNodeKeys());
        }

        /**
         * Declares that the result of the node identified by the given {@code TaskNodeKey} is
         * to be delivered to the {@link TaskGraphExecutionResult}. Calling this method with
         * the same key multiple times has the same effect as calling it only once.
         *
         * @param nodeKey the id of the node whose result is to be delivered
         *   to the {@code TaskGraphExecutionResult}. This argument cannot be {@code null}.
         */
        public final void addResultNodeKey(TaskNodeKey<?, ?> nodeKey) {
            ExceptionHelper.checkNotNullArgument(nodeKey, "nodeKey");
            this.resultNodeKeys.add(nodeKey);
        }

        /**
         * Declares that the result of the nodes identified by the given {@code TaskNodeKey}s
         * are to be delivered to the {@link TaskGraphExecutionResult}. Calling this method with
         * the same key multiple times has the same effect as calling it only once.
         *
         * @param nodeKeys the ids of the nodes whose result is to be delivered
         *   to the {@code TaskGraphExecutionResult}. This argument cannot be {@code null}.
         */
        public final void addResultNodeKeys(Collection<? extends TaskNodeKey<?, ?>> nodeKeys) {
            ExceptionHelper.checkNotNullElements(nodeKeys, "nodeKeys");
            this.resultNodeKeys.addAll(nodeKeys);
        }

        /**
         * Sets the flag determining if the result of the computation should be delivered
         * to the {@link TaskGraphExecutionResult} or not. That is, if this flag is set,
         * the task graph execution never completes exceptionally, instead it will always
         * deliver a {@link TaskGraphExecutionResult} (even if canceled). So, if this flag
         * is set, partial results can be retrieved.
         *
         * @param deliverResultOnFailure {@code true} if results on failure must be delivered,
         *   {@code false} otherwise
         *
         * @see #setStopOnFailure(boolean)
         */
        public final void setDeliverResultOnFailure(boolean deliverResultOnFailure) {
            this.deliverResultOnFailure = deliverResultOnFailure;
        }

        /**
         * Sets the flag determining if execution should be canceled after encountering
         * the first failure, or not.
         *
         * @param stopOnFailure {@code true} if execution should be canceled after encountering
         *   the first failure, {@code false} otherwise
         *
         * @see #setDeliverResultOnFailure(boolean)
         */
        public final void setStopOnFailure(boolean stopOnFailure) {
            this.stopOnFailure = stopOnFailure;
        }

        /**
         * Sets the callback notified whenever a failure occurs while trying to execute the action of
         * a node. Setting the callback will override any previously set value.
         *
         * @param computeErrorHandler the callback notified whenever a failure occurs while trying
         *   to execute the action of a node. This argument cannot be {@code null}.
         */
        public final void setComputeErrorHandler(TaskErrorHandler computeErrorHandler) {
            ExceptionHelper.checkNotNullArgument(computeErrorHandler, "computeErrorHandler");
            this.computeErrorHandler = computeErrorHandler;
        }

        /**
         * Creates a snapshot of the current state of this {@code Builder}. Further adjustment of
         * this {@code Builder} will not affect the returned {@code TaskGraphExecutorProperties}.
         *
         * @return a snapshot of the current state of this {@code Builder}. This method may never
         *   return {@code null}.
         */
        public TaskGraphExecutorProperties build() {
            return new TaskGraphExecutorProperties(this);
        }
    }
}
