package org.jtrim2.concurrent.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Defines an {@code AsyncDataController} which forwards every call to a wrapped
 * {@code AsyncDataController} but the wrapped {@code AsyncDataController} can
 * be specified later, after construction time.
 * <P>
 * After creating an {@code InitLaterDataController}, sooner or later the
 * {@link #initController(AsyncDataController) initController} method must be
 * called to set the wrapped {@code AsyncDataController} to which calls are
 * forwarded after {@code initController} returns. Up until the point before
 * {@code initController} was invoked, the {@code InitLaterDataController}
 * records the actions requested and will forward all these requests to the
 * wrapped {@code AsyncDataController} immediately after it is initialized. More
 * specifically: It will remember every
 * {@link AsyncDataController#controlData(Object) control object} sent to it and
 * will forward them to the wrapped {@code AsyncDataController} after
 * initialized, in the same order as the control objects were submitted. For the
 * {@link AsyncDataController#getDataState() state of progress}, the
 * {@code InitLaterDataController} will return the state specified at
 * construction time before it is initialized (and after that, it will just
 * return the state returned by wrapped {@code AsyncDataController}.
 * <P>
 * This class is advantageous to use when an {@code AsyncDataController} cannot
 * be created right away but is still required (possibly because it need to be
 * returned by a method). Note that even in this case, it is recommended to
 * quickly initialize the {@code InitLaterDataController} to avoid excessive
 * accumulation of control objects.
 * <P>
 * Note that if an {@code InitLaterDataController} instance is returned, it is
 * safer to wrap it in a {@link DelegatedAsyncDataController} instance
 * (for the return value), so the code accepting the return value may not be
 * able to call the {@code initController} method.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safe to be used by multiple threads concurrently.
 * Note however, that the
 * {@link #initController(AsyncDataController) initController} method may only
 * be called at most once.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class derive their synchronization transparency property
 * from the wrapped {@code AsyncDataController}.
 *
 * @see DelegatedAsyncDataController
 */
public final class InitLaterDataController implements AsyncDataController {
    private final ReentrantReadWriteLock dataLock;
    private AsyncDataController finalController;

    private AsyncDataState firstState;
    private List<Object> controlDataList;

    /**
     * Creates the {@code InitLaterDataController} with a {@code null}
     * {@link #getDataState() state of progress object} to return before
     * actually {@link #initController(AsyncDataController) initialized} with
     * an {@code AsyncDataController}.
     * <P>
     * Note that the {@link #initController(AsyncDataController) initController}
     * method needs to be called so method calls may be forwarded to the actual
     * {@code AsyncDataController}.
     */
    public InitLaterDataController() {
        this(null);
    }

    /**
     * Creates the {@code InitLaterDataController} with a
     * {@link #getDataState() state of progress object} to return before
     * actually {@link #initController(AsyncDataController) initialized} with
     * an {@code AsyncDataController}.
     * <P>
     * Note that the {@link #initController(AsyncDataController) initController}
     * method needs to be called so method calls may be forwarded to the actual
     * {@code AsyncDataController}.
     *
     * @param firstState the state of progress to be returned by the
     *   {@link #getDataState() getDataState} method before the newly created
     *   {@code InitLaterDataController} instance is initialized. This argument
     *   can be {@code null}, in which case {@code null} will be returned prior
     *   initialization.
     */
    public InitLaterDataController(AsyncDataState firstState) {
        this.dataLock = new ReentrantReadWriteLock();
        this.finalController = null;
        this.controlDataList = new ArrayList<>();
        this.firstState = firstState;
    }

    /**
     * Initializes this {@code InitLaterDataController} with the specified
     * {@code AsyncDataController} to which calls are to be forwarded to. This
     * method may invoke the
     * {@link AsyncDataController#controlData(Object) controlData} of the
     * specified {@code AsyncDataController}.
     * <P>
     * This method may only be called once.
     *
     * @param wrappedController the {@code AsyncDataController} to which calls
     *   are forwarded to. This argument cannot be {@code null}.
     *
     * @throws IllegalStateException thrown if this method has already been
     *   called
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataController} was {@code null}
     */
    public void initController(AsyncDataController wrappedController) {
        Objects.requireNonNull(wrappedController, "wrappedController");

        Object[] controlArgs;

        Lock lock = dataLock.writeLock();
        lock.lock();
        try {
            if (finalController != null) {
                throw new IllegalStateException("The wrapped controller was already initialized.");
            }

            finalController = wrappedController;

            controlArgs = controlDataList.toArray();
            if (controlArgs.length > 0) {
                controlDataList.clear();
            } else {
                controlDataList = null;
            }

            // Allow it to be garbage collected since it will not be used.
            firstState = null;
        } finally {
            lock.unlock();
        }

        while (controlArgs.length > 0) {
            for (Object controlArg: controlArgs) {
                wrappedController.controlData(controlArg);
            }

            lock.lock();
            try {
                controlArgs = controlDataList.toArray();
                if (controlArgs.length > 0) {
                    controlDataList.clear();
                } else {
                    controlDataList = null;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * It is more efficient to check first holding only the read lock
     * because we expect to have the wrapped controller initialized
     * not long after making this controller publicly available.
     *
     * @return the wrapped controller or <EM>null</EM> if there is non yet.
     */
    private AsyncDataController getFinalController() {
        Lock lock = dataLock.readLock();
        lock.lock();
        try {
            return finalController;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * This implementation will forward its call to the wrapped
     * {@code AsyncDataController} after being
     * {@link #initController(AsyncDataController) initialized}, before that
     * it will collect the specified control object and will forward the control
     * object after initialization.
     */
    @Override
    public void controlData(Object controlArg) {
        AsyncDataController wrapped = null;

        Lock rLock = dataLock.readLock();
        rLock.lock();
        try {
            // As long as it is not null, we have to gather data in this list.
            if (controlDataList == null) {
                wrapped = finalController;
            }
        } finally {
            rLock.unlock();
        }

        if (wrapped != null) {
            wrapped.controlData(controlArg);
            return;
        }

        Lock lock = dataLock.writeLock();
        lock.lock();
        try {
            if (controlDataList == null) {
                wrapped = finalController;
            }

            if (wrapped == null) {
                controlDataList.add(controlArg);
            }
        } finally {
            lock.unlock();
        }

        if (wrapped != null) {
            wrapped.controlData(controlArg);
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * This implementation will forward its call to the wrapped
     * {@code AsyncDataController} after being
     * {@link #initController(AsyncDataController) initialized}, before that
     * it will return the state object specified at construction time.
     */
    @Override
    public AsyncDataState getDataState() {
        AsyncDataController wrapped = getFinalController();
        if (wrapped != null) {
            return wrapped.getDataState();
        }

        Lock lock = dataLock.writeLock();
        lock.lock();
        try {
            wrapped = finalController;
            if (wrapped == null) {
                return firstState;
            }
        } finally {
            lock.unlock();
        }

        // Notice that wrapped is not null because
        // the method returns in this case.
        return wrapped.getDataState();
    }

    /**
     * Returns the string representation of this {@code InitLaterDataController}
     * in no particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "DataController{" + "state=" + getDataState() + '}';
    }
}
