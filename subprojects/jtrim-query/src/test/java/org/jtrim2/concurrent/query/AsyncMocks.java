package org.jtrim2.concurrent.query;

import org.jtrim2.cancel.CancellationToken;

import static org.mockito.Mockito.*;

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

    @SuppressWarnings("unchecked")
    public static void stubController(AsyncDataLink<?> mockLink, AsyncDataController controller) {
        when(mockLink.getData(any(CancellationToken.class), any(AsyncDataListener.class)))
                .thenReturn(controller);
    }

    private AsyncMocks() {
        throw new AssertionError();
    }
}
