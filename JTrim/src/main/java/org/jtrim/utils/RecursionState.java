/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Kelemen Attila
 */
public final class RecursionState {
    private final AtomicLong callCount;

    public RecursionState() {
        this.callCount = new AtomicLong(0);
    }

    public void enterCall() {
        callCount.incrementAndGet();
    }

    public void leaveCall() {
        if (callCount.decrementAndGet() < 0) {
            throw new IllegalStateException("There are too many leave calls.");
        }
    }

    public boolean isRecursive() {
        return getRecursionCount() > 1;
    }

    public boolean isCalled() {
        return getRecursionCount() > 0;
    }

    public long getRecursionCount() {
        return callCount.get();
    }
}
