package org.jtrim.swing.concurrent.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.ChildCancellationSource;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncHelper;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.SimpleDataController;
import org.jtrim.concurrent.async.SimpleDataState;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class GenericAsyncRendererFactory implements AsyncRendererFactory {
    private static final Logger LOGGER = Logger.getLogger(GenericAsyncRendererFactory.class.getName());

    private static final AsyncDataState NOT_STARTED_STATE
            = new SimpleDataState("Data receiving has not yet been started.", 0.0);

    private static final RenderTask<?> POISON_RENDER_TASK = new RenderTask<>(
            new GenericAsyncRenderer(SyncTaskExecutor.getSimpleExecutor()),
            Cancellation.CANCELED_TOKEN,
            DummyDataLink.getInstance(),
            DummyRenderer.INSTANCE);

    private final TaskExecutor executor;

    public GenericAsyncRendererFactory(TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
    }

    @Override
    public AsyncRenderer createRenderer() {
        return new GenericAsyncRenderer(executor);
    }


    private static class GenericAsyncRenderer implements AsyncRenderer {
        private final TaskExecutor executor;
        private final AtomicReference<RenderTask<?>> taskRef;

        public GenericAsyncRenderer(TaskExecutor executor) {
            this.executor = executor;
            this.taskRef = new AtomicReference<>(null);
        }

        @Override
        public <DataType> RenderingState render(
                CancellationToken cancelToken,
                AsyncDataLink<DataType> dataLink,
                DataRenderer<? super DataType> renderer) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(renderer, "renderer");

            RenderTask<DataType> task = dataLink != null
                    ? new RenderTask<>(this, cancelToken, dataLink, renderer)
                    : new RenderTask<>(this, cancelToken, DummyDataLink.<DataType>getInstance(), renderer);

            return task.startTask();
        }

        public RenderTask<?> setTask(RenderTask<?> task) {
            return taskRef.getAndSet(task);
        }

        public RenderTask<?> setTaskIf(RenderTask<?> expectedTask, RenderTask<?> task) {
            RenderTask<?> currentTask;
            do {
                currentTask = taskRef.get();
                if (currentTask != expectedTask) {
                    break;
                }
            } while (!taskRef.compareAndSet(currentTask, task));
            return currentTask;
        }

        public TaskExecutor getExecutor() {
            return executor;
        }
    }

    private static class RenderTask<DataType> implements RenderingState {
        private final GenericAsyncRenderer asyncRenderer;
        private final CancellationToken cancelToken;
        private final AsyncDataLink<DataType> dataLink;
        private final DataRenderer<? super DataType> renderer;

        private final TaskExecutor rendererExecutor;
        private final UpdateTaskExecutor dataExecutor;

        private final AtomicReference<Runnable> onFinishTaskRef;
        private final AtomicReference<RenderTask<?>> nextTaskRef;

        private final AtomicReference<CancellationController> cancelControllerRef;

        // returned by System.nanoTime()
        private final long startTime;

        private volatile AsyncDataController dataController;
        private volatile boolean replacable;
        private volatile boolean finished;

        public RenderTask(
                GenericAsyncRenderer asyncRenderer,
                CancellationToken cancelToken,
                AsyncDataLink<DataType> dataLink,
                DataRenderer<? super DataType> renderer) {
            // asyncRenderer can be null for the POISON_RENDERER_TASK
            assert asyncRenderer != null;
            assert cancelToken != null;
            assert dataLink != null;
            assert renderer != null;

            this.renderer = renderer;
            this.cancelToken = cancelToken;
            this.dataLink = dataLink;
            this.asyncRenderer = asyncRenderer;
            this.startTime = System.nanoTime();
            this.finished = false;
            this.dataController = null;
            this.cancelControllerRef = new AtomicReference<>(null);
            this.onFinishTaskRef = new AtomicReference<>(Tasks.noOpTask());
            this.nextTaskRef = new AtomicReference<>(null);
            this.rendererExecutor = TaskExecutors.inOrderExecutor(asyncRenderer.getExecutor());
            this.replacable = false;
            this.dataExecutor = new GenericUpdateTaskExecutor(this.rendererExecutor);
        }

        private boolean trySetNextTask(RenderTask<?> nextTask) {
            RenderTask<?> currentNextTask;
            do {
                currentNextTask = nextTaskRef.get();
                if (currentNextTask == POISON_RENDER_TASK) {
                    return false;
                }
            } while (!nextTaskRef.compareAndSet(currentNextTask, nextTask));

            if (currentNextTask != null) {
                currentNextTask.setFinished();
            }

            return true;
        }

        private void addFinishTask(final Runnable task) {
            Runnable currentFinishTask;
            Runnable newFinishTask;
            do {
                currentFinishTask = onFinishTaskRef.get();
                if (currentFinishTask == null) {
                    // The task has already completed, so execute the finish
                    // task now.
                    task.run();
                    return;
                }

                newFinishTask = new MultiTask(currentFinishTask, task);
            } while (!onFinishTaskRef.compareAndSet(currentFinishTask, newFinishTask));
        }

        // This method may not be called more than once.
        public RenderingState startTask() {
            while (true) {
                RenderTask<?> currentTask = asyncRenderer.setTaskIf(null, this);
                if (currentTask == null) {
                    // There was no renderTask for this key, so start now.
                    doStartTask();
                    return this;
                }
                else {
                    // There is currently a rendering in progress, so attempt
                    // to attach this task to the current rendering.
                    if (currentTask.trySetNextTask(this)) {
                        // We successfully attached this task to the current
                        // rendering task, so it will be started by that task
                        // when done.

                        if (replacable) {
                            cancel();
                        }
                        return this;
                    }
                }
            }
        }

        private void mayFetchNextTask() {
            replacable = true;
            if (nextTaskRef.get() != null) {
                cancel();
            }
        }

        private void doStartTask() {
            final ChildCancellationSource cancelSource = Cancellation.createChildCancellationSource(cancelToken);
            if (!cancelControllerRef.compareAndSet(null, cancelSource.getController())) {
                // cancel has already been called, so do not execute this task.
                setFinished();
                cancelSource.detachFromParent();
                return;
            }

            addFinishTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        setFinished();
                        cancelSource.detachFromParent();
                    } finally {
                        completeThisTask();
                    }
                }
            });

            final AtomicBoolean startedRendering = new AtomicBoolean(false);

            rendererExecutor.execute(cancelSource.getToken(), new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    startedRendering.set(true);
                    boolean mayReplace = renderer.startRendering();
                    if (mayReplace) {
                        mayFetchNextTask();
                    }
                }
            }, null);

            AsyncDataListener<DataType> dataListener = new AsyncDataListener<DataType>() {
                @Override
                public void onDataArrive(final DataType data) {
                    final boolean promisedToBeSignificant;
                    if (nextTaskRef.get() != null && renderer.willDoSignificantRender(data)) {
                        cancelSource.getController().cancel();
                        promisedToBeSignificant = true;
                    }
                    else {
                        promisedToBeSignificant = false;
                    }

                    dataExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (startedRendering.get()) {
                                boolean mayReplace = renderer.render(data);
                                if (mayReplace) {
                                    mayFetchNextTask();
                                }
                                else if (promisedToBeSignificant) {
                                    LOGGER.log(Level.WARNING,
                                            "willDoSignificantRender reported"
                                                + " that the renderer will do a"
                                                + " significant rendering but"
                                                + " render returned false.");
                                }
                            }
                        }
                    });
                }

                @Override
                public void onDoneReceive(final AsyncReport report) {
                    rendererExecutor.execute(
                            Cancellation.UNCANCELABLE_TOKEN,
                            Tasks.noOpCancelableTask(),
                            new CleanupTask() {
                        @Override
                        public void cleanup(boolean canceled, Throwable error) {
                            try {
                                if (startedRendering.get()) {
                                    renderer.finishRendering(report);
                                }
                            } finally {
                                Runnable finishTask = onFinishTaskRef.getAndSet(null);
                                if (finishTask != null) {
                                    finishTask.run();
                                }
                            }
                        }
                    });
                }
            };
            final AsyncDataListener<DataType> safeListener
                    = AsyncHelper.makeSafeListener(dataListener);

            final ListenerRef cancelRef = cancelSource.getToken().addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    safeListener.onDoneReceive(AsyncReport.CANCELED);
                }
            });

            addFinishTask(new Runnable() {
                @Override
                public void run() {
                    cancelRef.unregister();
                }
            });

            dataController = dataLink.getData(cancelSource.getToken(), safeListener);
        }

        private void setFinished() {
            finished = true;
        }

        private void cancel() {
            CancellationController currentController = cancelControllerRef.getAndSet(
                    Cancellation.DO_NOTHING_CONTROLLER);

            if (currentController != null) {
                currentController.cancel();
            }
        }

        // This method is called only once and only after all the rendering
        // has completed.
        private void completeThisTask() {
            RenderTask<?> nextTask = nextTaskRef.getAndSet(POISON_RENDER_TASK);
            if (nextTask != null) {
                RenderTask<?> currentTask = asyncRenderer.setTask(nextTask);
                nextTask.doStartTask();

                assert currentTask == this;
            }
            else {
                asyncRenderer.setTaskIf(this, null);
            }
        }

        @Override
        public boolean isRenderingFinished() {
            return finished;
        }

        @Override
        public long getRenderingTime(TimeUnit unit) {
            return unit.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }

        @Override
        public AsyncDataState getAsyncDataState() {
            AsyncDataController currentController = dataController;
            return currentController != null
                    ? currentController.getDataState()
                    : NOT_STARTED_STATE;
        }
    }

    private static class MultiTask implements Runnable {
        private final Runnable task1;
        private final Runnable task2;

        public MultiTask(Runnable task1, Runnable task2) {
            this.task1 = task1 != null ? task1 : Tasks.noOpTask();
            this.task2 = task2 != null ? task2 : Tasks.noOpTask();
        }

        @Override
        public void run() {
            try {
                task1.run();
            } finally {
                task2.run();
            }
        }
    }

    private enum DummyRenderer implements DataRenderer<Object> {
        INSTANCE;

        @Override
        public boolean startRendering() {
            return true;
        }

        @Override
        public boolean willDoSignificantRender(Object data) {
            return true;
        }

        @Override
        public boolean render(Object data) {
            return true;
        }

        @Override
        public void finishRendering(AsyncReport report) {
        }
    }

    private final static class DummyDataLink implements AsyncDataLink<Object> {
        private static final DummyDataLink INSTANCE = new DummyDataLink();

        @SuppressWarnings("unchecked")
        private static <DataType> AsyncDataLink<DataType> getInstance() {
            // This cast is safe because the returned link only invokes the
            // onDoneReceive method which is independent of the DataType.
            return (AsyncDataLink<DataType>)INSTANCE;
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                AsyncDataListener<? super Object> dataListener) {
            dataListener.onDoneReceive(AsyncReport.SUCCESS);
            return new SimpleDataController(new SimpleDataState("", 1.0));
        }
    }
}
