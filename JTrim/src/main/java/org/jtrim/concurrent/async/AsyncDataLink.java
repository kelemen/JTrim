/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public interface AsyncDataLink<DataType> {
    public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener);
}
