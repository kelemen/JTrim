package org.jtrim2.concurrent.async;

/**
 * @see AsyncLinks#removeUidFromResult(AsyncDataLink)
 * @see AsyncQueries#removeUidFromResults(AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
@StatelessClass
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
