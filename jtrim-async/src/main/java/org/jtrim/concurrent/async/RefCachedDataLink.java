package org.jtrim.concurrent.async;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.ContextAwareTaskExecutor;
import org.jtrim.concurrent.ExecutorsEx;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncLinks#refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
 *
 * @author Kelemen Attila
 */
final class RefCachedDataLink<DataType>
implements
        AsyncDataLink<RefCachedData<DataType>> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

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

    // Everything is synchronized by being accessed on inOrderExecutor
    // So except for the executeSynchronized methods and where otherwise noted,
    // private methods (including even public methods of private inner classes)
    // are only allowed to be called in the context of inOrderExecutor.
    private final ContextAwareTaskExecutor inOrderExecutor;
    private final RefList<Registration> currentRegistrations;
    private SessionInfo<DataType> currentSession;

    public RefCachedDataLink(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeoutUnit) {
        ExceptionHelper.checkNotNullArgument(wrappedDataLink, "wrappedDataLink");
        ExceptionHelper.checkNotNullArgument(refType, "refType");
        ExceptionHelper.checkNotNullArgument(timeoutUnit, "timeoutUnit");
        ExceptionHelper.checkArgumentInRange(dataCancelTimeout, 0, Long.MAX_VALUE, "dataCancelTimeout");

        this.refType = refType;
        this.refCreator = refCreator != null
                ? refCreator
                : JavaRefObjectCache.INSTANCE;

        this.dataCancelTimeoutNanos = timeoutUnit.toNanos(dataCancelTimeout);
        this.wrappedDataLink = wrappedDataLink;

        this.inOrderExecutor = TaskExecutors.inOrderSyncExecutor();
        this.currentRegistrations = new RefLinkedList<>();
        this.currentSession = new SessionInfo<>();
    }

    private void executeSynchronized(CancelableTask task) {
        inOrderExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
    }

    private void executeSynchronized(final Runnable task) {
        executeSynchronized(new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                task.run();
            }
        });
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super RefCachedData<DataType>> dataListener) {
        final Registration registration = new Registration(cancelToken, dataListener);

        final InitLaterDataController controller = new InitLaterDataController();
        executeSynchronized(new Runnable() {
            @Override
            public void run() {
                AsyncDataController wrappedController;
                switch (currentSession.state) {
                    case NOT_STARTED:
                        wrappedController = startNewSession(registration);
                        break;
                    case RUNNING:
                        wrappedController = attachToSession(registration);
                        break;
                    case FINALIZING:
                        throw new IllegalStateException("This data link is"
                                + " broken due to an error in the"
                                + " onDoneReceive.");
                    case DONE:
                        wrappedController = attachToDoneSession(registration);
                        break;
                    default:
                        throw new AssertionError("Unexpected enum value.");
                }

                controller.initController(wrappedController);
            }
        });

        return new DelegatedAsyncDataController(controller);
    }

    private void clearCurrentSession() {
        assert inOrderExecutor.isExecutingInThis();

        Future<?> prevCancelTimer = currentSession.cancelTimerFuture;
        CancellationSource prevCancelSource = currentSession.cancelSource;
        currentSession = new SessionInfo<>();

        if (prevCancelTimer != null) {
            // Just to be on the safe side we don't interrupt
            // the thread, since we have no idea what thread would
            // that interrupt. Besides, cancellation tasks are expected
            // to be fast and should not consider thread interruption.
            prevCancelTimer.cancel(false);
        }
        if (prevCancelSource != null) {
            prevCancelSource.getController().cancel();
        }
    }

    private AsyncDataController startNewSession(Registration registration) {
        assert inOrderExecutor.isExecutingInThis();

        clearCurrentSession();

        registration.attach();
        return startNewSession();
    }

    // session must be cleared before this method call.
    private AsyncDataController startNewSession() {
        assert currentSession.state == ProviderState.NOT_STARTED;
        assert inOrderExecutor.isExecutingInThis();

        currentSession.controller = wrappedDataLink.getData(
                currentSession.cancelSource.getToken(),
                new DispatcherListener(currentSession));

        currentSession.state = ProviderState.RUNNING;
        return currentSession.controller;
    }

    private RefCachedData<DataType> getCurrentCachedData() {
        assert inOrderExecutor.isExecutingInThis();

        VolatileReference<DataType> cachedDataRef = currentSession.cachedData;
        if (cachedDataRef == null) {
            return null;
        }

        DataType cachedData = cachedDataRef.get();
        if (cachedData == null) {
            return null;
        }

        return new RefCachedData<>(cachedData, cachedDataRef);
    }

    private AsyncDataController attachToSession(Registration registration) {
        assert currentSession.state == ProviderState.RUNNING;
        assert inOrderExecutor.isExecutingInThis();

        registration.attach();

        RefCachedData<DataType> cachedData = getCurrentCachedData();
        if (cachedData != null) {
            registration.onDataArrive(cachedData);
            return currentSession.controller;
        }
        else {
            // We must restart the data retrieval if no more data will be
            // provided by the underlying data link and so must also replace
            // the controller.
            return registration.createReplacableController(currentSession.controller);
        }
    }

    private AsyncDataController attachToDoneSession(Registration registration) {
        assert currentSession.state == ProviderState.DONE;
        assert inOrderExecutor.isExecutingInThis();

        RefCachedData<DataType> cachedData = getCurrentCachedData();
        if (cachedData != null) {
            try {
                registration.onDataArrive(cachedData);
            } finally {
                registration.onDoneReceive(currentSession.finalReport);
            }

            return DoNothingDataController.INSTANCE;
        }
        else {
            return startNewSession(registration);
        }
    }

    private void dispatchData(DataType data) {
        assert inOrderExecutor.isExecutingInThis();

        currentSession.receivedData = true;
        RefCachedData<DataType> dataRef = new RefCachedData<>(data, refCreator, refType);

        // The previous data can be removed from the cache since, we have a new
        // more accurate one.
        VolatileReference<DataType> prevDataRef = currentSession.cachedData;
        if (prevDataRef != null) {
            prevDataRef.clear();
        }

        currentSession.cachedData = dataRef.getDataRef();

        Throwable error = null;
        for (Registration registration: currentRegistrations) {
            try {
                registration.onDataArrive(dataRef);
            } catch (Throwable ex) {
                if (error != null) error.addSuppressed(ex);
                else error = ex;
            }
        }

        ExceptionHelper.rethrowIfNotNull(error);
    }

    private void dispatchDone(AsyncReport report) {
        assert inOrderExecutor.isExecutingInThis();

        Throwable error = null;

        currentSession.state = ProviderState.FINALIZING;
        currentSession.controller = null;

        Future<?> cancelTimer = currentSession.cancelTimerFuture;
        currentSession.cancelTimerFuture = null;
        if (cancelTimer != null) {
            cancelTimer.cancel(false);
        }

        boolean sessionReceivedData = currentSession.receivedData;

        for (Registration registration: currentRegistrations) {
            try {
                // It is possible that a session was attached after the final data
                // has been sent and before onDoneReceive was called.
                // If data has been sent to the backing listener but not
                // the attached listener, we can be sure that we need to re-request
                // the data for that particular listener. These listeners will
                // remain in "currentRegistrations".
                if (!sessionReceivedData || registration.receivedData) {
                    registration.onDoneReceive(report);
                    // Notice that the onDoneReceive method call will remove
                    // "registration" from "currentRegistrations".
                }
            } catch (Throwable ex) {
                if (error != null) error.addSuppressed(ex);
                else error = ex;
            }
        }

        try {
            if (!currentRegistrations.isEmpty()) {
                clearCurrentSession();
                AsyncDataController newController = startNewSession();
                for (Registration registration: currentRegistrations) {
                    registration.replaceController(newController);
                }
            }
            else {
                currentSession.finalReport = report;
                currentSession.state = ProviderState.DONE;
            }
        } catch (Throwable ex) {
            if (error != null) error.addSuppressed(ex);
            else error = ex;
        }

        ExceptionHelper.rethrowIfNotNull(error);
    }

    private void checkStopCancellation() {
        assert inOrderExecutor.isExecutingInThis();

        if (!currentRegistrations.isEmpty()) {
            Future<?> currentCancelFuture = currentSession.cancelTimerFuture;
            currentSession.cancelTimerFuture = null;
            if (currentCancelFuture != null) {
                currentCancelFuture.cancel(false);
            }
        }
    }

    private void checkSessionCancellation() {
        assert inOrderExecutor.isExecutingInThis();

        if (!currentSession.state.isCompleted() && currentRegistrations.isEmpty()) {
            if (dataCancelTimeoutNanos == 0) {
                clearCurrentSession();
                return;
            }

            SessionInfo<?> cancelSession = currentSession;
            if (cancelSession.cancelTimerFuture == null) {
                final AtomicReference<Future<?>> cancelTimerFutureRef
                        = new AtomicReference<>();

                Future<?> currentFuture = CANCEL_TIMER.schedule(new Runnable() {
                    @Override
                    public void run() {
                        executeSynchronized(new Runnable() {
                            @Override
                            public void run() {
                                // cancelTimerFutureRef is already initialized
                                // here because this run() method cannot run
                                // concurrently, nor reentrantly with the
                                // outer code.
                                Future<?> ourFuture = cancelTimerFutureRef.get();
                                if (currentSession.cancelTimerFuture == ourFuture) {
                                    clearCurrentSession();
                                }
                            }
                        });
                    }
                }, dataCancelTimeoutNanos, TimeUnit.NANOSECONDS);

                cancelTimerFutureRef.set(currentFuture);
                cancelSession.cancelTimerFuture = currentFuture;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Cache [");
        result.append(refType);
        result.append("] result of ");
        AsyncFormatHelper.appendIndented(wrappedDataLink, result);

        return result.toString();
    }

    private static class SessionInfo<DataType> {
        public final Object sessionID = new Object();

        public final CancellationSource cancelSource = Cancellation.createCancellationSource();
        public ProviderState state = ProviderState.NOT_STARTED;
        public AsyncDataController controller = null;
        public VolatileReference<DataType> cachedData = null;
        public boolean receivedData = false;
        private Future<?> cancelTimerFuture = null;
        private AsyncReport finalReport;
    }

    private class Registration {
        private final CancellationToken cancelToken;
        private final AsyncDataListener<RefCachedData<DataType>> safeListener;
        private RefCollection.ElementRef<?> listenerRef;
        private ListenerRef cancelRef;
        private boolean receivedData;
        private ReplacableController controller;

        public Registration(
                CancellationToken cancelToken,
                AsyncDataListener<? super RefCachedData<DataType>> dataListener) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

            this.cancelToken = cancelToken;
            this.safeListener = AsyncHelper.makeSafeListener(dataListener);
            this.listenerRef = null;
            this.cancelRef = null;
            this.receivedData = false;
            this.controller = null;
        }

        public AsyncDataController createReplacableController(AsyncDataController initialController) {
            assert inOrderExecutor.isExecutingInThis();

            controller = new ReplacableController(initialController);
            return controller;
        }

        public void replaceController(AsyncDataController newController) {
            assert inOrderExecutor.isExecutingInThis();

            if (controller == null) {
                throw new IllegalStateException("Internal error: "
                        + "Unexpected new AsyncDataController");
            }

            controller.replaceController(newController);
            // We never need to restart the data transfer more than once and
            // we only replace controller when we restart.
            controller.willNotReplaceController();
        }

        public void onDataArrive(RefCachedData<DataType> dataRef) {
            assert inOrderExecutor.isExecutingInThis();

            receivedData = true;
            try {
                safeListener.onDataArrive(dataRef);
            } finally {
                // If we have received data, we will not replace the controller
                // because that means that from now on, we will receive every
                // data, so there is no reason to restart the data retrieval
                // an so no new controller is needed.
                if (controller != null) {
                    controller.willNotReplaceController();
                }
            }
        }

        public void onDoneReceive(AsyncReport report) {
            assert inOrderExecutor.isExecutingInThis();

            try {
                safeListener.onDoneReceive(report);
            } finally {
                cleanup();
                checkSessionCancellation();
            }
        }

        private void removeFromList() {
            assert inOrderExecutor.isExecutingInThis();

            RefCollection.ElementRef<?> currentRef = listenerRef;
            listenerRef = null;
            if (currentRef != null) {
                currentRef.remove();
            }
        }

        private void removeFromCancelToken() {
            assert inOrderExecutor.isExecutingInThis();

            ListenerRef currentRef = cancelRef;
            cancelRef = null;
            if (currentRef != null) {
                currentRef.unregister();
            }
        }

        private void cleanup() {
            assert inOrderExecutor.isExecutingInThis();

            removeFromList();
            removeFromCancelToken();
        }

        public void attach() {
            assert inOrderExecutor.isExecutingInThis();

            cleanup();

            listenerRef = currentRegistrations.addGetReference(this);
            checkStopCancellation();

            cancelRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    executeSynchronized(new Runnable() {
                        @Override
                        public void run() {
                            onDoneReceive(AsyncReport.CANCELED);
                        }
                    });
                }
            });
        }
    }

    /**
     * Called by external code, so inherited methods are not executed in the
     * context of inOrderExecutor.
     */
    private class DispatcherListener implements AsyncDataListener<DataType> {
        private final Object sessionID;
        private final UpdateTaskExecutor dataExecutor;

        public DispatcherListener(SessionInfo<?> session) {
            this.sessionID = session.sessionID;
            this.dataExecutor = new GenericUpdateTaskExecutor(inOrderExecutor);
        }

        private boolean isCurrentSession() {
            assert inOrderExecutor.isExecutingInThis();
            return currentSession.sessionID == sessionID;
        }

        @Override
        public void onDataArrive(final DataType data) {
            dataExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (isCurrentSession()) {
                        dispatchData(data);
                    }
                }
            });
        }

        @Override
        public void onDoneReceive(final AsyncReport report) {
            executeSynchronized(new Runnable() {
                @Override
                public void run() {
                    if (isCurrentSession()) {
                        dispatchDone(report);
                    }
                }
            });
        }
    }

    /**
     * Called by external code, so inherited methods are not executed in the
     * context of inOrderExecutor.
     */
    private class ReplacableController implements AsyncDataController {
        private List<Object> controllerArgs;
        private volatile AsyncDataController currentController;

        public ReplacableController(AsyncDataController initialController) {
            ExceptionHelper.checkNotNullArgument(initialController, "initialController");

            this.controllerArgs = new LinkedList<>();
            this.currentController = initialController;
        }

        @Override
        public void controlData(final Object controlArg) {
            executeSynchronized(new Runnable() {
                @Override
                public void run() {
                    List<Object> collectedControllerArgs = controllerArgs;
                    if (collectedControllerArgs != null) {
                        collectedControllerArgs.add(controlArg);
                    }
                    currentController.controlData(controlArg);
                }
            });
        }

        @Override
        public AsyncDataState getDataState() {
            return currentController.getDataState();
        }

        public void replaceController(AsyncDataController controller) {
            assert inOrderExecutor.isExecutingInThis();

            ExceptionHelper.checkNotNullArgument(controller, "controller");

            currentController = controller;
            for (Object controlArg: controllerArgs) {
                controller.controlData(controlArg);
            }
        }

        public void willNotReplaceController() {
            assert inOrderExecutor.isExecutingInThis();

            controllerArgs = null;
        }
    }

    private enum ProviderState {
        NOT_STARTED(false),
        RUNNING(false),
        FINALIZING(true),
        DONE(true);

        private final boolean completed;

        private ProviderState(boolean completed) {
            this.completed = completed;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}

