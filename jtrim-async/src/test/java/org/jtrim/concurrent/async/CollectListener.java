package org.jtrim.concurrent.async;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public class CollectListener<DataType> implements AsyncDataListener<DataType> {
    private final Runnable onDoneCheck;

    private final Queue<DataType> results;
    private final AtomicReference<AsyncReport> reportRef;
    private final WaitableSignal doneSignal;
    private final AtomicReference<String> miscErrorRef;

    public CollectListener() {
        this(Tasks.noOpTask());
    }

    public CollectListener(Runnable onDoneCheck) {
        ExceptionHelper.checkNotNullArgument(onDoneCheck, "onDoneCheck");

        this.onDoneCheck = onDoneCheck;
        this.results = new ConcurrentLinkedQueue<>();
        this.reportRef = new AtomicReference<>(null);
        this.doneSignal = new WaitableSignal();
        this.miscErrorRef = new AtomicReference<>(null);
    }

    private void setMiscError(String error) {
        miscErrorRef.compareAndSet(null, error);
    }

    public String getMiscError() {
        return miscErrorRef.get();
    }

    public boolean isCompleted() {
        return doneSignal.isSignaled();
    }

    public boolean tryWaitCompletion(long timeout, TimeUnit unit) {
        return doneSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    public List<DataType> getResults() {
        return new ArrayList<>(results);
    }

    public AsyncReport getReport() {
        return reportRef.get();
    }

    @Override
    public void onDataArrive(DataType data) {
        results.add(data);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        try {
            try {
                onDoneCheck.run();
            } catch (Throwable checkError) {
                setMiscError(checkError.getMessage());
            }
            if (!reportRef.compareAndSet(null, report)) {
                setMiscError("Report has been sent multiple times.");
            }
        } finally {
            doneSignal.signal();
        }
    }
}
