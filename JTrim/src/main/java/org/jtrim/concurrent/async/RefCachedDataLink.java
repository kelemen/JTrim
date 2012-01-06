/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;
import org.jtrim.cache.*;
import org.jtrim.collections.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class RefCachedDataLink<DataType>
implements
        AsyncDataLink<RefCachedData<DataType>> {
    // Note that if noone but the internal objects reference this
    // data link noone can register with it and if there is no
    // listener it would be safe to cancel immediately the data receiving
    // however this would make this code more complex so this feature is
    // not implemented yet.

    private static final ScheduledExecutorService CANCEL_TIMER
            = ExecutorsEx.newSchedulerThreadedExecutor(1, true,
            "RefCachedDataLink cancel timer");

    private final ReferenceType refType;
    private final ObjectCache refCreator;
    private final AsyncDataLink<? extends DataType> wrappedDataLink;
    private final long dataCancelTimeoutNanos;

    private final ReentrantLock listenersLock;
    private final RefList<RegisteredListener> listeners;

    private final InOrderScheduledSyncExecutor eventScheduler;
    private ExecutionState executionState;
    private Object currentSessionID; // executionState != NotStarted
    private AsyncDataController currentController; // executionState != NotStarted
    private VolatileReference<DataType> lastData; // executionState != NotStarted
    private AsyncReport lastReport; // executionState == Finished

    private final AtomicReference<RunnableFuture<?>> currentCancelTask;

    public RefCachedDataLink(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeoutUnit) {

        if (dataCancelTimeout < 0) {
            throw new IllegalArgumentException(
                    "The timeout value cannot be negative.");
        }

        ExceptionHelper.checkNotNullArgument(wrappedDataLink, "wrappedDataLink");
        ExceptionHelper.checkNotNullArgument(refType, "refType");
        ExceptionHelper.checkNotNullArgument(timeoutUnit, "timeoutUnit");

        this.refType = refType;
        this.refCreator = refCreator != null
                ? refCreator
                : JavaRefObjectCache.INSTANCE;

        this.dataCancelTimeoutNanos = timeoutUnit.toNanos(dataCancelTimeout);

        this.listenersLock = new ReentrantLock();
        this.listeners = new RefLinkedList<>();

        this.eventScheduler = new InOrderScheduledSyncExecutor();
        this.executionState = ExecutionState.NotStarted;
        this.wrappedDataLink = wrappedDataLink;
        this.currentSessionID = null;
        this.currentController = null;
        this.lastData = null;
        this.lastReport = null;

        this.currentCancelTask = new AtomicReference<>(null);
    }

    @Override
    public AsyncDataController getData(
            AsyncDataListener<? super RefCachedData<DataType>> dataListener) {

        final RegisteredListener listener;
        listener = new RegisteredListener(null, dataListener);
        listener.register();

        eventScheduler.execute(new RegisterNewListenerTask(listener));

        return listener;
    }

    private void tryCancelNow(RunnableFuture<?> callingTask) {
        eventScheduler.execute(new CancelNowTask(callingTask));
    }

    private boolean hasRegisteredListeners() {
        listenersLock.lock();
        try {
            return !listeners.isEmpty();
        } finally {
            listenersLock.unlock();
        }
    }

    private void testForCancel() {
        if (hasRegisteredListeners()) {
            return;
        }

        if (dataCancelTimeoutNanos == 0) {
            tryCancelNow(null);
        }
        else {
            AsyncCancelTask cancelTask = new AsyncCancelTask();
            RunnableFuture<?> thisTask = new FutureTask<>(cancelTask, null);
            cancelTask.init(thisTask);

            boolean wasSet = false;

            listenersLock.lock();
            try {
                if (listeners.isEmpty()) {
                    wasSet = currentCancelTask.compareAndSet(null, thisTask);
                }
            } finally {
                listenersLock.unlock();
            }

            if (wasSet) {
                CANCEL_TIMER.schedule(thisTask,
                        dataCancelTimeoutNanos, TimeUnit.NANOSECONDS);
            }
        }
    }

    private static Object newSessionID() {
        return new Object();
    }

    private void startNewSession() {
        assert eventScheduler.isCurrentThreadExecuting();
        executionState = ExecutionState.Started;
        currentSessionID = newSessionID();
        currentController = null;
        lastReport = null;
        lastData = null;
    }

    private void trySetFinishSession(Object sessionID,
            AsyncReport finishReport) {

        assert eventScheduler.isCurrentThreadExecuting();
        if (currentSessionID == sessionID
                && executionState != ExecutionState.Finished) {

            assert executionState != ExecutionState.NotStarted;
            assert lastReport == null;

            executionState = ExecutionState.Finished;
            lastReport = finishReport;

            if (finishReport.isCanceled()) {
                lastData = null;
            }
        }
    }

    private RefCachedData<DataType> getCached() {
        assert eventScheduler.isCurrentThreadExecuting();

        VolatileReference<DataType> dataRef = lastData;
        DataType data = dataRef != null ? dataRef.get() : null;

        return data != null ? new RefCachedData<>(data, dataRef) : null;
    }

    private class RegisteredListener
    implements
            AsyncDataController {

        private Object sessionID;
        private volatile boolean listenerReceivedData;
        private final AsyncDataListener<RefCachedData<DataType>> listener;
        private RefList.ElementRef<RegisteredListener> listRef;

        private final Lock controllerLock;
        private volatile List<Object> controlArgs;
        private volatile AsyncDataController controller;

        private RegisteredListener(Object sessionID,
                AsyncDataListener<? super RefCachedData<DataType>> listener) {
            assert listener != null;

            this.sessionID = sessionID;
            this.listener = AsyncDatas.makeSafeListener(listener);
            this.listRef = null;
            this.listenerReceivedData = false;
            this.controllerLock = new ReentrantLock();
            this.controlArgs = new LinkedList<>();
            this.controller = null;
        }

        // Must be called before doing anything with this object
        private void register() {
            assert !listenersLock.isHeldByCurrentThread();

            RunnableFuture<?> cancelTask;
            listenersLock.lock();
            try {
                assert listRef == null;
                listRef = listeners.addLastGetReference(this);
                cancelTask = currentCancelTask.getAndSet(null);
            } finally {
                listenersLock.unlock();
            }

            if (cancelTask != null) {
                cancelTask.cancel(false);
            }
        }

        private void initSession(Object sessionID) {
            assert sessionID != null;
            this.sessionID = sessionID;
        }

        private void initController(AsyncDataController controller) {
            assert eventScheduler.isCurrentThreadExecuting();
            assert listRef != null;
            assert controlArgs != null;
            assert controller != null;

            List<Object> argsToSend = new LinkedList<>();

            controllerLock.lock();
            try {
                List<Object> currentArgs = controlArgs;
                if (currentArgs != null) {
                    argsToSend.addAll(currentArgs);
                }

                if (listenerReceivedData) {
                    controlArgs = null;
                }

                this.controller = controller;
            } finally {
                controllerLock.unlock();
            }

            for (Object arg: argsToSend) {
                controller.controlData(arg);
            }
        }

        public boolean isListenerReceivedData() {
            return listenerReceivedData;
        }

        private boolean requireData() {
            return listener.requireData();
        }

        private void onDataArrive(Object sessionID,
                RefCachedData<DataType> data) {

            assert eventScheduler.isCurrentThreadExecuting();
            assert listRef != null;
            if (sessionID == this.sessionID) {
                if (controller != null) {
                    controlArgs = null;
                }

                listenerReceivedData = true;
                listener.onDataArrive(data);
            }
        }

        private void trySendCachedData(
                Object dataSessionID,
                RefCachedData<DataType> data) {

            assert eventScheduler.isCurrentThreadExecuting();
            assert listRef != null;
            if (dataSessionID == sessionID && !listenerReceivedData) {
                listenerReceivedData = true;
                listener.onDataArrive(data);
            }
        }

        private void onDoneReceive(Object sessionID, AsyncReport report) {
            assert eventScheduler.isCurrentThreadExecuting();

            assert listRef != null;
            if (sessionID == this.sessionID) {
                listenersLock.lock();
                try {
                    listRef.remove();
                } finally {
                    listenersLock.unlock();
                }

                listener.onDoneReceive(report);
            }
        }

        @Override
        public void controlData(Object controlArg) {
            assert listRef != null;
            AsyncDataController currentController;

            controllerLock.lock();
            try {
                currentController = controller;
                List<Object> currentArgs = controlArgs;

                if (currentArgs != null) {
                    currentArgs.add(controlArg);
                }
            } finally {
                controllerLock.unlock();
            }


            if (currentController != null) {
                currentController.controlData(controlArg);
            }
        }

        @Override
        public void cancel() {
            assert listRef != null;
            // TODO: use IdempotentTask
            listener.onDoneReceive(AsyncReport.CANCELED);

            boolean mayCancel;
            listenersLock.lock();
            try {
                listRef.remove();
                mayCancel = listeners.isEmpty();
            } finally {
                listenersLock.unlock();
            }

            if (mayCancel) {
                testForCancel();
            }
        }

        @Override
        public AsyncDataState getDataState() {
            AsyncDataController currentController = controller;
            return currentController != null
                    ? currentController.getDataState()
                    : null;
        }
    }

    private static <DataType> void forwardDataToAll(
            List<RefCachedDataLink<DataType>.RegisteredListener> listeners,
            Object sessionID, RefCachedData<DataType> dataToSend) {

        Iterator<RefCachedDataLink<DataType>.RegisteredListener> itr;
        itr = listeners.iterator();

        SublistenerException ex = null;
        while (itr.hasNext()) {
            try {
                do {
                    RefCachedDataLink<DataType>.RegisteredListener listener;
                    listener = itr.next();

                    listener.onDataArrive(sessionID, dataToSend);
                } while (itr.hasNext());
                break;
            } catch (Throwable subEx) {
                if (ex == null) {
                    ex = new SublistenerException(subEx);
                }
                else {
                    ex.addSuppressed(subEx);
                }
            }
        }

        if (ex != null) {
            throw ex;
        }
    }

    private static <DataType> void forwardDoneToAll(
            List<RefCachedDataLink<DataType>.RegisteredListener> listeners,
            Object sessionID, AsyncReport report) {

        Iterator<RefCachedDataLink<DataType>.RegisteredListener> itr;
        itr = listeners.iterator();

        SublistenerException ex = null;
        while (itr.hasNext()) {
            try {
                do {
                    RefCachedDataLink<DataType>.RegisteredListener listener;
                    listener = itr.next();

                    listener.onDoneReceive(sessionID, report);
                } while (itr.hasNext());
                break;
            } catch (Throwable subEx) {
                if (ex == null) {
                    ex = new SublistenerException(subEx);
                }
                else {
                    ex.addSuppressed(subEx);
                }
            }
        }

        if (ex != null) {
            throw ex;
        }
    }

    private class SessionForwarder
    implements
            AsyncDataListener<DataType> {

        private final Object sessionID;
        private final UpdateTaskExecutor dataSender;
        private boolean receivedData;

        public SessionForwarder(Object sessionID) {
            this.sessionID = sessionID;
            this.dataSender = new GenericUpdateTaskExecutor(eventScheduler);
            this.receivedData = false;
        }

        @Override
        public boolean requireData() {
            List<RegisteredListener> currentListeners = new LinkedList<>();

            listenersLock.lock();
            try {
                currentListeners.addAll(listeners);
            } finally {
                listenersLock.unlock();
            }

            for (RegisteredListener listener: currentListeners) {
                if (listener.requireData()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onDataArrive(DataType data) {
            dataSender.execute(new DataForwardTask(data));
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            eventScheduler.execute(new DoneForwardTask(report));
        }

        private class DataForwardTask implements Runnable {
            private final DataType data;

            public DataForwardTask(DataType data) {
                this.data = data;
            }

            @Override
            public void run() {
                receivedData = true;

                RefCachedData<DataType> dataToSend;
                dataToSend = new RefCachedData<>(data, refCreator, refType);

                if (currentSessionID == sessionID) {
                    if (lastData != null) {
                        lastData.clear();
                    }

                    lastData = dataToSend.getDataRef();
                }

                List<RegisteredListener> currentListeners = new LinkedList<>();

                listenersLock.lock();
                try {
                    currentListeners.addAll(listeners);
                } finally {
                    listenersLock.unlock();
                }

                forwardDataToAll(currentListeners, sessionID, dataToSend);
            }
        }

        private class DoneForwardTask implements Runnable {
            private final AsyncReport report;

            public DoneForwardTask(AsyncReport report) {
                this.report = report;
            }

            private void forwardCached(RefCachedData<DataType> cachedResult,
                    List<RegisteredListener> doneListeners) {
                assert cachedResult != null;

                SublistenerException ex = null;
                try {
                    forwardDataToAll(doneListeners, sessionID, cachedResult);
                } catch (Throwable subEx) {
                    if (subEx instanceof SublistenerException) {
                        ex = (SublistenerException)subEx;
                    }
                    else {
                        ex = new SublistenerException(subEx);
                    }
                }

                try {
                    forwardDoneToAll(doneListeners, sessionID, report);
                } catch (Throwable subEx) {
                    if (ex != null) {
                        ex.addSuppressed(subEx);
                    }
                    else if (subEx instanceof SublistenerException) {
                        ex = (SublistenerException)subEx;
                    }
                    else {
                        ex = new SublistenerException(subEx);
                    }
                }

                if (ex != null) {
                    throw ex;
                }
            }

            private void restartSession(List<RegisteredListener> toReRegister) {
                startNewSession();

                Object nextSessionID = currentSessionID;

                for (RegisteredListener listener: toReRegister) {
                    listener.initSession(nextSessionID);
                }

                AsyncDataController nextController;
                nextController = wrappedDataLink.getData(
                        new SessionForwarder(nextSessionID));
                currentController = nextController;

                for (RegisteredListener listener: toReRegister) {
                    listener.initController(nextController);
                }
            }

            private List<RegisteredListener> forwardDone(
                    List<RegisteredListener> currentListeners) {

                List<RegisteredListener> toReRegister = null;
                List<RegisteredListener> doneListeners = new LinkedList<>();

                for (RegisteredListener listener: currentListeners) {
                    if (!listener.isListenerReceivedData()) {
                        // Those who did not receive any data may need to be
                        // restarted.
                        if (listener.sessionID != sessionID) {
                            if (toReRegister == null) {
                                toReRegister = new LinkedList<>();
                            }
                            toReRegister.add(listener);
                        }
                    }
                    else {
                        doneListeners.add(listener);
                    }
                }

                forwardDoneToAll(doneListeners, sessionID, report);

                return toReRegister != null
                        ? toReRegister
                        : Collections.<RegisteredListener>emptyList();
            }

            @Override
            public void run() {
                assert eventScheduler.isCurrentThreadExecuting();

                List<RegisteredListener> currentListeners = new LinkedList<>();

                listenersLock.lock();
                try {
                    currentListeners.addAll(listeners);
                } finally {
                    listenersLock.unlock();
                }

                if (!currentListeners.isEmpty()) {
                    Throwable unexpectedException = null;
                    boolean doFinishSession = true;
                    try {
                        List<RegisteredListener> toReRegister = null;
                        toReRegister = forwardDone(currentListeners);

                        if (!toReRegister.isEmpty()) {
                            if (!receivedData) {
                                // If we did not receive any data then
                                // we don't have to restart just forward the
                                // onDoneReceive.
                                forwardDoneToAll(toReRegister, sessionID, report);
                            }
                            else {
                                RefCachedData<DataType> cachedResult;
                                cachedResult = sessionID == currentSessionID
                                        ? getCached()
                                        : null;

                                if (cachedResult != null) {
                                    // If the cache is not empty then the data
                                    // in the cache must have been the last data
                                    // sent in this session, so we may just forward
                                    // that and can avoid restarting.
                                    forwardCached(cachedResult, toReRegister);
                                }
                                else {
                                    // It seems we are out of luck. We have to
                                    // restart the listener so this time we will
                                    // surely receive the data.
                                    doFinishSession = false;
                                    restartSession(toReRegister);
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        unexpectedException = ex;
                    }

                    try {
                        if (doFinishSession) {
                            trySetFinishSession(sessionID, report);
                        }
                    } catch (Throwable ex) {
                        if (unexpectedException != null) {
                            unexpectedException.addSuppressed(ex);
                        }
                        else {
                            unexpectedException = ex;
                        }
                    }

                    if (unexpectedException != null) {
                        ExceptionHelper.rethrow(unexpectedException);
                    }
                }
            }
        }
    }

    private enum ExecutionState {
        NotStarted, Started, Finished
    }

    private class AsyncCancelTask implements Runnable {

        private RunnableFuture<?> callingTask;

        public void init(RunnableFuture<?> callingTask) {
            this.callingTask = callingTask;
        }

        @Override
        public void run() {
            assert callingTask != null;
            tryCancelNow(callingTask);
        }
    }

    private class CancelNowTask implements Runnable {
        private final RunnableFuture<?> callingTask;

        public CancelNowTask(RunnableFuture<?> callingTask) {
            this.callingTask = callingTask;
        }

        @Override
        public void run() {
            assert eventScheduler.isCurrentThreadExecuting();

            currentCancelTask.compareAndSet(callingTask, null);

            AsyncDataController controllerToCancel = null;

            listenersLock.lock();
            try {
                if (listeners.isEmpty()
                        && executionState == ExecutionState.Started) {

                    currentSessionID = null;
                    lastReport = null;
                    lastData = null;
                    executionState = ExecutionState.NotStarted;
                    controllerToCancel = currentController;
                }
            } finally {
                listenersLock.unlock();
            }

            if (controllerToCancel != null) {
                controllerToCancel.cancel();
                controllerToCancel = null;
            }
        }
    }

    private class RegisterNewListenerTask implements Runnable {
        private final RegisteredListener listener;

        public RegisterNewListenerTask(RegisteredListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            assert eventScheduler.isCurrentThreadExecuting();

            switch (executionState) {
                case NotStarted:
                {
                    startNewSession();
                    listener.initSession(currentSessionID);
                    currentController = wrappedDataLink.getData(
                            new SessionForwarder(currentSessionID));
                    listener.initController(currentController);
                    break;
                }
                case Started:
                {
                    Object lastSessionID = currentSessionID;
                    listener.initSession(lastSessionID);
                    listener.initController(currentController);

                    RefCachedData<DataType> toSend = getCached();
                    if (toSend != null) {
                        listener.trySendCachedData(lastSessionID, toSend);
                    }
                    break;
                }
                case Finished:
                {
                    Object lastSessionID = currentSessionID;

                    RefCachedData<DataType> toSend = getCached();

                    if (toSend != null) {
                        assert lastReport != null;

                        listener.initSession(lastSessionID);
                        listener.initController(currentController);

                        try {
                            listener.onDataArrive(lastSessionID, toSend);
                        } finally {
                            listener.onDoneReceive(lastSessionID, lastReport);
                        }
                    }
                    else {
                        startNewSession();
                        listener.initSession(currentSessionID);
                        currentController = wrappedDataLink.getData(
                                new SessionForwarder(currentSessionID));
                        listener.initController(currentController);
                    }
                    break;
                }
            }
        }
    }
}

