package org.jtrim.concurrent.async;

/**
 * @see AsyncDatas#extractCachedResult(AsyncDataLink)
 * @see AsyncDatas#extractCachedResults(AsyncDataQuery)
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
