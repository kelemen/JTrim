/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class IdempotentTask implements Runnable {
    private final AtomicReference<Runnable> task;

    public IdempotentTask(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        this.task = new AtomicReference<>(task);
    }

    @Override
    public void run() {
        Runnable currentTask = task.getAndSet(null);
        if (currentTask != null) {
            currentTask.run();
        }
    }
}
