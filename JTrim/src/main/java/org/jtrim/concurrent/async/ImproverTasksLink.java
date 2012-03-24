package org.jtrim.concurrent.async;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncDatas#convertGradually(Object, List)
 *
 * @author Kelemen Attila
 */
final class ImproverTasksLink<InputType, ResultType>
implements
        AsyncDataLink<ResultType> {

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
            AsyncDataListener<? super ResultType> dataListener) {

        AsyncDataListener<ResultType> safeListener;
        safeListener = AsyncDatas.makeSafeListener(dataListener);

        TasksState<ResultType> state;
        state = new TasksState<>(transformers.size(), safeListener);

        PartialTask<InputType, ResultType> firstTask;
        firstTask = new PartialTask<>(input, state, 0,
                transformers.getFirstReference(), safeListener);

        firstTask.submit();
        return state;
    }

    @Override
    public String toString() {
        String transformersStr = AsyncFormatHelper.collectionToString(transformers);

        StringBuilder result = new StringBuilder(256);
        result.append("Transform gradually (");
        AsyncFormatHelper.appendIndented(input, result);
        result.append(")\nUsing ");
        result.append(AsyncFormatHelper.indentText(transformersStr, false));

        return result.toString();
    }

    private static class TasksState<ResultType>
    implements
            AsyncDataState, AsyncDataController {

        private final AsyncDataListener<ResultType> safeListener;
        private final int taskCount;
        private final AtomicInteger processedTaskCount;
        private volatile boolean canceled;
        private final AtomicStampedReference<Future<?>> currentFuture;

        public TasksState(int taskCount,
                AsyncDataListener<ResultType> safeListener) {
            this.taskCount = taskCount;
            this.safeListener = safeListener;
            this.processedTaskCount = new AtomicInteger(0);
            this.canceled = false;
            this.currentFuture = new AtomicStampedReference<>(null, -1);
        }

        @Override
        public double getProgress() {
            return (double)processedTaskCount.get() / (double)taskCount;
        }

        @Override
        public void controlData(Object controlArg) {
        }

        private boolean isCanceled() {
            return canceled;
        }

        @Override
        public void cancel() {
            canceled = true;
            Future<?> future = currentFuture.getReference();
            if (future != null) {
                future.cancel(true);
            }

            safeListener.onDoneReceive(AsyncReport.CANCELED);
        }

        private void incProcessedCount() {
            processedTaskCount.getAndIncrement();
        }

        private void setFuture(Future<?> future, int futureIndex) {
            assert futureIndex < taskCount;

            int[] currentIndex = new int[1];
            Future<?> current;
            current = currentFuture.get(currentIndex);

            // Note that since the index can only increase,
            // this loop will end in at most taskCount rounds.
            while (currentIndex[0] < futureIndex) {
                if (currentFuture.compareAndSet(current, future,
                        currentIndex[0], futureIndex)) {
                    break;
                }
            }

            if (canceled) {
                // Since there is no lock, a concurrent call to cancel
                // may cause an unnecessary cancel call but this is unlikely
                // and has no consequences because cancel is idempotent.
                future.cancel(true);
            }
        }

        @Override
        public AsyncDataState getDataState() {
            return this;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(128);
            result.append("ImproverTaskState{");

            if (isCanceled() && taskCount > processedTaskCount.get()) {
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
            Runnable {

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
            this.dataListener = dataListener;
        }

        public void submit() {
            ExecutorService executor;
            executor = currentPart.getElement().getExecutor();

            Future<?> future;
            future = executor.submit(this);
            state.setFuture(future, partIndex);
        }

        @Override
        public void run() {
            if (state.isCanceled()) {
                dataListener.onDoneReceive(AsyncReport.CANCELED);
                return;
            }

            RefList.ElementRef<AsyncDataConverter<InputType, ResultType>> nextPart;
            nextPart = currentPart.getNext(1);

            DataConverter<InputType, ResultType> converter;
            converter = currentPart.getElement().getConverter();

            try {
                ResultType result = converter.convertData(input);
                dataListener.onDataArrive(result);
            } catch (Throwable ex) {
                dataListener.onDoneReceive(AsyncReport.getReport(ex, true));
                throw ex;
            }

            state.incProcessedCount();
            if (nextPart != null && !state.isCanceled()) {
                PartialTask<InputType, ResultType> nextTask;
                nextTask = new PartialTask<>(input, state, partIndex + 1,
                        nextPart, dataListener);

                nextTask.submit();
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