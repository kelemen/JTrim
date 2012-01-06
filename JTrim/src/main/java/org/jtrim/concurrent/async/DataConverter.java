/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public interface DataConverter<OldDataType, NewDataType> {
    public NewDataType convertData(OldDataType data);
}
