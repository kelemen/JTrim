package org.jtrim2.swing.concurrent.async;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationController;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.query.AsyncDataController;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.AsyncDataState;
import org.jtrim2.concurrent.query.AsyncHelper;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.SimpleDataController;
import org.jtrim2.concurrent.query.SimpleDataState;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CancelableTasks;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.UpdateTaskExecutor;

/**
 * An implementation of {@code AsyncRendererFactory} which executes rendering
 * tasks in a {@link TaskExecutor} specified at construction time. That is,
 * the {@link DataRenderer#startRendering(CancellationToken)},
 * {@link DataRenderer#render(CancellationToken, Object) DataRenderer.render(CancellationToken, DataType)}
 * and the {@link DataRenderer#finishRendering(CancellationToken, AsyncReport)}
 * methods will be executed in the context of the {@code TaskExecutor}.
 * <P>
 * As required by the {@code AsyncRendererFactory} interface,
 * {@link AsyncRenderer} created by {@code GenericAsyncRendererFactory} are
 * independent in a way that no two {@code AsyncRenderer} instances will
 * overwrite each other's rendering requests.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Instances of this class are not <I>synchronization transparent</I>.
 *
 * @see AsyncRenderer
 * @see org.jtrim2.swing.component.AsyncRenderingComponent
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

    /**
     * Creates a new {@code GenericAsyncRendererFactory} which will execute
     * rendering tasks on the specified {@code TaskExecutor}.
     *
     * @param executor the {@code TaskExecutor} used to execute the submitted
     *   rendering tasks. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public GenericAsyncRendererFactory(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
    }

    /**
     * {@inheritDoc }
     */
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
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(renderer, "renderer");

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
        private final CancellationController cancelController;
        private final CancellationToken cancelToken;
        private final AsyncDataLink<DataType> dataLink;
        private final DataRenderer<? super DataType> renderer;

        private final TaskExecutor rendererExecutor;
        private final UpdateTaskExecutor dataExecutor;

        private final AtomicReference<Runnable> onFinishTaskRef;
        private final AtomicReference<RenderTask<?>> nextTaskRef;

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
            assert asyncRenderer != null;
            assert cancelToken != null;
            assert dataLink != null;
            assert renderer != null;

            CancellationSource taskCancelSource = Cancellation.createCancellationSource();
            this.cancelToken = Cancellation.anyToken(taskCancelSource.getToken(), cancelToken);
            this.cancelController = taskCancelSource.getController();
            this.renderer = renderer;
            this.dataLink = dataLink;
            this.asyncRenderer = asyncRenderer;
            this.startTime = System.nanoTime();
            this.finished = false;
            this.dataController = null;

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

            // If we were already marked replacable it is possible that
            // we did not have a nextTaskRef at that time, so check it now just
            // in case.
            if (replacable) {
                cancel();
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
            if (cancelToken.isCanceled()) {
                // cancel has already been called, so do not execute this task.
                completeThisTask();
                return;
            }

            addFinishTask(this::completeThisTask);

            final AtomicBoolean startedRendering = new AtomicBoolean(false);

            rendererExecutor.execute(cancelToken, (CancellationToken rendererCancelToken) -> {
                startedRendering.set(true);
                boolean mayReplace = renderer.startRendering(rendererCancelToken);
                if (mayReplace) {
                    mayFetchNextTask();
                }
            }, null);

            AsyncDataListener<DataType> dataListener = new AsyncDataListener<DataType>() {
                @Override
                public void onDataArrive(final DataType data) {
                    final boolean promisedToBeSignificant;
                    if (nextTaskRef.get() != null && renderer.willDoSignificantRender(data)) {
                        cancel();
                        promisedToBeSignificant = true;
                    }
                    else {
                        promisedToBeSignificant = false;
                    }

                    dataExecutor.execute(() -> {
                        if (startedRendering.get()) {
                            boolean mayReplace = renderer.render(cancelToken, data);
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
                    });
                }

                @Override
                public void onDoneReceive(final AsyncReport report) {
                    CancelableTask noop = CancelableTasks.noOpCancelableTask();
                    rendererExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, noop, (canceled, error) -> {
                        try {
                            if (startedRendering.get()) {
                                renderer.finishRendering(cancelToken, report);
                            }
                        } finally {
                            Runnable finishTask = onFinishTaskRef.getAndSet(null);
                            if (finishTask != null) {
                                finishTask.run();
                            }
                        }
                    });
                }
            };
            AsyncDataListener<DataType> safeListener = AsyncHelper.makeSafeListener(dataListener);

            ListenerRef cancelRef = cancelToken.addCancellationListener(() -> {
                safeListener.onDoneReceive(AsyncReport.CANCELED);
            });

            addFinishTask(cancelRef::unregister);

            dataController = dataLink.getData(cancelToken, safeListener);
        }

        private void setFinished() {
            finished = true;
        }

        private void cancel() {
            cancelController.cancel();
        }

        // This method is called only once and only after all the rendering
        // has completed.
        private void completeThisTask() {
            setFinished();

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
            assert task1 != null;
            assert task2 != null;

            this.task1 = task1;
            this.task2 = task2;
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
        public boolean startRendering(CancellationToken cancelToken) {
            return true;
        }

        @Override
        public boolean willDoSignificantRender(Object data) {
            return true;
        }

        @Override
        public boolean render(CancellationToken cancelToken, Object data) {
            return true;
        }

        @Override
        public void finishRendering(CancellationToken cancelToken, AsyncReport report) {
        }
    }

    private static final class DummyDataLink implements AsyncDataLink<Object> {
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
