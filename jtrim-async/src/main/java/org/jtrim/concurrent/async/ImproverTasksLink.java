package org.jtrim.concurrent.async;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncLinks#convertGradually(Object, List)
 *
 * @author Kelemen Attila
 */
final class ImproverTasksLink<InputType, ResultType>
implements
        AsyncDataLink<ResultType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final InputType input;
    private final RefList<AsyncDataConverter<InputType, ResultType>> transformers;

    public ImproverTasksLink(
            InputType input,
            List<? extends AsyncDataConverter<InputType, ResultType>> transformers) {

        this.input = input;
        this.transformers = new RefLinkedList<>(transformers);

        ExceptionHelper.checkNotNullElements(this.transformers, "transformers");
        if (this.transformers.isEmpty()) {
            throw new IllegalArgumentException("There are no transformations.");
        }
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super ResultType> dataListener) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");

        AsyncDataListener<ResultType> safeListener;
        safeListener = AsyncHelper.makeSafeListener(dataListener);

        TasksState<ResultType> state;
        state = new TasksState<>(cancelToken, transformers.size());

        PartialTask<InputType, ResultType> firstTask;
        firstTask = new PartialTask<>(input, state, 0,
                transformers.getFirstReference(), safeListener);

        firstTask.submit(cancelToken);
        return state;
    }

    @Override
    public String toString() {
        String transformersStr = AsyncFormatHelper.collectionToString(transformers);

        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Transform gradually (");
        AsyncFormatHelper.appendIndented(input, result);
        result.append(")\nUsing ");
        result.append(AsyncFormatHelper.indentText(transformersStr, false));

        return result.toString();
    }

    private static class TasksState<ResultType> implements AsyncDataController {
        private final CancellationToken cancelToken;
        private final int taskCount;
        private final AtomicInteger processedTaskCount;

        public TasksState(CancellationToken cancelToken, int taskCount) {
            this.cancelToken = cancelToken;
            this.taskCount = taskCount;
            this.processedTaskCount = new AtomicInteger(0);
        }

        private double getProgress() {
            return (double)processedTaskCount.get() / (double)taskCount;
        }

        @Override
        public void controlData(Object controlArg) {
        }

        private void incProcessedCount() {
            processedTaskCount.getAndIncrement();
        }

        @Override
        public AsyncDataState getDataState() {
            return new SimpleDataState(toString(), getProgress());
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
            result.append("ImproverTaskState{");

            if (cancelToken.isCanceled() && taskCount > processedTaskCount.get()) {
                result.append("CANCELED");
            }
            else {
                result.append(new DecimalFormat("#.##").format(100.0 * getProgress()));
                result.append("%");
            }

            result.append("}");
            return result.toString();
        }
    }

    private static class PartialTask<InputType, ResultType>
    implements
            CancelableTask {

        private final InputType input;
        private final TasksState<ResultType> state;
        private final int partIndex;
        private final RefList.ElementRef<AsyncDataConverter<InputType, ResultType>> currentPart;
        private final AsyncDataListener<? super ResultType> dataListener;

        public PartialTask(
                InputType input,
                TasksState<ResultType> state,
                int partIndex,
                RefList.ElementRef<AsyncDataConverter<InputType, ResultType>> currentPart,
                AsyncDataListener<? super ResultType> dataListener) {
            this.input = input;
            this.state = state;
            this.partIndex = partIndex;
            this.currentPart = currentPart;
            this.dataListener = dataListener; // must be a safe listener
        }

        public void submit(CancellationToken cancelToken) {
            TaskExecutorService executor;
            executor = currentPart.getElement().getExecutor();
            executor.submit(cancelToken, this, (boolean canceled, Throwable error) -> {
                if (canceled || error != null) {
                    dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
                }
            });
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            RefList.ElementRef<AsyncDataConverter<InputType, ResultType>> nextPart;
            nextPart = currentPart.getNext(1);

            DataConverter<InputType, ResultType> converter;
            converter = currentPart.getElement().getConverter();

            ResultType result = converter.convertData(input);
            dataListener.onDataArrive(result);

            state.incProcessedCount();
            if (nextPart != null && !cancelToken.isCanceled()) {
                PartialTask<InputType, ResultType> nextTask;
                nextTask = new PartialTask<>(input, state,
                        partIndex + 1, nextPart, dataListener);

                nextTask.submit(cancelToken);
            }
            else {
                AsyncReport report = nextPart != null
                        ? AsyncReport.CANCELED
                        : AsyncReport.SUCCESS;

                dataListener.onDoneReceive(report);
            }
        }
    }
}
