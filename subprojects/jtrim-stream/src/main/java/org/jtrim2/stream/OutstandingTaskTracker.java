package org.jtrim2.stream;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;

final class OutstandingTaskTracker {
    private final Runnable onDoneTask;

    private final AtomicInteger outstandingTaskCountRef;
    private volatile boolean finishedAdding;
    private final WaitableSignal finishedAll;

    public OutstandingTaskTracker(Runnable onDoneTask) {
        this.onDoneTask = Tasks.runOnceTask(onDoneTask);
        this.outstandingTaskCountRef = new AtomicInteger();
        this.finishedAdding = false;
        this.finishedAll = new WaitableSignal();
    }

    public TaskRef startTask() {
        if (finishedAdding) {
            throw new IllegalStateException("May not start a new task after finishAddingTasks.");
        }

        Runnable releaseTask = Tasks.runOnceTask(this::releaseOne);
        outstandingTaskCountRef.incrementAndGet();
        return releaseTask::run;
    }

    public void finishAddingTasks() {
        finishedAdding = true;
        if (outstandingTaskCountRef.get() == 0) {
            finish();
        }
    }

    private void releaseOne() {
        if (outstandingTaskCountRef.decrementAndGet() == 0) {
            if (finishedAdding) {
                finish();
            }
        }
    }

    private void finish() {
        try {
            onDoneTask.run();
        } finally {
            finishedAll.signal();
        }
    }

    public void waitForAllTasks() {
        finishedAll.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
    }

    // For testing purposes only
    boolean isFinishedAll() {
        return finishedAll.isSignaled();
    }

    public interface TaskRef {
        public void finishedTask();
    }
}
