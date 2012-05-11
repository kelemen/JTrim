package org.jtrim.concurrent;

/**
 * Contains static helper and factory methods for various useful
 * {@link TaskExecutor} and {@link TaskExecutorService} implementations.
 * <P>
 * This class cannot be inherited and instantiated.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are
 * <I>synchronization transparent</I>.
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
