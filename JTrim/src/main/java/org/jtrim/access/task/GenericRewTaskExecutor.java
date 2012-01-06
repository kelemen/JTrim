package org.jtrim.access.task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jtrim.access.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.*;

/**
 * A {@link RewTaskExecutor} implementation which executes the
 * {@link RewTask#evaluate(java.lang.Object, org.jtrim.access.task.RewTaskReporter) evaluate part}
 * of the submitted REW tasks on a specific
 * {@link java.util.concurrent.ExecutorService ExecutorService}.
 * <P>
 * This implementation will allow reporting the progress of the evaluate part
 * of the REW task until the specified write
 * {@link org.jtrim.access.AccessToken AccessToken} is terminated.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are completely thread-safe and the methods can be
 * called from any thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I> unless
 * otherwise noted but they will not wait for asynchronous or external tasks to
 * complete.
 *
 * @author Kelemen Attila
 */
public final class GenericRewTaskExecutor implements RewTaskExecutor {
    private final ExecutorService evaluateExecutor;

    /**
     * Initializes this REW task executor with the specified
     * {@code ExecutorService}. This {@code ExecutorService} will be used
     * to execute the evaluate part of the submitted REW task.
     *
     * @param evaluateExecutor the executor used to execute the evaluate part
     *   of submitted REW tasks. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public GenericRewTaskExecutor(ExecutorService evaluateExecutor) {
        ExceptionHelper.checkNotNullArgument(evaluateExecutor, "evaluateExecutor");

        this.evaluateExecutor = evaluateExecutor;
    }

    private <I, O> Future<?> executeGeneric(RewTask<I, O> task,
            AccessToken<?> readToken, AccessToken<?> writeToken,
            boolean releaseOnTerminate, boolean readNow) {

        RewFutureTask<I, O> futureTask;
        futureTask = new RewFutureTask<>(evaluateExecutor, task,
                readToken, writeToken, releaseOnTerminate);

        return futureTask.start(readNow);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> execute(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(task, readToken, writeToken, false, false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> executeAndRelease(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(task, readToken, writeToken, true, false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> executeNow(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(task, readToken, writeToken, false, true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> executeNowAndRelease(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(task, readToken, writeToken, true, true);
    }

    private static class RewFutureTask<InputType, OutputType>
    extends
            RewBase<Object> {

        private final ExecutorService evaluateExecutor;
        private final RewTask<InputType, OutputType> rewTask;
        private final AtomicReference<Future<?>> evaluateFuture;

        public RewFutureTask(
                ExecutorService evaluateExecutor,
                RewTask<InputType, OutputType> rewTask,
                AccessToken<?> readToken, AccessToken<?> writeToken,
                boolean releaseOnTerminate) {
            super(readToken, writeToken, releaseOnTerminate);

            ExceptionHelper.checkNotNullArgument(evaluateExecutor, "evaluateExecutor");
            ExceptionHelper.checkNotNullArgument(rewTask, "rewTask");

            this.evaluateExecutor = evaluateExecutor;
            this.rewTask = rewTask;
            this.evaluateFuture = new AtomicReference<>(null);
        }

        private void setFuture(Future<?> future) {
            if (!evaluateFuture.compareAndSet(null, future)) {
                future.cancel(true);
            }
        }

        @Override
        protected Runnable createReadTask() {
            return new Runnable() {
                @Override
                public void run() {
                    InputType input = task.executeSubTask(new InputReader());
                    if (!task.isDone()) {
                        Future<?> future = task.submitSubTask(
                                evaluateExecutor,
                                new GenericRewEvaluateTask(input));

                        setFuture(future);
                    }
                }
            };
        }

        @Override
        public void onTerminate(Object result, Throwable exception,
                boolean canceled) {

            if (canceled) {
                rewTask.cancel();

                Future<?> oldFuture;
                oldFuture = evaluateFuture.getAndSet(ExecutorsEx.canceledFuture());

                if (oldFuture != null) {
                    oldFuture.cancel(true);
                }
            }
        }

        private class InputReader implements Callable<InputType> {
            @Override
            public InputType call() {
                return rewTask.readInput();
            }
        }

        private class OutputForwarder implements Runnable {
            private final OutputType data;

            public OutputForwarder(OutputType data) {
                this.data = data;
            }

            @Override
            public void run() {
                Throwable error = null;
                try {
                    rewTask.writeOutput(data);
                } catch (Throwable ex) {
                    error = ex;
                }
                task.finishTask(null, error, false);
            }
        }

        private class GenericRewEvaluateTask implements Callable<Object> {
            private final InputType input;

            public GenericRewEvaluateTask(InputType input) {
                this.input = input;
            }

            @Override
            public Object call() throws InterruptedException {
                OutputType output;
                output = rewTask.evaluate(input, new GenericRewReporter());
                task.submitSubTask(writeToken, new OutputForwarder(output));
                return null;
            }
        }

        private class GenericRewReporter implements RewTaskReporter {
            private final UpdateTaskExecutor progressExecutor;

            public GenericRewReporter() {
                this.progressExecutor
                        = new GenericUpdateTaskExecutor(
                                new InOrderExecutor(writeToken));
            }

            @Override
            public void reportProgress(double progress) {
                reportProgress(new TaskProgress<>(progress, null));
            }

            @Override
            public void reportProgress(final TaskProgress<?> progress) {
                progressExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        rewTask.writeProgress(progress);
                    }
                });
            }

            @Override
            public void reportData(final Object data) {
                writeToken.execute(new Runnable() {
                    @Override
                    public void run() {
                        rewTask.writeData(data);
                    }
                });
            }
        }
    }

    @Override
    public String toString() {
        return "GenericRewExecutor{" + evaluateExecutor + '}';
    }
}
