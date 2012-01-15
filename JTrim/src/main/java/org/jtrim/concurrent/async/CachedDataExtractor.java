package org.jtrim.concurrent.async;

/**
 * @see AsyncDatas#extractCachedResult(org.jtrim.concurrent.async.AsyncDataLink)
 * @see AsyncDatas#extractCachedResults(org.jtrim.concurrent.async.AsyncDataQuery)
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
