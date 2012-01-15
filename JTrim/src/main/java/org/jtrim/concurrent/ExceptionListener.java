/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

/**
 *
 * @author Kelemen Attila
 */
public interface ExceptionListener<T> {
    public void onException(Throwable error, T arg);
}
