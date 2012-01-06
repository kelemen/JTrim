/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
final class CachedDataExtractor<DataType>
implements
        DataConverter<RefCachedData<? extends DataType>, DataType> {

    @Override
    public DataType convertData(RefCachedData<? extends DataType> data) {
        return data.getData();
    }

    @Override
    public String toString() {
        return "Extract Data";
    }
}
