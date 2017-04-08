package org.jtrim.taskgraph;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class TaskFactoryKey<R, I> {
    private final Class<R> resultType;
    private final Class<I> factoryArgType;
    private final Object key;

    public TaskFactoryKey(Class<R> resultType, Class<I> factoryArgType) {
        this(resultType, factoryArgType, null);
    }

    public TaskFactoryKey(Class<R> resultType, Class<I> factoryArgType, Object key) {
        ExceptionHelper.checkNotNullArgument(resultType, "resultType");
        ExceptionHelper.checkNotNullArgument(factoryArgType, "factoryArgType");

        this.resultType = resultType;
        this.factoryArgType = factoryArgType;
        this.key = key;
    }

    public Class<R> getResultType() {
        return resultType;
    }

    public Class<I> getFactoryArgType() {
        return factoryArgType;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(resultType);
        hash = 41 * hash + Objects.hashCode(factoryArgType);
        hash = 41 * hash + Objects.hashCode(key);
        return hash;
    }

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

    @Override
    public String toString() {
        return "TaskDefKey{"
                + "resultType=" + resultType.getSimpleName()
                + ", factoryArgType=" + factoryArgType.getSimpleName()
                + ", key=" + key + '}';
    }
}
