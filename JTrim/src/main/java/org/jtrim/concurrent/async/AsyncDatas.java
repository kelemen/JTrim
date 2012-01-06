/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.*;
import java.util.concurrent.*;
import org.jtrim.cache.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class AsyncDatas {
    private static final int DEFAULT_CACHE_TIMEOUT = 1 * 1000; // ms
    private static final int DEFAULT_CACHE_SIZE = 128;

    private AsyncDatas() {
        throw new AssertionError();
    }

    // Exceptions

    private static DataTransferException toTransferException(Throwable exception) {
        ExceptionHelper.checkNotNullArgument(exception, "exception");

        if (exception instanceof DataTransferException) {
            return (DataTransferException)exception;
        }
        else {
            return new DataTransferException(exception);
        }
    }

    public static DataTransferException getTransferException(Throwable... exceptions) {
        DataTransferException result;

        switch (exceptions.length) {
            case 0:
                result = null;
                break;
            case 1:
                result = toTransferException(exceptions[0]);
                break;
            default:
                result = new DataTransferException();
                ExceptionHelper.checkNotNullElements(exceptions, "exceptions");
                for (Throwable exception: exceptions) {
                    result.addSuppressed(exception);
                }
                break;
        }

        return result;
    }

    // Listener builder methods

    public static <DataType>
            AsyncDataListener<OrderedData<DataType>> makeSafeOrderedListener(
            AsyncDataListener<? super DataType> outputListener) {

        return new SafeDataListener<>(outputListener);
    }

    public static <DataType>
            AsyncDataListener<DataType> makeSafeListener(
            AsyncDataListener<? super DataType> outputListener) {

        return new DataOrdererListener<>(
                makeSafeOrderedListener(outputListener));
    }

    // Link builder methods

    public static <OldType, NewType> AsyncDataLink<NewType> convertResult(
            AsyncDataLink<? extends OldType> link,
            DataConverter<? super OldType, ? extends NewType> converter) {

        return new AsyncDataLinkConverter<>(link, converter);
    }

    public static <OldType, NewType> AsyncDataLink<NewType> convertResult(
            AsyncDataLink<? extends OldType> input,
            AsyncDataQuery<? super OldType, ? extends NewType> converter) {

        return new LinkedAsyncDataLink<>(input, converter);
    }

    public static <DataType> AsyncDataLink<DataType> extractCachedResult(
            AsyncDataLink<RefCachedData<DataType>> link) {

        return convertResult(link, new CachedDataExtractor<DataType>());
    }

    public static <DataType> AsyncDataLink<DataType> removeUidFromResult(
            AsyncDataLink<DataWithUid<DataType>> link) {

        return convertResult(link, new DataIDRemover<DataType>());
    }

    public static <DataType> AsyncDataLink<DataWithUid<DataType>> markResultWithUid(
            AsyncDataLink<? extends DataType> link) {

        return convertResult(link, new MarkWithIDConverter<DataType>());
    }

    public static <DataType> AsyncDataLink<DataType> cacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator) {

        return cacheResult(
                wrappedDataLink,
                refType, refCreator,
                DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <DataType> AsyncDataLink<DataType> cacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeUnit
            ) {

        return extractCachedResult(
                refCacheResult(wrappedDataLink, refType, refCreator,
                    dataCancelTimeout, timeUnit));
    }

    public static <DataType> AsyncDataLink<RefCachedData<DataType>> refCacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator
            ) {

        return refCacheResult(
                wrappedDataLink,
                refType, refCreator,
                DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <DataType> AsyncDataLink<RefCachedData<DataType>> refCacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeUnit
            ) {

        return new RefCachedDataLink<>(
                wrappedDataLink,
                refType, refCreator,
                dataCancelTimeout, timeUnit);
    }

    public static <InputType, ResultType> AsyncDataLink<ResultType> convertGradually(
            InputType input,
            List<? extends AsyncDataConverter<InputType, ResultType>> transformers) {

        return new ImproverTasksLink<>(input, transformers);
    }

    public static <DataType> AsyncDataLink<DataType> createStateReporterLink(
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        return createStateReporterLink(null, wrappedLink, reporter,
                period, periodUnit);
    }

    public static <DataType> AsyncDataLink<DataType> createStateReporterLink(
            UpdateTaskExecutor reportExecutor,
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        return new PeriodicStateReporterLink<>(reportExecutor, wrappedLink,
                reporter, period, periodUnit);
    }

    public static <DataType> AsyncDataLink<DataType> createPreparedLink(
            DataType data, AsyncDataState state) {

        return new PreparedDataLink<>(data, state);
    }

    // Query builder methods

    public static <QueryArgType, DataType>
            AsyncDataQuery<CachedDataRequest<QueryArgType>, DataType> cacheResults(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery) {

        return new AsyncCachedLinkQuery<>(wrappedQuery);
    }

    public static <QueryArgType, DataType>
            CachedAsyncDataQuery<QueryArgType, DataType> cacheLinks(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery) {

        return cacheLinks(wrappedQuery, DEFAULT_CACHE_SIZE);
    }

    public static <QueryArgType, DataType>
            CachedAsyncDataQuery<QueryArgType, DataType> cacheLinks(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery,
            int maxCacheSize) {

        return new CachedAsyncDataQuery<>(wrappedQuery, maxCacheSize);
    }

    public static <QueryArgType, DataType>
            CachedByIDAsyncDataQuery<QueryArgType, DataType> cacheByID(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator) {

        return cacheByID(wrappedQuery, refType, refCreator, DEFAULT_CACHE_SIZE);
    }

    public static <QueryArgType, DataType>
            CachedByIDAsyncDataQuery<QueryArgType, DataType> cacheByID(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator,
            int maxCacheSize) {

        return new CachedByIDAsyncDataQuery<>(wrappedQuery,
                refType, refCreator, maxCacheSize);
    }

    public static <OldDataType, QueryArgType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResults(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        return new AsyncDataQueryConverter<>(wrappedQuery, converter);
    }

    public static <QueryArgType, SecArgType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResults(
            AsyncDataQuery<? super QueryArgType, ? extends SecArgType> wrappedQuery,
            AsyncDataQuery<? super SecArgType, ? extends NewDataType> converter) {

        return new LinkedAsyncDataQuery<>(wrappedQuery, converter);
    }

    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> extractCachedResults(
            AsyncDataQuery<? super QueryArgType, RefCachedData<DataType>> query) {

        return convertResults(query, new CachedDataExtractor<DataType>());
    }

    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> removeUidFromResults(
            AsyncDataQuery<? super QueryArgType, DataWithUid<DataType>> query) {

        return convertResults(query, new DataIDRemover<DataType>());
    }

    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataWithUid<DataType>> markResultsWithUid(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> query) {

        return convertResults(query, new MarkWithIDConverter<DataType>());
    }
}
