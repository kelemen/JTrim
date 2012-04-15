package org.jtrim.access.task;

/**
 * A convenient base class for {@link RewTask} implementations.
 * This implementation simply maintains a canceled flag and this flag can be
 * queried. The task should periodically poll this flag and return immediately
 * if the canceled flag was set.
 *
 * @param <InputType> the type of the input of this REW task.
 *   See: {@link #readInput() readInput()}
 * @param <OutputType> the type of the output of this REW task.
 *   See: {@link #writeOutput(Object) writeOutput(OutputType)}
 *
 * @author Kelemen Attila
 */
public abstract class AbstractRewTask<InputType, OutputType>
implements
        RewTask<InputType, OutputType> {

    private volatile boolean canceled;

    /**
     * Initializes this task with the canceled
     */
    public AbstractRewTask() {
        this.canceled = false;
    }

    /**
     * This method does not nothing and returns immediately. Subclasses
     * may override this method if needed.
     *
     * @param progress the current state of progress of the {@code evaluate}
     *   method. This argument can never be {@code null}.
     */
    @Override
    public void writeProgress(TaskProgress<?> progress) {
    }

    /**
     * This method does not nothing and returns immediately. Subclasses
     * may override this method if needed.
     *
     * @param data the data which was reported by the {@code evaluate} method.
     *   This argument can be {@code null}, if the {@code evaluate} method
     *   can report {@code null} values.
     */
    @Override
    public void writeData(Object data) {
    }

    /**
     * Sets the canceled flag. The flag cannot be cleared and therefore after
     * this method call {@link #isCanceled()} will always return {@code true}.
     */
    @Override
    public void cancel() {
        this.canceled = true;
    }

    /**
     * Checks whether the canceled flag was set or not. The flag can be set
     * by calling the {@link #cancel()} method. Implementations should
     * periodically check this flag by calling this method and return
     * immediately after the canceled flag was set.
     *
     * @return {@code true} if the {@link #cancel() cancel} method was called,
     *   {@code false} otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }
}
