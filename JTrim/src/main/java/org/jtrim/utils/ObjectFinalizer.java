/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author Kelemen Attila
 */
public final class ObjectFinalizer {
    private static final String MISSED_FINALIZE_MESSAGE
            = "An object was not finalized explicitly."
            + " Finalizer task: {0}/{1}.";

    private static final Logger finalizerLogger
            = Logger.getLogger(ObjectFinalizer.class.getName());

    private final AtomicReference<Runnable> finalizerTask;
    private final String className;
    private final String taskDescription;

    public ObjectFinalizer(Runnable finalizerTask) {
        this(finalizerTask, finalizerTask.toString());
    }

    public ObjectFinalizer(Runnable finalizerTask, String taskDescription) {
        ExceptionHelper.checkNotNullArgument(finalizerTask, "finalizerTask");
        ExceptionHelper.checkNotNullArgument(taskDescription, "taskDescription");

        this.finalizerTask = new AtomicReference<>(finalizerTask);
        this.taskDescription = taskDescription;
        this.className = finalizerTask.getClass().getName();
    }

    public boolean doFinalize() {
        Runnable task = finalizerTask.getAndSet(null);
        if (task != null) {
            task.run();
            return true;
        }

        return false;
    }

    public boolean isFinalized() {
        return finalizerTask.get() == null;
    }

    public void checkNotFinalized() {
        if (isFinalized()) {
            throw new IllegalStateException("Object was already finalized: "
                    + className + "/" + taskDescription);
        }
    }

    @Override
    @SuppressWarnings("FinalizeDoesntCallSuperFinalize")
    protected void finalize() {
        Throwable exception = null;
        Runnable task = null;

        try {
            task = finalizerTask.getAndSet(null);
            if (task != null) {
                task.run();
            }
        } catch (Throwable ex) {
            exception = ex;

        }

        if (task != null && finalizerLogger.isLoggable(Level.SEVERE)) {
            LogRecord logRecord
                    = new LogRecord(Level.SEVERE, MISSED_FINALIZE_MESSAGE);

            logRecord.setSourceClassName(ObjectFinalizer.class.getName());
            logRecord.setSourceMethodName("finalize()");
            logRecord.setThrown(exception);
            logRecord.setParameters(new Object[]{ className, taskDescription });

            finalizerLogger.log(logRecord);
        }
    }
}
