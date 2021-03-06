package org.jtrim2.concurrent.query;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @see AsyncQueries#cacheResults(AsyncDataQuery)
 *
 */
final class AsyncCachedLinkQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<CachedDataRequest<QueryArgType>, DataType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery;

    public AsyncCachedLinkQuery(AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery) {
        Objects.requireNonNull(wrappedQuery, "wrappedQuery");

        this.wrappedQuery = wrappedQuery;
    }

    @Override
    public AsyncDataLink<DataType> createDataLink(CachedDataRequest<QueryArgType> arg) {
        return AsyncLinks.cacheResult(
                wrappedQuery.createDataLink(arg.getQueryArg()),
                arg.getRefType(), arg.getObjectCache(),
                arg.getDataCancelTimeout(TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Use ");
        AsyncFormatHelper.appendIndented(wrappedQuery, result);
        result.append("\nCache results");

        return result.toString();
    }
}
