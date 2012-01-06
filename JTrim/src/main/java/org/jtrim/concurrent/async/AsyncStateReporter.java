/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public interface AsyncStateReporter<DataType> {
    public void reportState(
            AsyncDataLink<DataType> dataLink,
            AsyncDataListener<? super DataType> dataListener,
            AsyncDataController controller);
}
