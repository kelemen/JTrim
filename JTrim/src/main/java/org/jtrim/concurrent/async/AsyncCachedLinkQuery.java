package org.jtrim.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncDatas#cacheResults(org.jtrim.concurrent.async.AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
final class AsyncCachedLinkQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<CachedDataRequest<QueryArgType>, DataType> {

    private final AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery;

    public AsyncCachedLinkQuery(AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery) {
        ExceptionHelper.checkNotNullArgument(wrappedQuery, "wrappedQuery");

        this.wrappedQuery = wrappedQuery;
    }

    public AsyncDataQuery<? super QueryArgType, ? extends DataType> getWrappedQuery() {
        return wrappedQuery;
    }

    @Override
    public AsyncDataLink<DataType> createDataLink(CachedDataRequest<QueryArgType> arg) {
        return AsyncDatas.cacheResult(
                wrappedQuery.createDataLink(arg.getQueryArg()),
                arg.getRefType(), arg.getObjectCache(),
                arg.getDataCancelTimeout(TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Use ");
        AsyncFormatHelper.appendIndented(wrappedQuery, result);
        result.append("\nCache results");

        return result.toString();
    }
}
