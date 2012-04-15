package org.jtrim.concurrent.async;

/**
 * @see AsyncLinks#extractCachedResult(AsyncDataLink)
 * @see AsyncQueries#extractCachedResults(AsyncDataQuery)
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
