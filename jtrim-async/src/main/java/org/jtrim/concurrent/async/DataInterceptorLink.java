package org.jtrim.concurrent.async;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncLinks#interceptData(AsyncDataLink, DataInterceptor)
 *
 * @author Kelemen Attila
 */
final class DataInterceptorLink<DataType>
        implements AsyncDataLink<DataType> {

    private final AsyncDataLink<? extends DataType> wrappedLink;
    private final DataInterceptor<? super DataType> interceptor;

    public DataInterceptorLink(
            AsyncDataLink<? extends DataType> wrappedLink,
            DataInterceptor<? super DataType> interceptor) {

        ExceptionHelper.checkNotNullArgument(wrappedLink, "wrappedLink");
        ExceptionHelper.checkNotNullArgument(interceptor, "interceptor");

        this.wrappedLink = wrappedLink;
        this.interceptor = interceptor;
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            final AsyncDataListener<? super DataType> dataListener) {
        ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

        return wrappedLink.getData(cancelToken,
                new InterceptorListener<>(dataListener, interceptor));
    }

    private static class InterceptorListener<DataType>
    implements
            AsyncDataListener<DataType> {

        private final AsyncDataListener<? super DataType> dataListener;
        private final DataInterceptor<? super DataType> interceptor;

        public InterceptorListener(
                AsyncDataListener<? super DataType> dataListener,
                DataInterceptor<? super DataType> interceptor) {
            this.dataListener = dataListener;
            this.interceptor = interceptor;
        }

        @Override
        public void onDataArrive(DataType newData) {
            if (interceptor.onDataArrive(newData)) {
                dataListener.onDataArrive(newData);
            }
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            try {
                interceptor.onDoneReceive(report);
            } finally {
                dataListener.onDoneReceive(report);
            }
        }
    }
}
