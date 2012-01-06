/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.*;
import java.util.concurrent.locks.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class InitLaterDataController implements AsyncDataController {
    private final ReentrantReadWriteLock dataLock;
    private AsyncDataController finalController;

    private AsyncDataState firstState;
    private List<Object> controlDataList;
    private boolean cancelImmediately;

    public InitLaterDataController() {
        this(null);
    }

    public InitLaterDataController(AsyncDataState firstState) {
        this.dataLock = new ReentrantReadWriteLock();
        this.finalController = null;
        this.controlDataList = new LinkedList<>();
        this.firstState = firstState;
        this.cancelImmediately = false;
    }

    public void initController(AsyncDataController wrappedController) {
        ExceptionHelper.checkNotNullArgument(wrappedController, "wrappedController");

        boolean cancelNow;
        Object[] controlArgs;

        Lock lock = dataLock.writeLock();
        lock.lock();
        try {
            if (finalController != null) {
                throw new IllegalStateException("The wrapped controller was already initialized.");
            }

            finalController = wrappedController;

            cancelNow = cancelImmediately;
            controlArgs = controlDataList.toArray();
            if (controlArgs.length > 0) {
                controlDataList.clear();
            }
            else {
                controlDataList = null;
            }

            // Allow it to be garbage collected since it will not be used.
            firstState = null;
        } finally {
            lock.unlock();
        }

        if (cancelNow) {
            wrappedController.cancel();
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
                }
                else {
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

    @Override
    public void cancel() {
        AsyncDataController wrapped = getFinalController();
        if (wrapped != null) {
            wrapped.cancel();
            return;
        }

        Lock lock = dataLock.writeLock();
        lock.lock();
        try {
            wrapped = finalController;
            // there is no need to check for the wrapped controller
            // because we will not use this value after it was set.
            cancelImmediately = true;
        } finally {
            lock.unlock();
        }

        if (wrapped != null) {
            wrapped.cancel();
        }
    }

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

    @Override
    public String toString() {
        return "DataController{" + "state=" + getDataState() + '}';
    }
}
