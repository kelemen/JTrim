package org.jtrim2.concurrent.query;

/**
 * @see AsyncLinks#removeUidFromResult(AsyncDataLink)
 * @see AsyncQueries#removeUidFromResults(AsyncDataQuery)
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
