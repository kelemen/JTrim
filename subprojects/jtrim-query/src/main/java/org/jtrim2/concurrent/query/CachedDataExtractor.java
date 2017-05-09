package org.jtrim2.concurrent.query;

/**
 * @see AsyncLinks#extractCachedResult(AsyncDataLink)
 * @see AsyncQueries#extractCachedResults(AsyncDataQuery)
 */
@StatelessClass
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
