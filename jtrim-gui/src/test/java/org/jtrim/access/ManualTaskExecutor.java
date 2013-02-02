package org.jtrim.access;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutionException;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class ManualTaskExecutor implements TaskExecutor {
    private final Lock mainLock;
    private final List<TaskExecutorJob> jobs;

    public ManualTaskExecutor() {
        this.mainLock = new ReentrantLock();
        this.jobs = new LinkedList<>();
    }

    public boolean tryExecuteOne() {
        TaskExecutorJob job;
        mainLock.lock();
        try {
            job = jobs.isEmpty() ? null : jobs.remove(0);
        } finally {
            mainLock.unlock();
        }
        if (job != null) {
            try {
                job.execute();
            } catch (Throwable ex) {
                throw new TaskExecutionException(ex);
            }
            return true;
        }
        else {
            return false;
        }
    }

    public int executeCurrentlySubmitted() {
        TaskExecutorJob[] currentJobs;
        mainLock.lock();
        try {
            currentJobs = jobs.toArray(new TaskExecutorJob[jobs.size()]);
            jobs.clear();
        } finally {
            mainLock.unlock();
        }
        TaskExecutionException toThrow = null;
        for (TaskExecutorJob job : currentJobs) {
            try {
                job.execute();
            } catch (Throwable ex) {
                if (toThrow == null)
                    toThrow = new TaskExecutionException(ex);
                else
                    toThrow.addSuppressed(ex);
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return currentJobs.length;
    }

    @Override
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
        TaskExecutorJob job = new TaskExecutorJob(cancelToken, task, cleanupTask);
        mainLock.lock();
        try {
            jobs.add(job);
        } finally {
            mainLock.unlock();
        }
    }

    private static final class TaskExecutorJob {
        private final CancellationToken cancelToken;
        private final CancelableTask task;
        private final CleanupTask cleanupTask;

        public TaskExecutorJob(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            this.cancelToken = cancelToken;
            this.task = task;
            this.cleanupTask = cleanupTask;
        }

        public void execute() throws Exception {
            boolean canceled = false;
            Throwable taskError = null;

            try {
                task.execute(cancelToken);
            } catch (OperationCanceledException ex) {
                canceled = true;
                taskError = ex;
            } catch (Throwable ex) {
                taskError = ex;
            } finally {
                cleanupTask.cleanup(canceled, taskError);
            }
        }
    }
}
