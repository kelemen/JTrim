package org.jtrim2.concurrent.query;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see AsyncLinks#interceptData(AsyncDataLink, DataInterceptor)
 */
final class DataInterceptorLink<DataType>
        implements AsyncDataLink<DataType> {

    private final AsyncDataLink<? extends DataType> wrappedLink;
    private final DataInterceptor<? super DataType> interceptor;

    public DataInterceptorLink(
            AsyncDataLink<? extends DataType> wrappedLink,
            DataInterceptor<? super DataType> interceptor) {

        Objects.requireNonNull(wrappedLink, "wrappedLink");
        Objects.requireNonNull(interceptor, "interceptor");

        this.wrappedLink = wrappedLink;
        this.interceptor = interceptor;
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            final AsyncDataListener<? super DataType> dataListener) {
        Objects.requireNonNull(dataListener, "dataListener");

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
