package org.jtrim.taskgraph;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class TaskNodeKey<R, I> {
    private final TaskFactoryKey<R, I> factoryKey;
    private final I factoryArg;

    public TaskNodeKey(TaskFactoryKey<R, I> factoryKey, I factoryArg) {
        ExceptionHelper.checkNotNullArgument(factoryKey, "factoryKey");

        this.factoryKey = factoryKey;
        this.factoryArg = factoryArg;
    }

    public TaskFactoryKey<R, I> getFactoryKey() {
        return factoryKey;
    }

    public I getFactoryArg() {
        return factoryArg;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(factoryKey);
        hash = 67 * hash + Objects.hashCode(factoryArg);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final TaskNodeKey<?, ?> other = (TaskNodeKey<?, ?>)obj;
        return Objects.equals(this.factoryKey, other.factoryKey)
                && Objects.equals(this.factoryArg, other.factoryArg);
    }

    @Override
    public String toString() {
        return "TaskNodeKey{" + "factoryKey=" + factoryKey + ", factoryArg=" + factoryArg + '}';
    }
}
