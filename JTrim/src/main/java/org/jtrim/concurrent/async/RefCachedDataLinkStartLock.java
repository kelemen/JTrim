/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.jtrim.cache.*;
import org.jtrim.collections.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class RefCachedDataLinkStartLock<DataType>
implements
        AsyncDataLink<RefCachedData<DataType>> {

    private static final ScheduledExecutorService CANCEL_TIMER
            = ExecutorsEx.newSchedulerThreadedExecutor(1, true,
            "RefCachedDataLink(old) cancel timer");

    private final RefList<ListenerInfo> listeners;

    private Object currentSessionID;
    private final AsyncDataLink<? extends DataType> wrappedDataLink;
    private AsyncDataController currentController;
    private final InternalDataListener internalListener;

    private final ReentrantLock mainLock;
    private final ReentrantLock startLock;

    private boolean started; // protected by startLock as well
    private boolean receiving; // protected by startLock as well
    private boolean hasData;

    private AsyncReport lastReport;
    private VolatileReference<DataType> lastReceivedDataRef;
    private long lastReceivedOrder;

    private final long dataCancelTimeout; // ms
    private RunnableFuture<?> currentCancelTask;

    private final ReferenceType refType;
    private final ObjectCache refCreator;

    public RefCachedDataLinkStartLock(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeoutUnit) {

        if (dataCancelTimeout < 0) {
            throw new IllegalArgumentException(
                    "The timeout value cannot be negative.");
        }

        ExceptionHelper.checkNotNullArgument(wrappedDataLink, "wrappedDataLink");
        ExceptionHelper.checkNotNullArgument(refType, "refType");

        this.currentSessionID = null;
        this.listeners = new RefLinkedList<>();
        this.wrappedDataLink = wrappedDataLink;
        this.currentController = null;
        this.internalListener = new InternalDataListener();
        this.mainLock = new ReentrantLock();
        this.startLock = new ReentrantLock();
        this.started = false;
        this.receiving = false;
        this.hasData = false;
        this.lastReport = null;
        this.lastReceivedDataRef = null;
        this.refType = refType;
        this.currentCancelTask = null;
        this.dataCancelTimeout = timeoutUnit.toMillis(dataCancelTimeout);

        this.refCreator = refCreator != null
                ? refCreator
                : JavaRefObjectCache.INSTANCE;
    }

    private static Object newSession() {
        return new Object();
    }

    private AsyncDataController getAsyncBlockingDataSafe(ListenerInfo newListener) {
        AsyncDataController controller;

        AsyncReport currentReport = null;

        DataType lastData;
        VolatileReference<DataType> refLastData;

        long lastDataIndex;

        startLock.lock();
        try {
            // wrappedDataLink.getAsyncData is not allowed to call
            // this method (on the wrapper link) since we cannot handle such
            // event. This check will protect us from such recursions.
            if (startLock.getHoldCount() > 1) {
                throw new IllegalStateException("Recursive calls on"
                        + " RefCachedAsyncDataLink.getAsyncBlockingDataSafe"
                        + " are not allowed.");
            }

            boolean needStart;
            Object sessionID;

            mainLock.lock();
            try {
                refLastData = lastReceivedDataRef;
                lastData = refLastData != null ? refLastData.get() : null;
                lastDataIndex = lastReceivedOrder;

                needStart = !receiving;

                if (needStart && started && (lastData != null || !hasData)) {
                    needStart = false;
                    currentReport = lastReport;
                    assert currentReport != null;
                }
                else {
                    newListener.setSelfRef(listeners.addLastGetReference(newListener));
                }

                if (needStart) {
                    assert !receiving;
                    started = true;
                    hasData = false;
                    // We will not use it, so we will clear the reference
                    // so reappearing data may not confuse us in the future.
                    lastReceivedDataRef = null;
                    currentSessionID = newSession();
                }

                sessionID = currentSessionID;
            } finally {
                mainLock.unlock();
            }

            if (needStart) {
                controller = wrappedDataLink.getData(
                        new MarkedListener(sessionID, internalListener));

                mainLock.lock();
                try {
                    receiving = true;
                    currentController = controller;
                } finally {
                    mainLock.unlock();
                }
            }
            else {
                mainLock.lock();
                try {
                    controller = currentController;
                } finally {
                    mainLock.unlock();
                }
            }
        } finally {
            startLock.unlock();
        }

        OrderedData<RefCachedData<DataType>> dataToSend;

        if (lastData != null) {
            dataToSend = new OrderedData<>(
                    lastDataIndex,
                    new RefCachedData<>(lastData, refLastData));
        }
        else {
            dataToSend = null;
        }

        AsyncDataListener<OrderedData<RefCachedData<DataType>>> currentListener;
        currentListener = newListener.getSafeListener();

        try {
            if (dataToSend != null) {
                currentListener.onDataArrive(dataToSend);
            }
        } finally {
            if (currentReport != null) {
                currentListener.onDoneReceive(currentReport);
            }
        }

        newListener.setController(controller);
        return new SubLinkController(newListener);
    }

    @Override
    public AsyncDataController getData(AsyncDataListener<? super RefCachedData<DataType>> dataListener) {
        AsyncDataListener<OrderedData<RefCachedData<DataType>>> safeListener;
        safeListener = AsyncDatas.makeSafeOrderedListener(dataListener);

        return getAsyncBlockingDataSafe(new ListenerInfo(safeListener));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("From ");
        AsyncFormatHelper.appendIndented(wrappedDataLink, result);
        result.append("\nCache result (Data + DataRef) using ");
        result.append(refType);
        result.append(" (");
        result.append(refCreator);
        result.append(")");

        return result.toString();
    }

    private boolean tryCancelNow() {
        assert !mainLock.isHeldByCurrentThread();

        boolean result = false;
        AsyncDataController controller = null;
        RunnableFuture<?> cancelTask = null;

        startLock.lock();
        try {
            mainLock.lock();
            try {
                if (receiving && listeners.isEmpty()) {
                    controller = currentController;
                    cancelTask = currentCancelTask;

                    started = false;
                    lastReport = null;
                    receiving = false;
                    hasData = false;
                    lastReceivedDataRef = null;
                    currentSessionID = newSession();
                    // Change the session ID so new datas will not be
                    // cached.

                    currentCancelTask = null;
                    result = true;
                }
            } finally {
                mainLock.unlock();
            }
        } finally {
            startLock.unlock();
        }

        if (controller != null) {
            controller.cancel();
        }

        if (cancelTask != null) {
            cancelTask.cancel(false);
        }

        return result;
    }

    private void startCancelTimer() {
        if (dataCancelTimeout == 0) {
            tryCancelNow();
            return;
        }

        RunnableFuture<?> cancelTask = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
                tryCancelNow();
            }
        }, null);

        mainLock.lock();
        try {
            if (currentCancelTask == null) {
                currentCancelTask = cancelTask;
            }
            else {
                cancelTask = null;
            }
        } finally {
            mainLock.unlock();
        }

        if (cancelTask != null) {
            CANCEL_TIMER.schedule(cancelTask,
                    dataCancelTimeout, TimeUnit.MILLISECONDS);
        }
    }

    private class ListenerInfo {
        private final AsyncDataListener<OrderedData<RefCachedData<DataType>>> safeListener;
        private RefList.ElementRef<ListenerInfo> selfRef;
        private boolean hasController;
        private AsyncDataController controller;
        private final ReentrantLock controllerLock;
        private final List<Object> controlArgs;
        private boolean receivedData;

        // Note that ListenerInfo must not be published only
        // after setting the data controller.
        public ListenerInfo(AsyncDataListener<OrderedData<RefCachedData<DataType>>> safeListener) {
            this.safeListener = safeListener;
            this.selfRef = null;
            this.hasController = false;
            this.controller = null;
            this.controllerLock = new ReentrantLock();
            this.controlArgs = new LinkedList<>();
            this.receivedData = false;
        }

        public void resetInfo() {
            mainLock.lock();
            try {
                this.selfRef = null;
            } finally {
                mainLock.unlock();
            }

            controllerLock.lock();
            try {
                assert !receivedData;
                this.controller = null;
                this.hasController = false;
            } finally {
                controllerLock.unlock();
            }

        }

        private boolean maySwitchController() {
            assert controllerLock.isHeldByCurrentThread();
            return !hasController || !receivedData;
        }

        public AsyncDataState getDataState() {
            AsyncDataController currentController;
            controllerLock.lock();
            try {
                currentController = controller;
            } finally {
                controllerLock.unlock();
            }

            return currentController != null
                    ? currentController.getDataState()
                    : null;
        }

        public void controlData(Object controlArg) {
            assert !mainLock.isHeldByCurrentThread();

            AsyncDataController currentController;
            controllerLock.lock();
            try {
                currentController = controller;
                if (maySwitchController() && currentController != null) {
                    controlArgs.add(controlArg);
                }
            } finally {
                controllerLock.unlock();
            }

            if (currentController != null) {
                currentController.controlData(controlArg);
            }
        }

        public void setController(AsyncDataController controller) {
            if (controller == null) {
                throw new IllegalArgumentException("controller cannot be null.");
            }

            assert !mainLock.isHeldByCurrentThread();

            List<Object> controlArgsCopy = new LinkedList<>();

            //FIXME: The order of controlArg sending may change
            //       because new args may be sent while calling
            //       controller.controlData
            controllerLock.lock();
            try {
                assert maySwitchController();
                this.controller = controller;
                controlArgsCopy.addAll(controlArgs);
                controlArgs.clear();
            } finally {
                controllerLock.unlock();
            }

            for (Object arg: controlArgsCopy) {
                controller.controlData(arg);
            }
        }

        public RefList.ElementRef<ListenerInfo> getSelfRef() {
            assert mainLock.isHeldByCurrentThread();
            return selfRef;
        }

        public void setSelfRef(RefList.ElementRef<ListenerInfo> selfRef) {
            assert mainLock.isHeldByCurrentThread();
            assert selfRef != null;
            assert selfRef.getElement() == this;

            this.selfRef = selfRef;
        }

        public AsyncDataListener<OrderedData<RefCachedData<DataType>>> getSafeListener() {
            return safeListener;
        }

        public void setReceivedData() {
            controllerLock.lock();
            try {
                // Notice that once we have received data, we will
                // never need to pass the controller arguments to
                // another controller because we will receive every subsequent
                // asynchronous data.
                receivedData = true;
                controlArgs.clear();
            } finally {
                controllerLock.unlock();
            }
        }

        public boolean isReceivedData() {
            controllerLock.lock();
            try {
                return receivedData;
            } finally {
                controllerLock.unlock();
            }
        }
    }

    private class SubLinkController implements AsyncDataController {

        private final ListenerInfo listenerInfo;

        public SubLinkController(ListenerInfo listenerInfo) {
            this.listenerInfo = listenerInfo;
        }

        @Override
        public void controlData(Object controlArg) {
            listenerInfo.controlData(controlArg);
        }

        @Override
        public AsyncDataState getDataState() {
            return listenerInfo.getDataState();
        }

        @Override
        public void cancel() {
            boolean startCancel;

            listenerInfo.getSafeListener().onDoneReceive(AsyncReport.CANCELED);

            mainLock.lock();
            try {
                RefList.ElementRef<ListenerInfo> ref;
                ref = listenerInfo.getSelfRef();

                // If ref is null, it was already removed from the list
                // so the timer was already started if there was no listener
                // attached.
                if (ref != null) {
                    assert ref.getElement() == listenerInfo;
                    ref.remove();

                    startCancel = listeners.isEmpty();
                }
                else {
                    startCancel = false;
                }

            } finally {
                mainLock.unlock();
            }

            if (startCancel) {
                startCancelTimer();
            }
        }

    }

    private class MarkedListener implements AsyncDataListener<DataType> {
        private final Object sessionID;
        private final InternalDataListener wrappedListener;

        public MarkedListener(Object sessionID, InternalDataListener wrappedListener) {
            this.sessionID = sessionID;
            this.wrappedListener = wrappedListener;
        }

        @Override
        public boolean requireData() {
            return wrappedListener.requireData();
        }

        @Override
        public void onDataArrive(DataType newData) {
            wrappedListener.onDataArrive(newData, sessionID);
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            ExceptionHelper.checkNotNullArgument(report, "report");

            wrappedListener.onDoneReceive(sessionID, report);
        }
    }

    private class InternalDataListener {
        private long dataIndex;

        public InternalDataListener() {
            // Long.MIN_VALUE would be less likely to overflow but using 0 is
            // better for debugging purposes.
            this.dataIndex = 0;
        }

        public boolean requireData() {
            List<ListenerInfo> listenersCopy = new LinkedList<>();

            mainLock.lock();
            try {
                listenersCopy.addAll(listeners);
            } finally {
                mainLock.unlock();
            }

            for (ListenerInfo listener: listenersCopy) {
                if (listener.getSafeListener().requireData()) {
                    return true;
                }
            }

            return false;
        }

        public void onDataArrive(DataType newData, Object sessionID) {
            List<ListenerInfo> listenersCopy = new LinkedList<>();
            long index;
            RefCachedData<DataType> dataToSend;
            dataToSend = new RefCachedData<>(newData, refCreator, refType);
            VolatileReference<?> refToClear = null;

            mainLock.lock();
            try {
                index = dataIndex;

                if (sessionID == currentSessionID) {
                    dataIndex++;

                    refToClear = lastReceivedDataRef;
                    lastReceivedDataRef = dataToSend.getDataRef();
                    lastReceivedOrder = index;
                    hasData = true;
                    listenersCopy.addAll(listeners);
                }
            } finally {
                mainLock.unlock();
            }

            if (refToClear != null) {
                refToClear.clear();
            }

            OrderedData<RefCachedData<DataType>> data;
            data = new OrderedData<>(index, dataToSend);

            for (ListenerInfo listener: listenersCopy) {
                listener.setReceivedData();
                // Note that if an exception is thrown, other listeners
                // will not be notified.
                listener.getSafeListener().onDataArrive(data);
            }
        }

        public void onDoneReceive(Object sessionID, AsyncReport report) {
            List<ListenerInfo> listenersCopy = new LinkedList<>();
            boolean hasReceivedData;

            // We have to remove the cached reference if the sending was
            // canceled.
            VolatileReference<DataType> lastData = report.isCanceled()
                    ? GenericReference.<DataType>getNoReference()
                    : null;

            startLock.lock();
            try {
                mainLock.lock();
                try {
                    hasReceivedData = hasData;

                    if (sessionID == currentSessionID) {
                        lastReport = report;
                        receiving = false;
                        listenersCopy.addAll(listeners);
                        listeners.clear();

                        // Make it appear as if the data was removed from
                        // the cache. (if there was a data)
                        if (lastData != null && lastReceivedDataRef != null) {
                            lastReceivedDataRef = lastData;
                        }

                        // Since there are no listeners attached, it is
                        // safe to restart the data index.
                        // Long.MIN_VALUE would be less likely to overflow but using 0 is
                        // better for debugging purposes.
                        dataIndex = 0;
                    }
                } finally {
                    mainLock.unlock();
                }
            } finally {
                startLock.unlock();
            }

            Iterator<ListenerInfo> listenersItr = listenersCopy.iterator();
            while (listenersItr.hasNext()) {
                ListenerInfo listener = listenersItr.next();

                if (!hasReceivedData || listener.isReceivedData()) {
                    // Note that only one of the condition can hold.
                    // because if there was no data sent the listener
                    // could not receive one either.
                    assert hasReceivedData || !listener.isReceivedData();

                    // Note that if an exception is thrown, other listeners
                    // will not be notified and the data link will not be
                    // restarted.
                    listener.getSafeListener().onDoneReceive(report);
                    listenersItr.remove();
                }
                else {
                    listener.resetInfo();
                }
            }

            if (listenersCopy.size() > 0) {
                for (ListenerInfo listener: listenersCopy) {
                    // Notice that the old controller is just as good
                    // as this one.
                    getAsyncBlockingDataSafe(listener);
                }
            }
        }

    }
}
