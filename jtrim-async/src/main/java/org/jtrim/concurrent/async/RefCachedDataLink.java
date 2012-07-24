package org.jtrim.concurrent.async;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.ChildCancellationSource;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncLinks#refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
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

    // This code needs a major cleanup (i.e.: complete rewrite), since upgrading
    // to the new cancellation way allows for simpler code (and it was more
    // complex than necessary anyway).

    private static final ScheduledExecutorService CANCEL_TIMER
            = ExecutorsEx.newSchedulerThreadedExecutor(1, true,
            "RefCachedDataLink cancel timer");

    private final ReferenceType refType;
    private final ObjectCache refCreator;
    private final AsyncDataLink<? extends DataType> wrappedDataLink;
    private final long dataCancelTimeoutNanos;

    private final ReentrantLock listenersLock;
    private final RefList<RegisteredListener> listeners;

    private final TaskExecutor eventScheduler;
    private ExecutionState executionState;
    private Object currentSessionID; // executionState != NotStarted
    private ChildCancellationSource currentChildCancel; // executionState != NotStarted
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

        this.eventScheduler = TaskExecutors.inOrderSyncExecutor();
        this.executionState = ExecutionState.NotStarted;
        this.wrappedDataLink = wrappedDataLink;
        this.currentSessionID = null;
        this.currentChildCancel = null;
        this.currentController = null;
        this.lastData = null;
        this.lastReport = null;

        this.currentCancelTask = new AtomicReference<>(null);
    }

    private void submitEventTask(CancelableTask eventTask) {
        eventScheduler.execute(Cancellation.UNCANCELABLE_TOKEN, eventTask, null);
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super RefCachedData<DataType>> dataListener) {

        RegisteredListener listener;
        listener = new RegisteredListener(null, dataListener);
        listener.register(cancelToken);

        submitEventTask(new RegisterNewListenerTask(cancelToken, listener));

        return listener;
    }

    private AsyncDataController getWrappedData(
            CancellationToken cancelToken, Object sessionID) {
        //assert eventScheduler.isCurrentThreadExecuting();

        ChildCancellationSource childCancelSource
                = Cancellation.createChildCancellationSource(cancelToken);

        currentChildCancel = childCancelSource;
        return wrappedDataLink.getData(
                childCancelSource.getToken(),
                new SessionForwarder(childCancelSource, sessionID));
    }

    private void tryCancelNow(RunnableFuture<?> callingTask) {
        submitEventTask(new CancelNowTask(callingTask));
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
        //assert eventScheduler.isCurrentThreadExecuting();
        executionState = ExecutionState.Started;
        currentSessionID = newSessionID();
        currentController = null;
        lastReport = null;
        lastData = null;
    }

    private void trySetFinishSession(Object sessionID,
            AsyncReport finishReport) {

        //assert eventScheduler.isCurrentThreadExecuting();
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
        //assert eventScheduler.isCurrentThreadExecuting();

        VolatileReference<DataType> dataRef = lastData;
        DataType data = dataRef != null ? dataRef.get() : null;

        return data != null ? new RefCachedData<>(data, dataRef) : null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Cache [");
        result.append(refType);
        result.append("] result of ");
        AsyncFormatHelper.appendIndented(wrappedDataLink, result);

        return result.toString();
    }

    private class RegisteredListener
    implements
            AsyncDataController {

        private Object sessionID;
        private volatile boolean listenerReceivedData;
        private final SafeCancelableListener<RefCachedData<DataType>> listener;
        private RefList.ElementRef<RegisteredListener> listRef;

        private final Lock controllerLock;
        private volatile List<Object> controlArgs;
        private volatile AsyncDataController controller;

        private RegisteredListener(Object sessionID,
                AsyncDataListener<? super RefCachedData<DataType>> listener) {
            assert listener != null;

            this.sessionID = sessionID;
            this.listener = new SafeCancelableListener<>(listener);
            this.listRef = null;
            this.listenerReceivedData = false;
            this.controllerLock = new ReentrantLock();
            this.controlArgs = new LinkedList<>();
            this.controller = null;
        }

        // Must be called before doing anything with this object
        public void register(CancellationToken cancelToken) {
            assert !listenersLock.isHeldByCurrentThread();

            listener.listenForCancellation(cancelToken, new Runnable() {
                @Override
                public void run() {
                    cancel();
                }
            });

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
            //assert eventScheduler.isCurrentThreadExecuting();
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

            //assert eventScheduler.isCurrentThreadExecuting();
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

            //assert eventScheduler.isCurrentThreadExecuting();
            assert listRef != null;
            if (dataSessionID == sessionID && !listenerReceivedData) {
                listenerReceivedData = true;
                listener.onDataArrive(data);
            }
        }

        private void onDoneReceive(Object sessionID, AsyncReport report) {
            //assert eventScheduler.isCurrentThreadExecuting();

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

        private final ChildCancellationSource cancelSource;
        private final Object sessionID;
        private final UpdateTaskExecutor dataSender;
        private boolean receivedData;

        public SessionForwarder(ChildCancellationSource cancelSource, Object sessionID) {
            this.cancelSource = cancelSource;
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
            submitEventTask(new DoneForwardTask(cancelSource, report));
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

        private class DoneForwardTask implements CancelableTask {
            private final ChildCancellationSource cancelSource;
            private final AsyncReport report;

            public DoneForwardTask(
                    ChildCancellationSource cancelSource, AsyncReport report) {
                this.cancelSource = cancelSource;
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

                AsyncDataController nextController = getWrappedData(
                        cancelSource.getParentToken(),
                        nextSessionID);
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
            public void execute(CancellationToken cancelToken) {
                //assert eventScheduler.isCurrentThreadExecuting();
                cancelSource.detachFromParent();

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
                        List<RegisteredListener> toReRegister;
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

    private class CancelNowTask implements CancelableTask {
        private final RunnableFuture<?> callingTask;

        public CancelNowTask(RunnableFuture<?> callingTask) {
            this.callingTask = callingTask;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            //assert eventScheduler.isCurrentThreadExecuting();

            currentCancelTask.compareAndSet(callingTask, null);

            ChildCancellationSource toCancel = null;

            listenersLock.lock();
            try {
                if (listeners.isEmpty()
                        && executionState == ExecutionState.Started) {

                    currentSessionID = null;
                    lastReport = null;
                    lastData = null;
                    executionState = ExecutionState.NotStarted;
                    toCancel = currentChildCancel;
                }
            } finally {
                listenersLock.unlock();
            }

            if (toCancel != null) {
                toCancel.getController().cancel();
            }
        }
    }

    private class RegisterNewListenerTask implements CancelableTask {
        private final CancellationToken taskCancelToken;
        private final RegisteredListener listener;

        public RegisterNewListenerTask(
                CancellationToken taskCancelToken, RegisteredListener listener) {
            this.taskCancelToken = taskCancelToken;
            this.listener = listener;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            //assert eventScheduler.isCurrentThreadExecuting();

            switch (executionState) {
                case NotStarted:
                {
                    startNewSession();
                    listener.initSession(currentSessionID);
                    currentController = getWrappedData(taskCancelToken, currentSessionID);
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
                        currentController = getWrappedData(taskCancelToken, currentSessionID);
                        listener.initController(currentController);
                    }
                    break;
                }
            }
        }
    }
}

