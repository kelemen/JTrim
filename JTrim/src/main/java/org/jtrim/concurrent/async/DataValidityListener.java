/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public interface DataValidityListener<DataRefType> {
    public void onInvalidateData(DataRefType arg);
}
