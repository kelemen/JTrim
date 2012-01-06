/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent;

import java.util.concurrent.Executor;
import org.jtrim.concurrent.AbstractUpdateTaskExecutor;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingUpdateTaskExecutor extends AbstractUpdateTaskExecutor {

    private final Executor executor;

    public SwingUpdateTaskExecutor() {
        this(true);
    }

    public SwingUpdateTaskExecutor(boolean alwaysInvokeLater) {
        this.executor = SwingTaskExecutor.getSimpleExecutor(alwaysInvokeLater);
    }

    @Override
    protected void runTask(Runnable task) {
        executor.execute(task);
    }
}
