package org.jtrim.concurrent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ManualExecutor extends AbstractTaskExecutorService {
    private ListenerManager<Runnable, Void> listeners = new CopyOnTriggerListenerManager<>();
    private boolean shuttedDown = false;
    private final List<SubmittedTask> submittedTasks = new LinkedList<>();

    public void executeSubmittedTasksWithoutRemoving() {
        try {
            for (SubmittedTask task : submittedTasks) {
                task.task.execute(task.cancelToken);
                task.cleanupTask.run();
            }
        } catch (Exception ex) {
            ExceptionHelper.rethrow(ex);
        }
    }

    public void executeOne() throws Exception {
        SubmittedTask task = submittedTasks.remove(0);
        task.task.execute(task.cancelToken);
        task.cleanupTask.run();
    }

    public void executeSubmittedTasks() {
        try {
            executeSubmittedTasksMayFail();
        } catch (Exception ex) {
            ExceptionHelper.rethrow(ex);
        }
    }

    private void executeSubmittedTasksMayFail() throws Exception {
        if (shuttedDown) {
            while (!submittedTasks.isEmpty()) {
                SubmittedTask task = submittedTasks.remove(0);
                task.cleanupTask.run();
            }
        }
        else {
            while (!submittedTasks.isEmpty()) {
                executeOne();
            }
        }
    }

    @Override
    protected void submitTask(CancellationToken cancelToken, CancellationController cancelController, CancelableTask task, Runnable cleanupTask, boolean hasUserDefinedCleanup) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(cancelController, "cancelController");
        ExceptionHelper.checkNotNullArgument(task, "task");
        ExceptionHelper.checkNotNullArgument(cleanupTask, "cleanupTask");
        submittedTasks.add(new SubmittedTask(cancelToken, cancelController, task, cleanupTask, hasUserDefinedCleanup));
    }

    @Override
    public void shutdown() {
        shuttedDown = true;
        ListenerManager<Runnable, Void> currentListeners = listeners;
        if (currentListeners != null) {
            listeners = null;
            currentListeners.onEvent(new EventDispatcher<Runnable, Void>() {
                @Override
                public void onEvent(Runnable eventListener, Void arg) {
                    eventListener.run();
                }
            }, null);
        }
    }

    @Override
    public void shutdownAndCancel() {
        shutdown();
    }

    @Override
    public boolean isShutdown() {
        return shuttedDown;
    }

    @Override
    public boolean isTerminated() {
        return shuttedDown;
    }

    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        if (listeners == null) {
            listener.run();
            return UnregisteredListenerRef.INSTANCE;
        }
        else {
            return listeners.registerListener(listener);
        }
    }

    @Override
    public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return shuttedDown;
    }

    private static class SubmittedTask {

        public final CancellationToken cancelToken;
        public final CancellationController cancelController;
        public final CancelableTask task;
        public final Runnable cleanupTask;
        public final boolean hasUserDefinedCleanup;

        public SubmittedTask(CancellationToken cancelToken, CancellationController cancelController, CancelableTask task, Runnable cleanupTask, boolean hasUserDefinedCleanup) {
            this.cancelToken = cancelToken;
            this.cancelController = cancelController;
            this.task = task;
            this.cleanupTask = cleanupTask;
            this.hasUserDefinedCleanup = hasUserDefinedCleanup;
        }
    }

}
