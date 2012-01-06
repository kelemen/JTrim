/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

/**
 *
 * @author Kelemen Attila
 */
public enum SilentTaskRefusePolicy implements TaskRefusePolicy {
    INSTANCE;

    @Override
    public void refuseTask(Runnable task) {
    }
}
