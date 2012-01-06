package org.jtrim.access;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jtrim.concurrent.*;
import org.jtrim.event.*;
import org.jtrim.utils.*;

/**
 * A helper class for creating REW (read, evaluate and write) frameworks.
 * The following REW frameworks are provided in JTrim:
 * <ul>
 * <li>{@code org.jtrim.access.query}</li>
 * <li>{@code org.jtrim.access.task}</li>
 * </ul>
 * These frameworks build in the idea that in many cases when an application
 * wants a work done it first needs to fetch the input (read), process it
 * some way (evaluate) then finally store the result somewhere (write).
 * <P>
 * This implementation is effectively a
 * {@link org.jtrim.concurrent.MultiPhaseTask MultiPhaseTask} and this task
 * can be manipulated by using the {@link #task task} field. Using this class
 * will help to automatically cancel the underlying {@code MultiPhaseTask}
 * if either the {@link #readToken read token} or the
 * {@link #writeToken write token} was shutted down.
 * <P>
 * <B>To start the REW task the {@link #start(boolean) start} method must be
 * called.</B>
 *
 * @param <ResultType> the type of the result of the underlying
 *   {@code MultiPhaseTask}. The result of this task is returned after no
 *   more writes are done. In most cases this result is irrelevant, so this
 *   type can be anything in the common case (preferably {@link Void}).
 *
 * @see org.jtrim.access.query.RewQueryExecutor
 * @see org.jtrim.access.task.RewTaskExecutor
 * @author Kelemen Attila
 */
public abstract class RewBase<ResultType>
implements
        MultiPhaseTask.TerminateListener<ResultType> {

    /**
     * The underlying {@code MultiPhaseTask} to be used. Subtasks such as
     * reads and writes should be submitted through this {@code MultiPhaseTask}.
     * Note that this task will be canceled in case any of the provided
     * {@link AccessToken AccessTokens} terminate.
     * <P>
     * This field is never {@code null}.
     */
    protected final MultiPhaseTask<ResultType> task;

    /**
     * The token to be used for the read part of the REW task.
     * In case this token was shutted down the {@link #task task} will be
     * canceled (if not already finished).
     * <P>
     * This field is never {@code null}.
     */
    protected final AccessToken<?> readToken;

    /**
     * The token to be used for the write part of the REW task.
     * In case this token was shutted down the {@link #task task} will be
     * canceled (if not already finished).
     * <P>
     * This field is never {@code null}.
     */
    protected final AccessToken<?> writeToken;

    private final boolean releaseOnTerminate;

    private final AtomicReference<Collection<ListenerRef<?>>> listenerRefs;

    private volatile boolean readDone;
    private volatile boolean writeDone;

    /**
     * Initializes all the protected fields so they can be used.
     * To use this class the {@link #start(boolean) start} method must be called
     * because this constructor only initializes fields and does not register
     * listeners nor submits tasks.
     *
     * @param readToken the token to be used for the read part of the REW task.
     *   This reference will be assigned to the {@link #readToken readToken}
     *   field and cannot be {@code null}.
     * @param writeToken the token to be used for the write part of the REW task.
     *   This reference will be assigned to the {@link #writeToken writeToken}
     *   field and cannot be {@code null}.
     * @param releaseOnTerminate if this argument is {@code true}
     *   the specified {@link AccessToken AccessTokens} will be shutted down
     *   after this {@link #task task} terminates. Note that you must still
     *   call the {@link #start(boolean) start} method for this argument to
     *   take effect.
     *
     * @throws NullPointerException thrown if any of the provided tokens are
     *   {@code null}
     */
    public RewBase(
            AccessToken<?> readToken, AccessToken<?> writeToken,
            boolean releaseOnTerminate) {

        ExceptionHelper.checkNotNullArgument(readToken, "readToken");
        ExceptionHelper.checkNotNullArgument(writeToken, "writeToken");

        this.task = new MultiPhaseTask<>(new TaskTerminateListener());
        this.readToken = readToken;
        this.writeToken = writeToken;
        this.releaseOnTerminate = releaseOnTerminate;
        this.listenerRefs = new AtomicReference<>(null);
    }

    private void registerListeners() {
        ListenerRef<?> readRef = readToken.addAccessListener(new AccessListener() {
            @Override
            public void onLostAccess() {
                if (!readDone) {
                    task.cancel();
                }
            }
        });

        ListenerRef<?> writeRef = writeToken.addAccessListener(new AccessListener() {
            @Override
            public void onLostAccess() {
                if (!writeDone) {
                    task.cancel();
                }
            }
        });

        Collection<ListenerRef<?>> refs = Arrays.asList(readRef, writeRef);
        if (!listenerRefs.compareAndSet(null, refs)) {
            unregisterListeners(refs);
            throw new IllegalStateException("Listeners were already registered"
                    + " probably because of multiple RewBase.start.");
        }
    }

    private void unregisterListeners(Collection<ListenerRef<?>> toRemove) {
        for (ListenerRef<?> ref: toRemove) {
            ref.unregister();
        }
    }

    /**
     * Unregisters the listeners registered in the {@link #start(boolean) start}
     * method. This method call is idempotent but can only be used after the
     * {@link #start(boolean) start} method was called.
     */
    private void unregisterListeners() {
        Collection<ListenerRef<?>> toRemove = listenerRefs.getAndSet(null);
        unregisterListeners(toRemove);
    }

    /**
     * Executes the read part of this REW task. This method must be called
     * exactly once before actually using this {@link #task task}.
     * <P>
     * This method will register listeners with the
     * {@link AccessToken AccessTokens} and will unregister these listeners
     * as soon as possible.
     *
     * @param readNow if {@code true} the
     *   {@link #createReadTask() read part of the REW task} will be executed
     *   synchronously on this thread. If {@code false} the read task will be
     *   submitted to {@link #readToken readToken}.
     * @return the future representing the completion of this REW task.
     *   This task is considered completed when the final write part of this
     *   REW task has finished (see:
     *   {@link MultiPhaseTask#finishTask(java.lang.Object, java.lang.Throwable, boolean) MultiPhaseTask.finishTask(ResultType, Throwable, boolean)}).
     *   This method never returns {@code null}.
     */
    public final Future<ResultType> start(boolean readNow) {

        registerListeners();
        if (readToken.isTerminated() || writeToken.isTerminated()) {
            unregisterListeners();
            task.cancel();
            return task.getFuture();
        }

        Runnable readTask = new ReadWrapper(createReadTask());

        if (releaseOnTerminate) {
            if (readNow) {
                readToken.executeNowAndShutdown(readTask);
            }
            else {
                readToken.executeAndShutdown(readTask);
            }
        }
        else {
            if (readNow) {
                readToken.executeNow(readTask);
            }
            else {
                readToken.execute(readTask);
            }
        }

        return task.getFuture();
    }

    /**
     * Creates the task that executes the read part of this REW task.
     * This method will be used by the {@link #start(boolean) start} method
     * to execute the read task. This task must fetch the input for
     * the evaluate part of the REW task then execute the subsequent parts
     * of the REW task and eventually finish by calling
     * {@link MultiPhaseTask#finishTask(java.lang.Object, java.lang.Throwable, boolean) task.finishTask}).
     * <P>
     * This method must return immediately without doing (possibly) long tasks.
     *
     * @return the {@code Runnable} which executes the read part of the REW
     *   task. This method must never return {@code null}.
     */
    protected abstract Runnable createReadTask();

    private class ReadWrapper implements Runnable {
        private final Runnable subTask;

        public ReadWrapper(Runnable subTask) {
            ExceptionHelper.checkNotNullArgument(subTask, "subTask");
            this.subTask = subTask;
        }

        @Override
        public void run() {
            try {
                subTask.run();
            } finally {
                readDone = true;
            }

        }
    }

    private class TaskTerminateListener
    implements
            MultiPhaseTask.TerminateListener<ResultType> {

        @Override
        public void onTerminate(ResultType result, Throwable exception,
                boolean canceled) {

            try {
                RewBase.this.onTerminate(result, exception, canceled);
            } finally {
                writeDone = true;

                unregisterListeners();

                if (releaseOnTerminate) {
                    readToken.shutdown();
                    writeToken.shutdown();
                }
            }
        }
    }
}
