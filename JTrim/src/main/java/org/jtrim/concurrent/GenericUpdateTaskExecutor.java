/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.concurrent.Executor;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class GenericUpdateTaskExecutor extends AbstractUpdateTaskExecutor {

    private final Executor executor;

    public GenericUpdateTaskExecutor(Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
    }

    @Override
    protected void runTask(Runnable task) {
        executor.execute(task);
    }

    @Override
    public String toString() {
        return "GenericUpdateTaskExecutor{" + executor + '}';
    }
}
