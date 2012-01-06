/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

/**
 *
 * @author Kelemen Attila
 */
public interface UpdateTaskExecutor {
    public void execute(Runnable task);
    public void shutdown();
}
