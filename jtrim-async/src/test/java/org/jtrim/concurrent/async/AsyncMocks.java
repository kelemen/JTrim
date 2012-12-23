package org.jtrim.concurrent.async;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public final class AsyncMocks {
    @SuppressWarnings("unchecked")
    public static <QueryArgType, DataType> AsyncDataQuery<QueryArgType, DataType> mockQuery() {
        return mock(AsyncDataQuery.class);
    }

    @SuppressWarnings("unchecked")
    public static <DataType> AsyncDataLink<DataType> mockLink() {
        return mock(AsyncDataLink.class);
    }

    @SuppressWarnings("unchecked")
    public static <DataType> AsyncDataListener<DataType> mockListener() {
        return mock(AsyncDataListener.class);
    }

    private AsyncMocks() {
        throw new AssertionError();
    }
}
