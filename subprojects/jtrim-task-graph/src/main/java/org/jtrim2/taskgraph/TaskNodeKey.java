package org.jtrim2.taskgraph;

import java.util.Objects;


/**
 * Defines the key uniquely identifying a particular task node.
 *
 * <h3>Thread safety</h3>
 * This class is immutable (with the assumption that the {@link #getFactoryArg() factory argument}
 * is also immutable, and as such, its methods can be safely accessed from multiple concurrent threads.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <R> the return type of the function of the associated task node
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   the node to be created
 *
 * @see TaskGraphBuilder
 * @see TaskFactoryKey
 */
public final class TaskNodeKey<R, I> {
    private final TaskFactoryKey<R, I> factoryKey;
    private final I factoryArg;

    /**
     * Creates a new {@code TaskNodeKey} with the given factory key and argument.
     *
     * @param factoryKey the key identifying the task factory creating the node.
     *   This argument cannot be {@code null}.
     * @param factoryArg the argument passed to the task factory creating the node.
     *   This argument can be {@code null}, if the task factory accepts {@code null} arguments.
     */
    public TaskNodeKey(TaskFactoryKey<R, I> factoryKey, I factoryArg) {
        Objects.requireNonNull(factoryKey, "factoryKey");

        this.factoryKey = factoryKey;
        this.factoryArg = factoryArg;
    }

    /**
     * Returns the key identifying the task factory creating the node.
     *
     * @return the key identifying the task factory creating the node. This method
     *   never returns {@code null}.
     */
    public TaskFactoryKey<R, I> getFactoryKey() {
        return factoryKey;
    }

    /**
     * Returns the argument passed to the task factory creating the node.
     *
     * @return the argument passed to the task factory creating the node.
     *   This method may return {@code null}, if {@code null} was specified in the constructor.
     */
    public I getFactoryArg() {
        return factoryArg;
    }

    /**
     * Returns a {@code TaskNodeKey} with the same properties as this {@code TaskNodeKey}
     * but with its {@link TaskFactoryKey#getKey() custom factory key} replaced.
     *
     * @param newKey the new custom key of the returned task node key. This argument can be
     *   {@code null}.
     * @return a {@code TaskNodeKey} with the same properties as this {@code TaskNodeKey}
     *   but with its {@link TaskFactoryKey#getKey() custom factory key} replaced. This method
     *   never returns {@code null}.
     */
    public TaskNodeKey<R, I> withCustomKey(Object newKey) {
        return new TaskNodeKey<>(factoryKey.withKey(newKey), factoryArg);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(factoryKey);
        hash = 67 * hash + Objects.hashCode(factoryArg);
        return hash;
    }

    /**
     * Returns {@code true} if the passed object is a {@code TaskNodeKey}
     * and identifies the same task node as this {@code TaskNodeKey}.
     *
     * @param obj the other object to which this {@code TaskNodeKey} is compared to.
     *   This argument can be {@code null}, in which case the return value is {@code false}.
     * @return {@code true} if the passed object is a {@code TaskNodeKey}
     *   and identifies the same task node as this {@code TaskNodeKey},
     *   {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final TaskNodeKey<?, ?> other = (TaskNodeKey<?, ?>) obj;
        return Objects.equals(this.factoryKey, other.factoryKey)
                && Objects.equals(this.factoryArg, other.factoryArg);
    }

    /**
     * Returns the string representation of this {@code TaskNodeKey} in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "TaskNodeKey{" + "factoryKey=" + factoryKey + ", factoryArg=" + factoryArg + '}';
    }
}
