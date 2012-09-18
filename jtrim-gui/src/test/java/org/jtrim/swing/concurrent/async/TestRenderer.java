package org.jtrim.swing.concurrent.async;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.concurrent.async.AsyncReport;

/**
 *
 * @author Kelemen Attila
 */
public final class TestRenderer<DataType> implements DataRenderer<DataType> {
    private final DataRenderer<DataType> renderer;
    private final CountDownLatch startRenderingLatch;
    private final CountDownLatch renderLatch;
    private final CountDownLatch finishRenderingLatch;
    private final WaitableSignal doneSignal;
    private final long timeoutNanos;
    private final AtomicReference<String> callInProgress;
    private final AtomicInteger startCallCount;
    private final AtomicInteger finishCallCount;
    private final Queue<DataType> receivedDatas;
    private final Queue<String> miscErrors;
    private AsyncReport finalReport;

    public TestRenderer(long timeout, TimeUnit unit) {
        this(null, timeout, unit);
    }

    public TestRenderer(DataRenderer<DataType> renderer, long timeout, TimeUnit unit) {
        this.renderer = renderer;
        this.startRenderingLatch = new CountDownLatch(1);
        this.renderLatch = new CountDownLatch(1);
        this.finishRenderingLatch = new CountDownLatch(1);
        this.doneSignal = new WaitableSignal();
        this.timeoutNanos = unit.toNanos(timeout);
        this.startCallCount = new AtomicInteger(0);
        this.finishCallCount = new AtomicInteger(0);
        this.callInProgress = new AtomicReference<>(null);
        this.receivedDatas = new ConcurrentLinkedQueue<>();
        this.miscErrors = new ConcurrentLinkedQueue<>();
        this.finalReport = null;
    }

    public boolean isStartRenderingCalled() {
        return startCallCount.get() > 0;
    }

    public int getRenderCallCount() {
        return receivedDatas.size();
    }

    public boolean isFinishRenderingCalled() {
        return finishCallCount.get() > 0;
    }

    public List<DataType> getReceivedDatas() {
        return new LinkedList<>(receivedDatas);
    }

    public List<String> getMiscErrors() {
        return new LinkedList<>(miscErrors);
    }

    public void allowStartRendering() {
        startRenderingLatch.countDown();
    }

    public void allowRender() {
        renderLatch.countDown();
    }

    public void allowFinishRendering() {
        finishRenderingLatch.countDown();
    }

    public void allowAll() {
        allowStartRendering();
        allowRender();
        allowFinishRendering();
    }

    public AsyncReport awaitDone() {
        return awaitDone(Cancellation.UNCANCELABLE_TOKEN);
    }

    public AsyncReport awaitDone(CancellationToken cancelToken) {
        doneSignal.waitSignal(cancelToken, timeoutNanos, TimeUnit.NANOSECONDS);
        return finalReport;
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Thread.interrupted();
        }
    }

    private void stateError(String errorStr) {
        miscErrors.add(errorStr);
        throw new IllegalStateException(errorStr);
    }

    private void enterCall(String call) {
        String currentCall;
        currentCall = callInProgress.get();
        if (currentCall != null) {
            String errorStr = "Attempting to call " + call + " concurrently with " + currentCall;
            stateError(errorStr);
        }
    }

    private void leaveCall() {
        callInProgress.set(null);
    }

    @Override
    public boolean startRendering() {
        enterCall("startRendering");
        try {
            if (startCallCount.getAndIncrement() > 0) {
                stateError("startRendering has been called multiple times.");
            }
            if (doneSignal.isSignaled()) {
                stateError("startRendering has been called after finishRendering.");
            }
            awaitLatch(startRenderingLatch);
            if (renderer != null) {
                return renderer.startRendering();
            }
            else {
                return true;
            }
        } finally {
            leaveCall();
        }
    }

    @Override
    public boolean willDoSignificantRender(DataType data) {
        return renderer != null ? renderer.willDoSignificantRender(data) : false;
    }
    
    @Override
    public boolean render(DataType data) {
        enterCall("render");
        try {
            if (startCallCount.get() == 0) {
                stateError("render has been called without calling startRendering.");
            }
            if (doneSignal.isSignaled()) {
                stateError("render has been called after finishRendering.");
            }
            awaitLatch(renderLatch);
            receivedDatas.add(data);
            if (renderer != null) {
                return renderer.render(data);
            }
            else {
                return false;
            }
        } finally {
            leaveCall();
        }
    }

    @Override
    public void finishRendering(AsyncReport report) {
        enterCall("finishRendering");
        try {
            if (finishCallCount.getAndIncrement() > 0) {
                stateError("finishRendering has been called multiple times.");
            }
            if (startCallCount.get() == 0) {
                stateError("finishRendering has been called without calling startRendering.");
            }
            awaitLatch(finishRenderingLatch);
            finalReport = report;
            if (renderer != null) {
                renderer.finishRendering(report);
            }
        } finally {
            doneSignal.signal();
            leaveCall();
        }
    }

}
