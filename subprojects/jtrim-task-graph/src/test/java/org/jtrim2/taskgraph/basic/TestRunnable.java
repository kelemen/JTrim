package org.jtrim2.taskgraph.basic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class TestRunnable implements Runnable {
    private final Object key;
    private final AtomicInteger callCount;
    private volatile Throwable lastCall;
    private final Consumer<Object> releaseCollector;

    public TestRunnable(Object key) {
        this(key, (a) -> { });
    }

    public TestRunnable(Object key, Consumer<Object> releaseCollector) {
        this.key = key;
        this.callCount = new AtomicInteger(0);
        this.releaseCollector = releaseCollector;
    }

    public void verifyNotCalled() {
        int currentCallCount = callCount.get();
        if (currentCallCount != 0) {
            throw new AssertionError("Task " + key
                    + " must not have been called but was called " + currentCallCount + " times", lastCall);
        }
    }

    public void verifyCalled() {
        int currentCallCount = callCount.get();
        if (currentCallCount != 1) {
            throw new AssertionError("Task " + key
                    + " must have been called exactly once but was called " + currentCallCount + " times",
                    lastCall);
        }
    }

    public boolean isCalled() {
        return callCount.get() > 0;
    }

    @Override
    public void run() {
        lastCall = new Exception();
        callCount.incrementAndGet();

        releaseCollector.accept(key);
    }
}
