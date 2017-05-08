package org.jtrim2.taskgraph;

import java.util.Objects;

/**
 * Defines the key uniquely identifying a particular task node factory. With the
 * task factory argument itself, it also uniquely identifies a particular node
 * in the task execution graph.
 *
 *
 * <h3>Thread safety</h3>
 * This class is immutable (with the assumption that the {@link #getKey() custom key} is also immutable,
 * and as such, its methods can be safely accessed from multiple concurrent threads.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <R> the return type of the task nodes created by the defined task node factory
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   a node to be created
 *
 * @see TaskNodeKey
 * @see TaskGraphDefConfigurer
 */
public final class TaskFactoryKey<R, I> {
    private final Class<R> resultType;
    private final Class<I> factoryArgType;
    private final Object key;

    /**
     * Creates a new {@code TaskFactoryKey} with the given properties with a
     * {@code null} {@link #getKey() custom key}.
     *
     * @param resultType the return type of the task nodes created by the defined task node factory.
     *   This argument cannot be {@code null}.
     * @param factoryArgType the type of the argument passed to the task node factory when requested for
     *   a node to be created. This argument cannot be {@code null}.
     */
    public TaskFactoryKey(Class<R> resultType, Class<I> factoryArgType) {
        this(resultType, factoryArgType, null);
    }

    /**
     * Creates a new {@code TaskFactoryKey} with the given properties.
     *
     * @param resultType the return type of the task nodes created by the defined task node factory.
     *   This argument cannot be {@code null}.
     * @param factoryArgType the type of the argument passed to the task node factory when requested for
     *   a node to be created. This argument cannot be {@code null}.
     * @param key a custom key differentiating between {@code TaskFactoryKey} when types are not
     *   enough. This argument can be {@code null}, if not {@code null}: This argument is expected
     *   to have a properly implemented {@code hashCode} and {@code equals} method.
     */
    public TaskFactoryKey(Class<R> resultType, Class<I> factoryArgType, Object key) {
        Objects.requireNonNull(resultType, "resultType");
        Objects.requireNonNull(factoryArgType, "factoryArgType");

        this.resultType = resultType;
        this.factoryArgType = factoryArgType;
        this.key = key;
    }

    /**
     * Returns the return type of the task nodes created by the defined task node factory.
     *
     * @return the return type of the task nodes created by the defined task node factory.
     *   This method never returns {@code null}.
     */
    public Class<R> getResultType() {
        return resultType;
    }

    /**
     * Returns the type of the argument passed to the task node factory when
     * requested for a node to be created.
     *
     * @return the type of the argument passed to the task node factory when
     *   requested for a node to be created. This method never returns {@code null}.
     */
    public Class<I> getFactoryArgType() {
        return factoryArgType;
    }

    /**
     * Returns the custom key differentiating between {@code TaskFactoryKey} instances
     * when types are not enough. Note that this property can be {@code null} if no
     * such differentiation is necessary.
     *
     * @return the custom key differentiating between {@code TaskFactoryKey} instances
     *   when types are not enough. This method may return {@code null}.
     */
    public Object getKey() {
        return key;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(resultType);
        hash = 41 * hash + Objects.hashCode(factoryArgType);
        hash = 41 * hash + Objects.hashCode(key);
        return hash;
    }

    /**
     * Returns {@code true} if the passed object is a {@code TaskFactoryKey}
     * and identifies the same task node factory as this {@code TaskFactoryKey}.
     *
     * @param obj the other object to which this {@code TaskNodeKey} is compared to.
     *   This argument can be {@code null}, in which case the return value is {@code false}.
     * @return {@code true} if the passed object is a {@code TaskFactoryKey}
     *   and identifies the same task node factory as this {@code TaskFactoryKey},
     *   {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final TaskFactoryKey<?, ?> other = (TaskFactoryKey<?, ?>)obj;
        return this.resultType == other.resultType
                && this.factoryArgType == other.factoryArgType
                && Objects.equals(this.key, other.key);
    }

    /**
     * Returns the string representation of this {@code TaskFactoryKey} in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "TaskFactoryKey{"
                + "resultType=" + resultType.getSimpleName()
                + ", factoryArgType=" + factoryArgType.getSimpleName()
                + ", key=" + key + '}';
    }
}
