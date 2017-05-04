package org.jtrim2.concurrent.async;

/**
 * @see AsyncLinks#markResultWithUid(AsyncDataLink)
 * @see AsyncQueries#markResultsWithUid(AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
@StatelessClass
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
