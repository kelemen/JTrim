package org.jtrim.concurrent.async;

/**
 * @see AsyncDatas#markResultWithUid(AsyncDataLink)
 * @see AsyncDatas#markResultsWithUid(AsyncDataQuery)
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
