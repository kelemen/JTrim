/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.ExecutorsEx;
import org.jtrim.concurrent.async.*;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.ObjectFinalizer;

/**
 * @deprecated Use {@link GenericAsyncRenderer} instead.
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class SimpleAsyncComponentRenderer
implements
        AsyncComponentRenderer {

    private static final int DEFAULT_OVERWRITE_TIMEOUT = 1000;
    private static final ScheduledExecutorService expireTimer;

    private final ObjectFinalizer finalizer;

    // mainLock must never be accuired after a stateLock
    private final ReentrantLock mainLock;
    private final int overwriteTimeout;

    private final IdentityHashMap<Component, RenderingState> currentlyRendering;
    private final IdentityHashMap<Component, RenderingState> stateMap;

    private boolean shutteddown;
    private final RefLinkedList<RenderingInfo> renderingQueue;
    private final boolean drawsOnFirstData;

    private final ExecutorService renderingProcessor;

    static {
        expireTimer = Executors.newSingleThreadScheduledExecutor(
                new ExecutorsEx.NamedThreadFactory(true, "Paint timeout timer")
                );
    }

    public SimpleAsyncComponentRenderer() {
        this(DEFAULT_OVERWRITE_TIMEOUT);
    }

    public SimpleAsyncComponentRenderer(int overwriteTimeout) {
        this(overwriteTimeout, true);
    }

    public SimpleAsyncComponentRenderer(int overwriteTimeout,
            boolean drawsOnFirstData) {

        this(overwriteTimeout, drawsOnFirstData, 1);
    }

    private SimpleAsyncComponentRenderer(int overwriteTimeout,
            boolean drawsOnFirstData, int threadCount) {

        if (threadCount > 1) {
            // Note that this implementation currentlty does not support more
            // than one thread because it could cause rendering the same
            // component parallel because of async datas.
            // Also rendering on multiple threads is not useful if they return
            // reasonably fast as recommended because the actual drawing will be
            // done on a single thread as well (AWT event dispatching thread).
            throw new UnsupportedOperationException(
                    "Thread count larger than 1 is not supported.");
        }

        if (threadCount < 1) {
            throw new IllegalArgumentException(
                    "The thread count must be larger than zero.");
        }

        if (overwriteTimeout < 0) {
            throw new IllegalArgumentException("The overwrite timeout"
                    + " of the renderer must be non-negative.");
        }

        this.shutteddown = false;
        this.drawsOnFirstData = drawsOnFirstData;
        this.mainLock = new ReentrantLock();
        this.currentlyRendering = new IdentityHashMap<>();
        this.stateMap = new IdentityHashMap<>();
        this.overwriteTimeout = overwriteTimeout;
        this.renderingQueue = new RefLinkedList<>();
        this.renderingProcessor = ExecutorsEx.newMultiThreadedExecutor(
                threadCount, true, toString());

        this.finalizer = new ObjectFinalizer(new Runnable() {
            @Override
            public void run() {
                doShutdown();
            }
        }, "SimpleAsyncComponentPainter.shutdown()");
    }

    public boolean isShuttedDown() {
        mainLock.lock();
        try {
            return shutteddown;
        } finally {
            mainLock.unlock();
        }
    }

    private void doShutdown() {
        List<RenderingState> states = new LinkedList<>();

        mainLock.lock();
        try {
            shutteddown = true;
            for (RenderingState state: stateMap.values()) {
                states.add(state);
                state.setPendingRequest(null);
            }

            renderingQueue.clear();

            // Note that from this point the rendering
            // queue will remain empty because:
            //
            // 1. paintComponent will not allow starting new requests.
            // 2. There is no pending data.
            // 3. Received asynchronous data will be ignored.
        } finally {
            mainLock.unlock();
        }

        for (RenderingState state: states) {
            state.cancel();
        }

        // Note that since rendering queue is empty
        // the current tasks would do nothing anyway.
        renderingProcessor.shutdownNow();
    }

    @Override
    public void shutdown() {
        finalizer.doFinalize();
    }

    private void startRendering(RenderingState renderingState) {
        RenderingRequest request = renderingState.getRequest();
        Component component = request.getComponent();

        assert mainLock.isHeldByCurrentThread();

        RenderingState oldState;
        oldState = stateMap.put(component, renderingState);
        assert oldState == null;

        RenderingInfo renderingInfo = new RenderingInfo(renderingState, null);

        RefList.ElementRef<RenderingInfo> itemRef;
        itemRef = renderingQueue.addLastGetReference(renderingInfo);
        renderingState.setQueueRef(itemRef);
    }

    private void doneRendering(RenderingState renderingState) {
        RenderingRequest request = renderingState.getRequest();
        Component component = request.getComponent();

        assert mainLock.isHeldByCurrentThread();

        RenderingState oldState;
        oldState = stateMap.remove(component);
        assert oldState == renderingState;

        renderingState.setRenderingDone();
    }

    private RenderingInfo tryPollFromRenderingQueue() {
        mainLock.lock();
        try {
            return renderingQueue.poll();
        } finally {
            mainLock.unlock();
        }
    }

    private void addRenderingTask() {
        renderingProcessor.execute(new InternalRenderingTask());
    }

    private void finishRenderingIfRequired(RenderingState renderingState) {
        Component component = renderingState.getRequest().getComponent();
        RenderingState pendingRequest = null;
        RunnableFuture<?> previousTimer = null;

        mainLock.lock();
        try {
            boolean hasFinished;

            hasFinished =
                    !renderingState.isInRenderingQueue()
                    && renderingState.isDataReceiveDone()
                    && currentlyRendering.get(component) != renderingState;

            if (hasFinished && !renderingState.isRenderingDone()) {
                doneRendering(renderingState);
                previousTimer = renderingState.getPendingRequestTimer();
                pendingRequest = renderingState.getPendingRequest();

                if (pendingRequest != null) {
                    startRendering(pendingRequest);
                }
            }
        } finally {
            mainLock.unlock();
        }

        // Since the rendering request is done,
        // there is no reason for this timer.
        if (previousTimer != null) {
            // It is unlikely that the cancel task will respond to an interrupt
            // so its safer this way.
            previousTimer.cancel(false);
        }

        if (pendingRequest != null) {
            addRenderingTask();
            pendingRequest.startRetrievingData();
        }
    }

    @Override
    public RenderingFuture renderComponent(int priority, Component component,
            ComponentRenderer renderer, RenderingParameters renderingParams,
            DrawingConnector<Object> drawingConnector) {

        ExceptionHelper.checkNotNullArgument(component, "component");
        ExceptionHelper.checkNotNullArgument(renderer, "renderer");
        ExceptionHelper.checkNotNullArgument(renderingParams, "renderingParams");
        ExceptionHelper.checkNotNullArgument(drawingConnector, "drawingConnector");

        RenderingRequest request;
        request = new RenderingRequest(priority, component, renderer,
                renderingParams, drawingConnector);

        RenderingState newState = new RenderingState(request);
        boolean startReceivingData;
        RenderingState stateToCancel = null;

        RenderingFuture result = new SimpleRenderingFuture(newState);

        RunnableFuture<?> newTimer = null;

        mainLock.lock();
        try {
            if (shutteddown) {
                // do nothing after shutdown.
                return DummyRenderingFuture.INSTANCE;
            }

            final RenderingState currentState = stateMap.get(component);

            if (currentState == null) {
                startRendering(newState);
                startReceivingData = true;
            }
            else {
                newTimer = currentState.setPendingRequest(newState);
                stateToCancel = currentState.isAlreadyPainted()
                        ? currentState
                        : null;

                startReceivingData = false;
            }
        } finally {
            mainLock.unlock();
        }

        if (startReceivingData) {
            addRenderingTask();
            newState.startRetrievingData();
        }

        if (stateToCancel != null) {
            // if stateToCancel is not null, starting the
            // cancel timer is useless because we can cancel this request
            // rightaway.
            stateToCancel.cancel();
        }
        else {
            if (newTimer != null) {
                expireTimer.schedule(newTimer,
                        overwriteTimeout, TimeUnit.MILLISECONDS);
            }
        }

        return result;
    }

    private static class RenderingInfo {
        private final RenderingState state;
        private final Object asyncData;

        public RenderingInfo(RenderingState state, Object asyncData) {
            assert state != null;

            this.state = state;
            this.asyncData = asyncData;
        }

        public Object getAsyncData() {
            return asyncData;
        }

        public RenderingState getState() {
            return state;
        }
    }

    /**
     * @deprecated Marked to avoid warning.
     */
    @Deprecated
    private static class RenderingRequest {
        private final int priority;
        private final Component component;
        private final ComponentRenderer renderer;
        private final RenderingParameters renderingArgs;
        private final DrawingConnector<Object> drawingConnector;

        public RenderingRequest(int priority, Component component,
                ComponentRenderer renderer, RenderingParameters renderingArgs,
                DrawingConnector<Object> drawingConnector) {

            this.priority = priority;
            this.component = component;
            this.renderer = renderer;
            this.renderingArgs = renderingArgs != null
                    ? renderingArgs
                    : new RenderingParameters(null);

            this.drawingConnector = drawingConnector;
        }

        public Component getComponent() {
            return component;
        }

        public DrawingConnector<Object> getDrawingConnector() {
            return drawingConnector;
        }

        public int getPriority() {
            return priority;
        }

        public ComponentRenderer getRenderer() {
            return renderer;
        }

        public RenderingParameters getRenderingArgs() {
            return renderingArgs;
        }
    }

    private class RenderingDataListener implements AsyncDataListener<Object> {
        private final RenderingState renderingState;

        public RenderingDataListener(RenderingState renderingState) {
            this.renderingState = renderingState;
        }

        @Override
        public void onDataArrive(Object newData) {
            if (renderingState.isCanceled()) {
                // If the rendering was canceled there is no reason to handle
                // the data.
                return;
            }

            boolean hasPendingRequest = false;
            boolean needNewTask = false;
            RenderingInfo newRenderingInfo;
            newRenderingInfo = new RenderingInfo(renderingState, newData);

            mainLock.lock();
            try {
                if (!shutteddown) {
                    RefList.ElementRef<RenderingInfo> queueRef;
                    queueRef = renderingState.getQueueRef();

                    if (queueRef != null && !queueRef.isRemoved()) {
                        queueRef.setElement(newRenderingInfo);
                    }
                    else {
                        queueRef = renderingQueue.addLastGetReference(newRenderingInfo);
                        renderingState.setQueueRef(queueRef);
                        needNewTask = true;
                    }

                    hasPendingRequest = renderingState.getPendingRequest() != null;
                }
            } finally {
                mainLock.unlock();
            }

            if (hasPendingRequest && drawsOnFirstData) {
                // If there is a pending request and we can assume
                // that the renderer will draw something on the first async
                // data, we may stop receiving anymore data because
                // this rendering will be canceled anyway.
                renderingState.cancelAsyncData();
            }

            if (needNewTask) {
                addRenderingTask();
            }
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            renderingState.setDataReceiveDone();
            finishRenderingIfRequired(renderingState);
        }
    }

    /**
     * @deprecated Marked to avoid warning.
     */
    @Deprecated
    private class RenderingState {
        private final ReentrantLock stateLock;
        private final RenderingRequest request;

        private long renderStartTime;
        private long renderEndTime;

        private boolean earlyDataSkip;
        private boolean dataReceiveStarted;
        private boolean dataReceiveDone;
        private CancellationController cancelController;
        private AsyncDataController dataController;
        private AsyncDataListener<Object> dataListener;

        private boolean alreadyPainted;
        private boolean renderingDone;
        private boolean canceled;

        private RenderingState pendingRequest;
        private RunnableFuture<?> pendingRequestTimer;

        private RefList.ElementRef<RenderingInfo> queueRef;

        public RenderingState(RenderingRequest request) {
            assert request != null;

            this.request = request;
            this.renderStartTime = System.nanoTime();
            // it should not be used before rendering is actually done
            // but setting this value will make it more noticable if
            // it is used because of a bug.
            this.renderEndTime = renderStartTime - 1;

            this.stateLock = new ReentrantLock();
            this.dataReceiveStarted = false;
            this.earlyDataSkip = false;
            this.dataReceiveDone = false;
            this.alreadyPainted = false;
            this.renderingDone = false;
            this.canceled = false;
            this.cancelController = null;
            this.dataController = null;
            this.dataListener = null;
            this.queueRef = null;
            this.pendingRequest = null;
        }

        private boolean hasLocks() {
            return mainLock.isHeldByCurrentThread()
                    || stateLock.isHeldByCurrentThread();
        }


        public void startRetrievingData() {
            assert !hasLocks()
                    : "Locks are not allowed here because unknown codes may"
                    + " cause dead-lock.";

            boolean skipStartReceive;
            boolean earlySkipWasReceived;

            stateLock.lock();
            try {
                skipStartReceive = dataReceiveStarted;
                dataReceiveStarted = true;
                earlySkipWasReceived = earlyDataSkip;
            } finally {
                stateLock.unlock();
            }

            if (skipStartReceive) {
                return;
            }

            AsyncDataListener<Object> dataHandler =
                    new RenderingDataListener(this);

            AsyncDataListener<Object> dataOrderer
                    = AsyncHelper.makeSafeListener(dataHandler);

            stateLock.lock();
            try {
                dataListener = dataOrderer;
            } finally {
                stateLock.unlock();
            }

            AsyncDataController controller;
            CancellationSource cancelSource;

            if (request.getRenderingArgs().hasBlockingData()
                    && !earlySkipWasReceived) {

                RenderingParameters renderingArgs = request.getRenderingArgs();
                cancelSource = Cancellation.createCancellationSource();
                controller = renderingArgs.getAsyncBlockingData(
                        cancelSource.getToken(), dataOrderer);
            }
            else {
                // If there is no blocking data emulate it as an immediately
                // finished data request.
                dataOrderer.onDoneReceive(AsyncReport.SUCCESS);
                controller = null;
                cancelSource = null;
            }

            stateLock.lock();
            try {
                assert dataController == null;
                assert cancelController == null;

                dataController = controller;
                cancelController = cancelSource != null
                        ? cancelSource.getController()
                        : null;
            } finally {
                stateLock.unlock();
            }
        }

        public boolean isCanceled() {
            stateLock.lock();
            try {
                return canceled;
            } finally {
                stateLock.unlock();
            }
        }

        public void cancel() {
            assert !hasLocks()
                    : "Locks are not allowed here because unknown codes may"
                    + " cause dead-lock.";

            boolean doCancelData = false;
            boolean mayFinished = false;

            mainLock.lock();
            try {
                stateLock.lock();
                try {
                    canceled = true;

                    if (dataReceiveStarted) {
                        doCancelData = true;
                    }
                    else {
                        earlyDataSkip = true;
                    }

                } finally {
                    stateLock.unlock();
                }
            } finally {
                mainLock.unlock();
            }

            if (doCancelData) {
                cancelAsyncData();
            }

            mainLock.lock();
            try {
                stateLock.lock();
                try {
                    if (queueRef != null) {
                        queueRef.remove();
                        queueRef = null;
                        mayFinished = true;
                    }
                } finally {
                    stateLock.unlock();
                }
            } finally {
                mainLock.unlock();
            }

            if (mayFinished) {
                finishRenderingIfRequired(this);
            }
        }

        public void cancelAsyncData() {
            assert !hasLocks()
                    : "Locks are not allowed here because unknown codes may"
                    + " cause dead-lock.";

            CancellationController currentCancelController;
            AsyncDataListener<Object> currentListener;

            stateLock.lock();
            try {
                currentCancelController = cancelController;
                currentListener = dataListener;
                dataReceiveDone = true;
            } finally {
                stateLock.unlock();
            }

            if (currentCancelController != null) {
                currentCancelController.cancel();
            }

            if (currentListener != null) {
                // We do not want to rely on the implementation of
                // current data link and will force a cancel. Since the
                // listener was made as safe further data receiving will be
                // ignored.
                // Nevertheless the code should work even if this call was not
                // here, this is here only to increase perceived performance by
                // allowing the next rendering to continue as soon as possible.
                currentListener.onDoneReceive(AsyncReport.CANCELED);
            }
        }

        public AsyncDataState getAsyncState() {
            assert !hasLocks()
                    : "Locks are not allowed here because unknown codes may"
                    + " cause dead-lock.";

            AsyncDataController currentDataController;

            stateLock.lock();
            try {
                currentDataController = dataController;
            } finally {
                stateLock.unlock();
            }

            return currentDataController != null
                    ? currentDataController.getDataState()
                    : null;
        }

        public RenderingRequest getRequest() {
            return request;
        }

        public void setAlreadyPainted() {
            boolean hasPendingRequest;

            stateLock.lock();
            try {
                alreadyPainted = true;
                hasPendingRequest = pendingRequest != null;
            } finally {
                stateLock.unlock();
            }

            if (hasPendingRequest) {
                cancel();
            }
        }

        public boolean isAlreadyPainted() {
            stateLock.lock();
            try {
                return alreadyPainted;
            } finally {
                stateLock.unlock();
            }
        }

        public boolean isInRenderingQueue() {
            mainLock.lock();
            try {
                stateLock.lock();
                try {
                    return queueRef != null ? !queueRef.isRemoved() : false;
                } finally {
                    stateLock.unlock();
                }
            } finally {
                mainLock.unlock();
            }
        }

        public RefList.ElementRef<RenderingInfo> getQueueRef() {
            stateLock.lock();
            try {
                return queueRef;
            } finally {
                stateLock.unlock();
            }
        }

        public void setQueueRef(RefList.ElementRef<RenderingInfo> queueRef) {
            stateLock.lock();
            try {
                this.queueRef = queueRef;
            } finally {
                stateLock.unlock();
            }
        }

        public RunnableFuture<?> getPendingRequestTimer() {
            stateLock.lock();
            try {
                return pendingRequestTimer;
            } finally {
                stateLock.unlock();
            }
        }

        public RenderingState getPendingRequest() {
            stateLock.lock();
            try {
                return pendingRequest;
            } finally {
                stateLock.unlock();
            }
        }

        public RunnableFuture<?> setPendingRequest(RenderingState pendingRequest) {
            RunnableFuture<?> resultTimer = null;

            stateLock.lock();
            try {
                if (this.pendingRequest == null && pendingRequest != null) {
                    resultTimer = new FutureTask<>(new Runnable() {
                        @Override
                        public void run() {
                            RenderingState.this.cancel();
                        }
                    }, null);

                    this.pendingRequestTimer = resultTimer;
                }

                this.pendingRequest = pendingRequest;
            } finally {
                stateLock.unlock();
            }

            return resultTimer;
        }

        public long getRenderingTime() {
            stateLock.lock();
            try {
                if (renderingDone) {
                    return renderEndTime - renderStartTime;
                }
            } finally {
                stateLock.unlock();
            }

            return System.nanoTime() - renderStartTime;
        }

        public boolean isRenderingDone() {
            stateLock.lock();
            try {
                return renderingDone;
            } finally {
                stateLock.unlock();
            }
        }

        public void setRenderingDone() {
            long time = System.nanoTime();

            stateLock.lock();
            try {
                if (!renderingDone) {
                    renderEndTime = time;
                    renderingDone = true;
                }
            } finally {
                stateLock.unlock();
            }
        }

        public boolean isDataReceiveDone() {
            stateLock.lock();
            try {
                return dataReceiveDone;
            } finally {
                stateLock.unlock();
            }
        }

        public void setDataReceiveDone() {
            stateLock.lock();
            try {
                this.dataReceiveDone = true;
            } finally {
                stateLock.unlock();
            }
        }
    }

    /**
     * @deprecated Marked to avoid warning.
     */
    @Deprecated
    private static class SimpleRenderingFuture implements RenderingFuture {
        private final RenderingState renderingState;

        public SimpleRenderingFuture(RenderingState renderingState) {
            assert renderingState != null;

            this.renderingState = renderingState;
        }

        @Override
        public boolean hasPainted() {
            return renderingState.isAlreadyPainted();
        }

        @Override
        public boolean isRenderingDone() {
            return renderingState.isRenderingDone();
        }

        @Override
        public long getRenderingTime() {
            return renderingState.getRenderingTime();
        }

        @Override
        public AsyncDataState getAsyncDataState() {
            return renderingState.getAsyncState();
        }

        @Override
        public void cancel() {
            renderingState.cancel();
        }
    }

    /**
     * @deprecated Marked to avoid warning.
     */
    @Deprecated
    private enum DummyRenderingFuture implements RenderingFuture {
        INSTANCE;

        @Override
        public boolean hasPainted() {
            return false;
        }

        @Override
        public boolean isRenderingDone() {
            return true;
        }

        @Override
        public long getRenderingTime() {
            return 0;
        }

        @Override
        public AsyncDataState getAsyncDataState() {
            return null;
        }

        @Override
        public void cancel() {
        }
    }

    /**
     * @deprecated Marked to avoid warning.
     */
    @Deprecated
    private class InternalRenderingTask implements Runnable {
        private void startDrawing(RenderingState state) {
            Component component = state.getRequest().getComponent();

            mainLock.lock();
            try {
                RenderingState oldState;
                oldState = currentlyRendering.put(component, state);
                assert oldState == null;
            } finally {
                mainLock.unlock();
            }
        }

        private void endDrawing(RenderingState state) {
            Component component = state.getRequest().getComponent();

            mainLock.lock();
            try {
                RenderingState oldState;
                oldState = currentlyRendering.remove(component);
                assert oldState == state;
            } finally {
                mainLock.unlock();
            }
        }

        private BufferedImage getPaintBuffer(RenderingInfo info) {
            RenderingRequest request = info.getState().getRequest();

            ComponentRenderer renderer = request.getRenderer();

            int reqType = renderer.getRequiredDrawingSurfaceType(
                    request.getRenderingArgs().getUserDefinedParams(),
                    info.getAsyncData());

            return request.getDrawingConnector().getDrawingSurface(reqType);
        }

        @Override
        public void run() {
            RenderingInfo renderingInfo;
            renderingInfo = tryPollFromRenderingQueue();

            if (renderingInfo == null) {
                return;
            }

            RenderingState state = renderingInfo.getState();
            RenderingRequest request = state.getRequest();
            ComponentRenderer renderer = request.getRenderer();
            Object userDefArgs = request.getRenderingArgs().getUserDefinedParams();
            Object asyncData = renderingInfo.getAsyncData();
            DrawingConnector<Object> drawingConnector = request.getDrawingConnector();
            AsyncRenderingResult result = null;
            boolean renderSuccess = false;

            try {
                if (!state.isCanceled()) {
                    startDrawing(state);
                    try {
                        BufferedImage drawingSurface;
                        drawingSurface = getPaintBuffer(renderingInfo);

                        try {
                            result = renderer.renderComponent(
                                    userDefArgs,
                                    asyncData,
                                    drawingSurface);
                            renderSuccess = true;
                        } finally {
                            if (result == null) {
                                drawingConnector.presentNewImage(
                                        drawingSurface,
                                        null);
                            }
                            else if (renderSuccess && result.needPaint()) {
                                drawingConnector.presentNewImage(
                                        drawingSurface,
                                        result.getPaintResult());
                            }
                            else if (drawingSurface != null) {
                                drawingConnector.offerBuffer(drawingSurface);
                            }
                        }

                        if (result == null) {
                            result = AsyncRenderingResult.done(true);
                        }

                        if (result.isRenderingFinished()) {
                            state.cancel();
                        }

                        if (renderSuccess && result.needPaint()) {
                            state.setAlreadyPainted();
                            renderer.displayResult();
                        }

                    } finally {
                        endDrawing(state);
                    }
                }
            } finally {
                finishRenderingIfRequired(state);
            }
        }
    }
}
