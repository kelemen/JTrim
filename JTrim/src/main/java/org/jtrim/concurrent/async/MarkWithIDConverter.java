/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
final class MarkWithIDConverter<DataType>
implements
        DataConverter<DataType, DataWithUid<DataType>> {

    @Override
    public DataWithUid<DataType> convertData(DataType data) {
        return new DataWithUid<>(data, new Object());
    }

    @Override
    public String toString() {
        return "Mark with UniqueID";
    }
}
