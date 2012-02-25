/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent;

import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingUpdateTaskExecutor implements UpdateTaskExecutor {
    private final UpdateTaskExecutor executor;

    public SwingUpdateTaskExecutor() {
        this(true);
    }

    public SwingUpdateTaskExecutor(boolean alwaysInvokeLater) {
        this.executor = new GenericUpdateTaskExecutor(
                SwingTaskExecutor.getSimpleExecutor(alwaysInvokeLater));
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
