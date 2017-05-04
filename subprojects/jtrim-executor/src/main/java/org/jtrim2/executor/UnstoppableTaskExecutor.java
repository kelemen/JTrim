package org.jtrim2.executor;

/**
 * @see TaskExecutors#asUnstoppableExecutor(TaskExecutorService)
 *
 * @author Kelemen Attila
 */
final class UnstoppableTaskExecutor extends DelegatedTaskExecutorService {
    public UnstoppableTaskExecutor(TaskExecutorService wrappedExecutor) {
        super(wrappedExecutor);
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException(
                "This executor cannot be shutted down.");
    }

    @Override
    public void shutdownAndCancel() {
        shutdown();
    }
}
