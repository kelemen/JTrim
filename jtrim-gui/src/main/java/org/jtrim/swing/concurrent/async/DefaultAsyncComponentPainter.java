/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.concurrent.ExecutorsEx;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncReport;


/**
 * @deprecated Use {@link SimpleAsyncComponentRenderer} instead.
 * @author Kelemen Attila
 */
@Deprecated
public final class DefaultAsyncComponentPainter
implements
        AsyncComponentRenderer {

    private static final long BLOCKED_RENDERING_SLEEPTIME = 50L; // ms

    private final FifoRenderTask fifoRenderTask;
    private final PriorityRenderTask priorityRenderTask;
    private final PrerendererPollTask prerendererPollTask;

    private final Lock prerendererLock;
    private final Lock mainLock;

    private final WeakHashMap<Component, RunningRenderingDescriptor> currentlyRendering;
    private final LinkedList<PrerenderingData> prerendererQueue;
    private final LinkedList<RenderingData> rendererQueue;

    private final WeakHashMap<Component, DataListener> blockingAsyncQueue;

    private final ExecutorService prerendererExecutor;
    private final ExecutorService priorityExecutor;
    private final ExecutorService fifoExecutor;

    private volatile boolean shutteddown;

    public DefaultAsyncComponentPainter() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public DefaultAsyncComponentPainter(int numberOfThreads) {
        final int hashMapSize = 64;

        this.shutteddown = false;
        this.prerendererLock = new ReentrantLock();
        this.mainLock = new ReentrantLock();
        this.currentlyRendering = new WeakHashMap<>(hashMapSize);
        this.prerendererQueue = new LinkedList<>();
        this.rendererQueue = new LinkedList<>();
        this.blockingAsyncQueue = new WeakHashMap<>(hashMapSize);

        // The prerenderer task is not designed to run on multiple threads
        // so this executor must have a maximum of one thread.
        this.prerendererExecutor = ExecutorsEx.newMultiThreadedExecutor(1, true);

        if (numberOfThreads > 1) {
            priorityExecutor = ExecutorsEx.newMultiThreadedExecutor(numberOfThreads - 1, true);
            fifoExecutor = ExecutorsEx.newMultiThreadedExecutor(1, true);
        }
        else {
            priorityExecutor = ExecutorsEx.newMultiThreadedExecutor(1, true);
            fifoExecutor = null;
        }

        this.fifoRenderTask = new FifoRenderTask();
        this.priorityRenderTask = new PriorityRenderTask();
        this.prerendererPollTask = new PrerendererPollTask();
    }

    @Override
    public void shutdown() {

        List<DataListener> remainingListeners = new LinkedList<>();

        mainLock.lock();
        try {
            shutteddown = true;
            remainingListeners.addAll(blockingAsyncQueue.values());
        } finally {
            mainLock.unlock();
        }

        for (DataListener listener: remainingListeners) {
            listener.stop();
        }

        if (prerendererExecutor != null) prerendererExecutor.shutdownNow();
        if (priorityExecutor != null) priorityExecutor.shutdownNow();
        if (fifoExecutor != null) fifoExecutor.shutdownNow();
    }

    @Override
    public RenderingFuture renderComponent(
            int priority,
            Component component,
            ComponentRenderer renderer,
            RenderingParameters renderingParams,
            DrawingConnector drawingConnector) {

        if (prerendererExecutor == null) {
            throw new IllegalStateException("The component painter is broken and cannot render.");
        }

        try {
            // If any exception is thrown in this block we may not able to verify
            // that the component did not override equals and hashCode.
            // However in this case we prefer believing the user of this class
            // rather than failing to render.
            Method m;
            m = component.getClass().getMethod("equals", Object.class);
            if (m.getDeclaringClass() != Object.class) {
                throw new IllegalArgumentException("Component cannot override equals(Object)");
            }

            m = component.getClass().getMethod("hashCode");
            if (m.getDeclaringClass() != Object.class) {
                throw new IllegalArgumentException("Component cannot override hashCode()");
            }
        } catch (NoSuchMethodException | SecurityException ex) {
            // NoSuchMethodException:
            // This should never happen since equals and hashCode are always available.
            // Blind faith
            //
            // SecurityException:
            // This may only happen in extreme cases, see the documentation
            // of Class.getMethod
            // Blind faith
        }
        PrerenderingData removed = null;
        boolean alreadyQueued = false;
        PrerenderingData newData = new PrerenderingData(priority, component, renderer, renderingParams, drawingConnector);
        DefaultRenderingFuture result = new DefaultRenderingFuture(newData);
        newData.setRenderingFuture(result);

        prerendererLock.lock();
        try {
            ListIterator<PrerenderingData> preIterator = prerendererQueue.listIterator();

            while (preIterator.hasNext()) {
                PrerenderingData data = preIterator.next();
                if (component.equals(data.getComponent())) {
                    alreadyQueued = true;
                    removed = data;
                    preIterator.set(newData);
                    break;
                }
            }

            if (!alreadyQueued) {
                prerendererQueue.addLast(newData);
            }
        } finally {
            prerendererLock.unlock();
        }

        if (!alreadyQueued) {
            prerendererExecutor.execute(prerendererPollTask);
        }
        else {
            onDoneRendering(removed);
        }

        return result;
    }

    private void onDoneRendering(PrerenderingData data) {
        if (data != null) {
            data.setDone();
        }
    }

    private void checkDone(PrerenderingData prerenderingData, boolean checkCurrent) {
        Component component = prerenderingData.getComponent();
        boolean doneRendering = false;

        if (component != null) {
            mainLock.lock();
            try {
                RunningRenderingDescriptor runningDescr = currentlyRendering.get(component);

                if (!checkCurrent || runningDescr == null || runningDescr.getPrerenderingData() != prerenderingData) {
                    DataListener blockingListener = blockingAsyncQueue.get(component);
                    if (blockingListener == null || blockingListener.getBlockedProcess() != prerenderingData) {
                        doneRendering = true;
                        for (RenderingData data: rendererQueue) {
                            if (data.getPrerenderingData() == prerenderingData) {
                                doneRendering = false;
                                break;
                            }
                        }
                    }
                }
            } finally {
                mainLock.unlock();
            }
        }
        else {
            prerenderingData.cancel();
            doneRendering = true;
        }

        if (doneRendering) {
            onDoneRendering(prerenderingData);
        }
    }

    private BufferedImage getPaintBuffer(RenderingData data) {
        ComponentRenderer renderer = data.getRenderer();

        ControlledBlockedData blockedData = data.getBlockedData();

        int reqType = renderer.getRequiredDrawingSurfaceType(
                data.getRenderingParams(),
                blockedData != null ? blockedData.getRawData() : null);

        return data.getDrawingConnector().getDrawingSurface(reqType);
    }

    private void stopBlockingData(PrerenderingData prerenderingData) {
        Component renderedComponent = prerenderingData.getComponent();
        if (renderedComponent == null) return;

        DataListener blockingListener;
        mainLock.lock();
        try {
            blockingListener = blockingAsyncQueue.get(renderedComponent);
            if (blockingListener != null) {
                if (blockingListener.getBlockedProcess() == prerenderingData) {
                    blockingAsyncQueue.remove(renderedComponent);
                }
                else {
                    blockingListener = null;
                }
            }
        } finally {
            mainLock.unlock();
        }

        if (blockingListener != null) {
            blockingListener.stop();
        }
    }

    private void removeFromRendererQueue(PrerenderingData prerenderingData) {
        mainLock.lock();
        try {
            Iterator<RenderingData> rendererItr = rendererQueue.iterator();

            while (rendererItr.hasNext()) {
                RenderingData data = rendererItr.next();
                if (data.getPrerenderingData() == prerenderingData) {
                    rendererItr.remove();
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void insertIntoRendererQueue(RenderingData data, boolean hasPriority) {
        if (data == null) return;

        Component newComponent = data.getComponent();
        if (newComponent == null) return;

        boolean done = false;
        RenderingData toDispose = null;

        mainLock.lock();
        try {
            if (!shutteddown) {
                ListIterator<RenderingData> itr = rendererQueue.listIterator();
                while (itr.hasNext()) {
                    RenderingData current = itr.next();
                    if (newComponent.equals(current.getComponent())) {
                        done = true;
                        if (hasPriority) {
                            itr.set(data);
                            toDispose = current;
                        }
                        else {
                            toDispose = data;
                        }

                        break;
                    }
                }

                if (!done) {
                    rendererQueue.addLast(data);
                }
            }
            else {
                done = true;
                toDispose = data;
            }

        } finally {
            mainLock.unlock();
        }

        if (!done) {
            if (priorityExecutor != null) {
                priorityExecutor.execute(priorityRenderTask);
            }

            if (fifoExecutor != null) {
                fifoExecutor.execute(fifoRenderTask);
            }
        }

        if (toDispose != null) {
            checkDone(toDispose.getPrerenderingData(), true);
        }
    }

    private class DataListener implements AsyncDataListener<Object> {
        private boolean stopped;
        private final PrerenderingData blockedProcess;
        private AsyncDataController dataController;
        private CancellationController controller;
        private boolean done;

        public DataListener(PrerenderingData blockedProcess) {
            this.stopped = false;
            this.done = false;
            this.blockedProcess = blockedProcess;
            this.dataController = null;
            this.controller = null;
        }

        public PrerenderingData getBlockedProcess() {
            return blockedProcess;
        }

        @Override
        public boolean requireData() {
            boolean result = true;

            mainLock.lock();
            try {
                for (RenderingData data: rendererQueue) {
                    if (data.getPrerenderingData() == blockedProcess) {
                        result = false;
                        break;
                    }
                }
            } finally {
                mainLock.unlock();
            }

            return result;
        }

        @Override
        public void onDataArrive(Object newData) {
            RenderingData renderingData = new RenderingData(
                    blockedProcess,
                    new ControlledBlockedData(newData, controller));

            if (renderingData.getComponent() == null) {
                stop();
                return;
            }

            mainLock.lock();
            try {
                if (!stopped) {
                    insertIntoRendererQueue(renderingData, true);
                }
            } finally {
                mainLock.unlock();
            }

        }

        public boolean isFinished() {
            boolean result;

            mainLock.lock();
            try {
                result = done || stopped;
            } finally {
                mainLock.unlock();
            }

            return result;
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            mainLock.lock();
            try {
                if (!stopped) {
                    done = true;
                    blockingAsyncQueue.remove(blockedProcess.getComponent());
                }
            } finally {
                mainLock.unlock();
            }
        }

        public CancellationController getController() {
            return controller;
        }

        public AsyncDataState getDataState() {
            return dataController != null
                    ? dataController.getDataState()
                    : null;
        }

        public void setController(
                AsyncDataController dataController,
                CancellationController controller) {

            this.dataController = dataController;
            this.controller = controller;

            boolean cancelImmediately;

            mainLock.lock();
            try {
                cancelImmediately = stopped;
            } finally {
                mainLock.unlock();
            }

            if (cancelImmediately) {
                if (controller != null) {
                    controller.cancel();
                }
            }
        }

        private void stop() {
            mainLock.lock();
            try {
                stopped = true;
            } finally {
                mainLock.unlock();
            }

            if (controller != null) {
                controller.cancel();
            }
        }
    }

    private class PrerendererPollTask implements Runnable {
        @Override
        public void run() {
            PrerenderingData newData;

            // This loop is not necessary
            // but remains here so the renderer may recover from
            // possible bugs. (ie.: not having enough tasks to process
            // every element of "prerendererQueue")
            do {
                prerendererLock.lock();
                try {
                    newData = prerendererQueue.poll();
                } finally {
                    prerendererLock.unlock();
                }

                if (newData != null) {
                    RenderingData renderingData = new RenderingData(newData, null);

                    if (renderingData.hasBlockingData()) {
                        RenderingParameters renderingParams = renderingData.getRenderingParams();
                        DataListener oldListener;

                        mainLock.lock();
                        try {
                            oldListener = blockingAsyncQueue.remove(renderingData.getComponent());
                        } finally {
                            mainLock.unlock();
                        }

                        if (oldListener != null) {
                            oldListener.stop();
                        }

                        mainLock.lock();
                        try {
                            // We need to put a dummy DataListener into the blocking queue
                            // so "checkDone" will not believe that the rendering is
                            // done in case a rendering finishes fast without
                            // rescheduling.
                            blockingAsyncQueue.put(renderingData.getComponent(), new DataListener(newData));
                        } finally {
                            mainLock.unlock();
                        }

                        insertIntoRendererQueue(renderingData, true);

                        DataListener dataListener = new DataListener(newData);
                        CancellationSource cancelSource = Cancellation.createCancellationSource();

                        AsyncDataController dataController;
                        dataController = renderingParams.getAsyncBlockingData(
                                cancelSource.getToken(), dataListener);

                        dataListener.setController(dataController, cancelSource.getController());

                        mainLock.lock();
                        try {
                            // Notice that this is the only place where
                            // a new listener is registered so
                            // there is no listener registered at this point
                            // (except for the previously registered dummy component)
                            // for the given component.
                            if (!dataListener.isFinished()) {
                                blockingAsyncQueue.put(renderingData.getComponent(), dataListener);
                            }
                            else {
                                // remove the dummy listener
                                blockingAsyncQueue.remove(renderingData.getComponent());
                            }
                        } finally {
                            mainLock.unlock();
                        }
                    }
                    else {
                        DataListener oldListener;

                        mainLock.lock();
                        try {
                            oldListener = blockingAsyncQueue.remove(renderingData.getComponent());
                        } finally {
                            mainLock.unlock();
                        }

                        if (oldListener != null) {
                            oldListener.stop();
                        }

                        insertIntoRendererQueue(renderingData, true);
                    }
                }
            } while (newData != null);
        }
    }

    private abstract class RenderTask implements Runnable {
        public RenderTask() {
        }

        protected void renderIfRequired(RenderingData renderingData) {
            if (renderingData == null) {
                // There was nothing to render so sleeping a little won't hurt.
                try {
                    Thread.sleep(BLOCKED_RENDERING_SLEEPTIME);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            else {
                if (!renderingData.getPrerenderingData().isCancelled()) {
                    render(renderingData);
                }
            }
        }

        protected void startRenderingUnlocked(RenderingData renderingData) {
            Thread.interrupted();

            Component renderedComponent = renderingData.getComponent();
            if (renderedComponent == null) return;

            RunningRenderingDescriptor newRenderingDescr =
                    new RunningRenderingDescriptor(Thread.currentThread(),  renderingData.getPrerenderingData());

            RunningRenderingDescriptor oldDescr = currentlyRendering.put(renderedComponent, newRenderingDescr);
            if (oldDescr != null) {
                // This is an internal error in this renderer.
                currentlyRendering.put(renderedComponent, oldDescr);
                throw new RuntimeException("Trying to render the same component in parallel.");
            }
        }

        protected void stopRendering(Component renderedComponent) {
            if (renderedComponent != null) {
                mainLock.lock();
                try {
                    currentlyRendering.remove(renderedComponent);
                } finally {
                    mainLock.unlock();
                }
            }

            Thread.interrupted();
        }

        public void render(RenderingData renderingData) {
            if (renderingData == null) return;

            boolean renderSuccess = false;
            final Component renderedComponent = renderingData.getComponent();
            if (renderedComponent == null) {
                return;
            }

            ComponentRenderer renderer = renderingData.getRenderer();

            Object userDefParams = renderingData.getUserDefinedParams();
            RenderingResult renderingResult = null;
            BufferedImage drawingSurface = getPaintBuffer(renderingData);

            try {
                ControlledBlockedData blockedData = renderingData.getBlockedData();
                if (drawingSurface != null) {
                    renderingResult = renderer.renderComponent(
                            userDefParams,
                            blockedData != null ? blockedData.getRawData() : null,
                            drawingSurface);
                    renderSuccess = true;
                }

            } finally {
                if (renderingResult == null) {
                    renderingData.getDrawingConnector().presentNewImage(drawingSurface, null);
                }
                else if (renderSuccess && renderingResult.needPaint()) {
                    renderingData.getDrawingConnector().presentNewImage(drawingSurface, renderingResult.getPaintResult());
                }
                else if (drawingSurface != null) {
                    renderingData.getDrawingConnector().offerBuffer(drawingSurface);
                }
            }

            if (renderingResult == null) {
                renderingResult = RenderingResult.done(true);
            }

            if (renderingResult.isRenderingFinished()) {
                PrerenderingData prerenderingData = renderingData.getPrerenderingData();

                try {
                    stopBlockingData(prerenderingData);
                    removeFromRendererQueue(prerenderingData);
                } finally {
                    checkDone(prerenderingData, false);
                }
            }
            else {
                checkDone(renderingData.getPrerenderingData(), false);
            }

            if (renderSuccess && renderingResult.needPaint()) {
                renderingData.getPrerenderingData().setHasPainted();
                renderedComponent.repaint();
            }
        }
    }

    private class PriorityRenderTask extends RenderTask {
        public PriorityRenderTask() {
        }

        @Override
        public void run() {
            RenderingData renderingData = null;

            mainLock.lock();
            try {
                int index = 0;
                int maxIndex = -1;
                int maxPriority = Integer.MIN_VALUE;

                for (RenderingData currentRenderingData: rendererQueue) {
                    if (currentRenderingData != null) {
                        if (!currentlyRendering.containsKey(currentRenderingData.getComponent())) {
                            int pri = currentRenderingData.getPriority();
                            if (pri > maxPriority || maxIndex < 0) {
                                maxIndex = index;
                                maxPriority = pri;
                            }
                        }
                    }

                    index++;
                }

                if (maxIndex >= 0) {
                    renderingData = rendererQueue.remove(maxIndex);
                }

                if (renderingData != null) {
                    // Notice that stopRenderingUnlocked will be called
                    // if mainLock.unlock() does not throw an exception.
                    startRenderingUnlocked(renderingData);
                }
            } finally {
                mainLock.unlock();
            }

            try {
                renderIfRequired(renderingData);
            } finally {
                if (renderingData != null) {
                    stopRendering(renderingData.getComponent());
                }
            }
        }
    }

    private class FifoRenderTask extends RenderTask {
        public FifoRenderTask() {
        }

        @Override
        public void run() {
            RenderingData renderingData = null;
            mainLock.lock();
            try {
                LinkedList<RenderingData> preQueue = new LinkedList<>();

                int queueSize = rendererQueue.size();
                for (int i = 0; i < queueSize; i++) {
                    RenderingData currentRenderingData = rendererQueue.poll();
                    if (currentRenderingData == null) continue;

                    if (!currentlyRendering.containsKey(currentRenderingData.getComponent())) {
                        renderingData = currentRenderingData;
                        break;
                    }

                    preQueue.addLast(currentRenderingData);
                }

                // Under normal cicumstances this list is empty.
                // It may only contains elements if a component was
                // already rendering.
                Iterator<RenderingData> backItr = preQueue.descendingIterator();
                while (backItr.hasNext()) {
                    rendererQueue.addFirst(backItr.next());
                }

                if (renderingData != null) {
                    // Notice that stopRenderingUnlocked will be called
                    // if queueLock.unlock() does not throw an exception.
                    startRenderingUnlocked(renderingData);
                }
            } finally {
                mainLock.unlock();
            }

            try {
                renderIfRequired(renderingData);
            } finally {
                if (renderingData != null) {
                    stopRendering(renderingData.getComponent());
                }
            }
        }
    }

    private static class ControlledBlockedData {
        private final Object rawData;
        private final CancellationController controller;
        private boolean disposed;

        public ControlledBlockedData(Object rawData, CancellationController controller) {
            this.rawData = rawData;
            this.controller = controller;
            this.disposed = false;
        }

        public CancellationController getController() {
            return controller;
        }

        public Object getRawData() {
            if (disposed) {
                throw new IllegalStateException("Requesting the blocked data after dispose.");
            }

            return rawData;
        }
    }

    private static class RenderingData {
        private final PrerenderingData prerenderingData;
        private final ControlledBlockedData blockedData;

        public RenderingData(PrerenderingData prerenderingData, ControlledBlockedData blockedData) {
            this.prerenderingData = prerenderingData;
            this.blockedData = blockedData;
        }

        public RenderingData cloneReplaceBlockedData(ControlledBlockedData newBlockedData) {
            return new RenderingData(prerenderingData, newBlockedData);
        }

        public ControlledBlockedData getBlockedData() {
            return blockedData;
        }

        public PrerenderingData getPrerenderingData() {
            return prerenderingData;
        }

        public RenderingParameters getRenderingParams() {
            return prerenderingData.getRenderingParams();
        }

        public Object getUserDefinedParams() {
            return prerenderingData.getUserDefinedParams();
        }

        public boolean hasBlockingData() {
            return prerenderingData.hasBlockingData();
        }


        public ComponentRenderer getRenderer() {
            return prerenderingData.getRenderer();
        }

        public int getPriority() {
            return prerenderingData.getPriority();
        }

        public DrawingConnector getDrawingConnector() {
            return prerenderingData.getDrawingConnector();
        }

        public Component getComponent() {
            return prerenderingData.getComponent();
        }
    }

    private static class PrerenderingData {
        private final int priority;
        private final WeakReference<Component> componentRef;
        private final ComponentRenderer renderer;
        private final RenderingParameters renderingParams;
        private final DrawingConnector drawingConnector;
        private DefaultRenderingFuture renderingFuture;
        private boolean cancelled;

        public PrerenderingData(int priority, Component component,
                ComponentRenderer renderer, RenderingParameters renderingParams,
                DrawingConnector drawingConnector) {
            this.priority = priority;
            this.componentRef = new WeakReference<>(component);
            this.renderer = renderer;
            this.renderingParams = renderingParams;
            this.drawingConnector = drawingConnector;
            this.renderingFuture = null;
            this.cancelled = false;
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setHasPainted() {
            if (renderingFuture != null) {
                renderingFuture.setHasPainted();
            }
        }

        public void setDone() {
            if (renderingFuture != null) {
                renderingFuture.setDone();
            }
        }

        public DefaultRenderingFuture getRenderingFuture() {
            return renderingFuture;
        }

        public void setRenderingFuture(DefaultRenderingFuture renderingFuture) {
            this.renderingFuture = renderingFuture;
        }

        public Component getComponent() {
            return componentRef.get();
        }

        public ComponentRenderer getRenderer() {
            return renderer;
        }

        public DrawingConnector getDrawingConnector() {
            return drawingConnector;
        }

        public int getPriority() {
            return priority;
        }

        public RenderingParameters getRenderingParams() {
            return renderingParams;
        }

        public boolean hasBlockingData() {
            if (renderingParams != null) {
                return renderingParams.hasBlockingData();
            }
            else {
                return false;
            }
        }

        public Object getUserDefinedParams() {
            if (renderingParams != null) {
                return renderingParams.getUserDefinedParams();
            }
            else {
                return null;
            }
        }
    }

    private static class RunningRenderingDescriptor {
        private final Thread runningThread;
        private final PrerenderingData prerenderingData;

        public RunningRenderingDescriptor(Thread runningThread, PrerenderingData prerenderingData) {
            this.runningThread = runningThread;
            this.prerenderingData = prerenderingData;
        }

        public PrerenderingData getPrerenderingData() {
            return prerenderingData;
        }

        public Thread getRunningThread() {
            return runningThread;
        }
    }

    private class DefaultRenderingFuture implements RenderingFuture {
        private final PrerenderingData renderingData;
        private final long startTime;
        private volatile boolean done;
        private volatile boolean painted;

        public DefaultRenderingFuture(PrerenderingData renderingData) {
            this.renderingData = renderingData;
            this.startTime = System.nanoTime();
            this.done = false;
            this.painted = false;
        }

        public void setDone() {
            done = true;
        }

        public void setHasPainted() {
            painted = true;
        }

        public PrerenderingData getRenderingData() {
            return renderingData;
        }

        @Override
        public boolean isRenderingDone() {
            return done;
        }

        @Override
        public AsyncDataState getAsyncDataState() {
            DataListener listener;

            mainLock.lock();
            try {
                listener = blockingAsyncQueue.get(renderingData.getComponent());
            } finally {
                mainLock.unlock();
            }

            return listener != null ? listener.getDataState() : null;
        }

        @Override
        public long getRenderingTime() {
            return System.nanoTime() - startTime;
        }

        @Override
        public void cancel() {
            renderingData.cancel();
            try {
                stopBlockingData(renderingData);
                removeFromRendererQueue(renderingData);
            } finally {
                checkDone(renderingData, true);
            }

            mainLock.lock();
            try {
                RunningRenderingDescriptor runningDescr = currentlyRendering.get(renderingData.getComponent());
                if (runningDescr != null && runningDescr.getPrerenderingData() == renderingData) {
                    runningDescr.getRunningThread().interrupt();
                }
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public boolean hasPainted() {
            return painted;
        }
    }
}
