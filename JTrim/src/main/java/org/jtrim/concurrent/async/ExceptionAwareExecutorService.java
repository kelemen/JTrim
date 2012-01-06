/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ExceptionAwareExecutorService implements ExecutorService {
    private final ExecutorService wrappedExecutor;
    private final ExceptionListener<Object> listener;

    public ExceptionAwareExecutorService(ExecutorService wrappedExecutor,
            ExceptionListener<Object> listener) {

        ExceptionHelper.checkNotNullArgument(wrappedExecutor, "listener");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.wrappedExecutor = wrappedExecutor;
        this.listener = listener;
    }

    @Override
    public void shutdown() {
        wrappedExecutor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return wrappedExecutor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return wrappedExecutor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return wrappedExecutor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return wrappedExecutor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return wrappedExecutor.submit(new ExceptionAwareCallable<>(task, listener));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return wrappedExecutor.submit(new ExceptionAwareRunnable(task, listener), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return wrappedExecutor.submit(new ExceptionAwareRunnable(task, listener));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAll(newTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAll(newTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAny(newTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        ArrayList<Callable<T>> newTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task: tasks) {
            newTasks.add(new ExceptionAwareCallable<>(task, listener));
        }

        return wrappedExecutor.invokeAny(newTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        wrappedExecutor.execute(new ExceptionAwareRunnable(command, listener));
    }

    @Override
    public String toString() {
        return "ExceptionAwareExecutorService{" + wrappedExecutor + '}';
    }
}
