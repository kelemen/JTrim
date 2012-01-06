/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.concurrent.RejectedExecutionException;

/**
 *
 * @author Kelemen Attila
 */
public enum AbortTaskRefusePolicy implements TaskRefusePolicy {
    INSTANCE;

    @Override
    public void refuseTask(Runnable task) {
        throw new RejectedExecutionException("The task cannot be executed in"
                + " the current state.");
    }

}
