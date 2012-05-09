package org.jtrim.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @deprecated MARKED FOR DELETION
 *
 * Defines an {@link ExecutorService} which will forward all the exceptions
 * thrown by submitted tasks to a user defined listener. Other than this
 * all method calls of this class will be forwarded to a specified
 * {@code ExecutorService}.
 *
 * <h3>Thread safety</h3>
 * The thread safety property is derived from the wrapped
 * {@code ExecutorService} and the specified listener.
 *
 * <h4>Synchronization transparency</h4>
 * The <I>synchronization transparent</I> property is derived from the wrapped
 * {@code ExecutorService} and the specified listener. However it is best to
 * assume that this class is not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class ExceptionAwareExecutorService implements ExecutorService {
    private final ExecutorService wrappedExecutor;
    private final ExceptionListener<Object> listener;

    /**
     * Creates a new {@code ExceptionAwareExecutorService} with the given
     * wrapped {@code ExecutorService} and listener. The listener will receive
     * either {@link Runnable} or {@link Callable} objects in its {@code task}
     * argument.
     *
     * @param wrappedExecutor the executor to which method calls will be
     *   forwarded to. This argument cannot be {@code null}.
     * @param listener the listener to be notified when a task throws an
     *   exception. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public ExceptionAwareExecutorService(ExecutorService wrappedExecutor,
            ExceptionListener<Object> listener) {

        ExceptionHelper.checkNotNullArgument(wrappedExecutor, "wrappedExecutor");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.wrappedExecutor = wrappedExecutor;
        this.listener = listener;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        wrappedExecutor.shutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Runnable> shutdownNow() {
        return wrappedExecutor.shutdownNow();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isShutdown() {
        return wrappedExecutor.isShutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isTerminated() {
        return wrappedExecutor.isTerminated();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return wrappedExecutor.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc }
     * @param <T> the type of the result of the specified task
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return wrappedExecutor.submit(
                new ExceptionAwareCallable<>(task, listener));
    }

    /**
     * {@inheritDoc }
     * @param <T> the type of the result of the specified task
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return wrappedExecutor.submit(
                new ExceptionAwareRunnable(task, listener), result);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> submit(Runnable task) {
        return wrappedExecutor.submit(
                new ExceptionAwareRunnable(task, listener));
    }

    /**
     * {@inheritDoc }
     * @param <T> the type of the result of the specified tasks
     */
    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks)
                throws InterruptedException {

        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAll(newTasks);
    }

    /**
     * {@inheritDoc }
     * @param <T> the type of the result of the specified tasks
     */
    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks,
            long timeout,
            TimeUnit unit)
                throws InterruptedException {

        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAll(newTasks, timeout, unit);
    }

    /**
     * {@inheritDoc }
     * @param <T> the type of the result of the specified tasks
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {

        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAny(newTasks);
    }

    /**
     * {@inheritDoc }
     * @param <T> the type of the result of the specified tasks
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
            long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {

        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAny(newTasks, timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(Runnable command) {
        wrappedExecutor.execute(new ExceptionAwareRunnable(command, listener));
    }

    /**
     * Returns the string representation of this executor in no particular
     * format. The string representation will also contain the string
     * representation of the wrapped {@code ExecutorService}.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "ExceptionAwareExecutorService{" + wrappedExecutor + '}';
    }
}
