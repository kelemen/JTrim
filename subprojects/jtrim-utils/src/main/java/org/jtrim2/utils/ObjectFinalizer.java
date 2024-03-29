package org.jtrim2.utils;

import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a simple safety-net for objects managing unmanaged resources (e.g.:
 * files).
 * <P>
 * {@code ObjectFinalizer} takes a {@link Runnable} in its constructor and
 * allows it to be called in the {@link #doFinalize() doFinalize()} method.
 * Also, {@code ObjectFinalizer} submits the provided cleanup task to a {@link Cleaner},
 * in which it will call {@code run()} method of the specified {@code Runnable}
 * if it was not called manually (by calling {@code doFinalize()}).
 * <P>
 * Once the {@code doFinalize()} method of an {@code ObjectFinalizer} was called
 * it will no longer retain a reference to the {@code Runnable} specified at
 * construction time.
 * <P>
 * See the following example code using {@code ObjectFinalizer}:
 * <pre>{@code
 * class UnmanagedResourceHolder implements Closable {
 *   private final ObjectFinalizer finalizer;
 *
 *   // Other declarations ...
 *
 *   public UnmanagedResourceHolder() {
 *     // Initialization code ...
 *
 *     this.finalizer = new ObjectFinalizer(this::doCleanup, "UnmanagedResourceHolder.cleanup");
 *   }
 *
 *   // Other code ...
 *
 *   private void doCleanup() {
 *     // cleanup unmanaged resources
 *   }
 *
 *   {@literal @Override}
 *   public void close() {
 *     finalizer.doFinalize();
 *   }
 * }
 * }</pre>
 * <P>
 * Assume, that in the above code an {@code UnmanagedResourceHolder} instance
 * becomes unreachable and as such is eligible for garbage collection. Notice
 * that in this case {@code finalizer} also becomes unreachable and such also
 * eligible for garbage collection. Also assume that the {@code close()} method
 * was not called. In this case when the JVM decides to clean up the now
 * unreachable {@code finalizer} instance, it will call its finalizer and in
 * turn invoke the {@code doCleanup()} method releasing the unmanaged resources.
 *
 * <h2>Unmanaged Resources</h2>
 * In Java a garbage collector is employed, so the programmer is relieved from
 * the burden (mostly) of manual memory management. However, the garbage
 * collector cannot handle anything beyond the memory allocated for objects, so
 * other unmanaged resources require a cleanup method. Although there are
 * {@link Cleaner cleaners} in Java, they are unreliable and the
 * only correct solution is the use of a cleanup method. Generally, objects
 * managing unmanaged resources should implement the {@link AutoCloseable}
 * interface.
 * <P>
 * Notice however, that a bug may prevent the program to call the cleanup
 * method of an object, causing the leakage of unmanaged resource, possibly
 * leading to resource exhaustion. In this case cleaners can be useful to
 * provide a safety-net, that even in the case of such previously mentioned bug,
 * a cleaner can be implemented to do the cleanup, so when the JVM actually
 * removes the object, it may clean up the unmanaged resources.
 *
 * <h2>Benefits of {@code ObjectFinalizer}</h2>
 * One may ask what are the benefits of using {@code ObjectFinalizer} instead of
 * directly using a {@link Cleaner}. There are actually two main benefits of
 * using {@code ObjectFinalizer}:
 * <P>
 * Notice that the {@code run()} method of the specified {@code Runnable}
 * instance can only be called at most once even if the {@code doFinalize()}
 * method is called concurrently multiple times. This effectively makes the
 * cleanup method idempotent for free which makes the cleanup method safer.
 * <P>
 * In case the cleanup method was failed to be called, the
 * {@code ObjectFinalizer} will log this failure when it detects during cleanup
 * method was not called. So this error will be documented in the logs and can
 * be analyzed later.
 *
 * <h2>Thread safety</h2>
 * This class is safe to be used by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Unless otherwise noted, methods of this class are not
 * <I>synchronization transparent</I>.
 */
public final class ObjectFinalizer {
    private static final String MISSED_FINALIZE_MESSAGE
            = "An object was not finalized explicitly."
            + " Finalizer task: {}/{}.";

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFinalizer.class);

    private final AtomicReference<Runnable> finalizerTask;
    private final String className;
    private final String taskDescription;
    private final Cleaner.Cleanable registrationRef;

    /**
     * Creates a new {@code ObjectFinalizer} using the specified
     * {@code Runnable} to be called by the {@link #doFinalize() doFinalize()}
     * method.
     * <P>
     * The task description to be used in the log when {@code doFinalize()}
     * is failed to get called is the result of the {@code toString()}
     * method of the specified task. The result of the {@code toString()} is
     * retrieved in this constructor call and not when actually required.
     *
     * @param finalizerTask the task to be invoked by the {@code doFinalize()}
     *   method to clean up unmanaged resources. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is
     *   {@code null} or its {@code toString()} method returns {@code null}
     */
    public ObjectFinalizer(Runnable finalizerTask) {
        this(finalizerTask, finalizerTask.toString());
    }

    /**
     * Creates a new {@code ObjectFinalizer} using the specified
     * {@code Runnable} to be called by the {@link #doFinalize() doFinalize()}
     * method and a task description to be used in logs if {@code doFinalize()}
     * is only called in the finalizer.
     *
     * @param finalizerTask the task to be invoked by the {@code doFinalize()}
     *   method to cleanup unmanaged resources. This argument cannot be
     *   {@code null}.
     * @param taskDescription the description to be added to the log message
     *   if the {@code doFinalize()} only gets called in the finalizer
     *
     * @throws NullPointerException thrown if the specified task or the task
     *   description is {@code null}
     */
    public ObjectFinalizer(Runnable finalizerTask, String taskDescription) {
        Objects.requireNonNull(finalizerTask, "finalizerTask");
        Objects.requireNonNull(taskDescription, "taskDescription");

        var finalizerTaskRef = new AtomicReference<>(finalizerTask);
        String finalizerTaskClassName = finalizerTask.getClass().getName();
        this.registrationRef = CleanerHolder.CLEANER.register(
                this,
                () -> cleanupNow(finalizerTaskRef, finalizerTaskClassName, taskDescription)
        );

        this.finalizerTask = finalizerTaskRef;
        this.taskDescription = taskDescription;
        this.className = finalizerTask.getClass().getName();
    }

    /**
     * Sets the state as if {@link #doFinalize() doFinalize()} has been called
     * but does not actually call {@code doFinalize}. This method is useful if
     * the object has been finalized in another way, so it is no longer an error
     * not to finalize the object.
     * <P>
     * After calling this method, subsequent {@code doFinalize()} method calls
     * will do nothing.
     */
    public void markFinalized() {
        finalizerTask.set(null);
        registrationRef.clean();
    }

    /**
     * Invokes the task specified at construction time if it was not called yet.
     * The task will be called only once, even if this method is called
     * concurrently by multiple threads.
     * <P>
     * The task is invoked synchronously on the current calling thread. Note
     * that therefore, this method will propagate every exception to the caller
     * thrown by the task.
     * <P>
     * Once this method returns (even if the called task throws an exception),
     * the task specified at construction time will no longer be referenced by
     * this {@code ObjectFinalizer}.
     *
     * @return {@code true} if this method actually invoked the task specified
     *   at construction time, {@code false} if {@code doFinalize()} was already
     *   called (or another {@code doFinalize()} is executing the task
     *   concurrently)
     */
    public boolean doFinalize() {
        Runnable task = finalizerTask.getAndSet(null);
        if (task != null) {
            task.run();
            return true;
        }

        return false;
    }

    /**
     * Returns {@code true} if {@link #doFinalize() doFinalize()} has already
     * been called. In case this method returns {@code true}, subsequent calls
     * to {@code doFinalize()} will do nothing but return immediately to the
     * caller.
     *
     * @return {@code true} if {@code doFinalize()} has already
     *   been called, {@code false} otherwise
     */
    public boolean isFinalized() {
        return finalizerTask.get() == null;
    }

    /**
     * Throws an {@link IllegalStateException} if the
     * {@link #doFinalize() doFinalize()} method has already been called. This
     * method can be used to implement a fail-fast behaviour when the object
     * this {@code ObjectFinalizer} protects is being used after cleanup.
     *
     * @throws IllegalStateException thrown if {@code doFinalize()} has already
     *   been called. That is, if {@link #isFinalized() isFinalized()} returns
     *   {@code true}.
     */
    public void checkNotFinalized() {
        if (isFinalized()) {
            throw new IllegalStateException("Object was already finalized: "
                    + className + "/" + taskDescription);
        }
    }

    /**
     * This method does the cleanup as if it was forgotten.
     */
    void doForgottenCleanup() {
        registrationRef.clean();
    }

    private static void cleanupNow(AtomicReference<Runnable> finalizerTask, String className, String taskDescription) {
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

        if (task != null && LOGGER.isErrorEnabled()) {
            if (exception == null) {
                LOGGER.error(MISSED_FINALIZE_MESSAGE, className, taskDescription);
            } else {
                LOGGER.error(MISSED_FINALIZE_MESSAGE, className, taskDescription, exception);
            }
        }
    }

    private static final class CleanerHolder {
        public static final Cleaner CLEANER = Cleaner.create();
    }
}
