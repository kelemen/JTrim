package org.jtrim.concurrent;

/**
 *
 * @author Kelemen Attila
 */
public final class TaskExecutors {
    /**
     * Returns an {@code TaskExecutorService} forwarding all of its methods to
     * the given {@code TaskExecutorService} but the returned
     * {@code TaskExecutorService} cannot be shutted down. Attempting to
     * shutdown the returned {@code ExecutorService} results in an unchecked
     * {@code UnsupportedOperationException} to be thrown.
     *
     * @param executor the executor to which calls to be forwarded by the
     *   returned {@code TaskExecutorService}. This argument cannot be
     *   {@code null}.
     * @return an {@code TaskExecutorService} which forwards all of its calls to
     *   the specified executor but cannot be shutted down. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static TaskExecutorService asUnstoppableExecutor(
            TaskExecutorService executor) {
        return new UnstoppableTaskExecutor(executor);
    }

    private TaskExecutors() {
        throw new AssertionError();
    }
}
