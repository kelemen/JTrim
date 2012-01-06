/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

/**
 *
 * @author Kelemen Attila
 */
public final class RunnableOnce implements Runnable {
    private final AtomicBoolean executed;
    private final Runnable wrappedRunnable;

    public RunnableOnce(Runnable wrappedRunnable) {
        this.wrappedRunnable = wrappedRunnable;
        this.executed = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        if (wrappedRunnable == null) return;

        if (!executed.getAndSet(true)) {
            wrappedRunnable.run();
        }
    }
}
