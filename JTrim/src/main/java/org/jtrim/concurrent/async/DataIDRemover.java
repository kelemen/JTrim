package org.jtrim.concurrent.async;

/**
 * @see AsyncDatas#removeUidFromResult(org.jtrim.concurrent.async.AsyncDataLink)
 * @see AsyncDatas#removeUidFromResults(org.jtrim.concurrent.async.AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
final class DataIDRemover<DataType>
implements
        DataConverter<DataWithUid<? extends DataType>, DataType> {

    @Override
    public DataType convertData(DataWithUid<? extends DataType> data) {
        return data.getData();
    }

    @Override
    public String toString() {
        return "Remove UniqueID";
    }
}
