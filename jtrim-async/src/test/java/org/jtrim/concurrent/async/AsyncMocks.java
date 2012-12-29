package org.jtrim.concurrent.async;

import org.jtrim.cancel.CancellationToken;

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

    @SuppressWarnings("unchecked")
    public static void stubController(AsyncDataLink<?> mockLink, AsyncDataController controller) {
        stub(mockLink.getData(any(CancellationToken.class), any(AsyncDataListener.class)))
                .toReturn(controller);
    }

    private AsyncMocks() {
        throw new AssertionError();
    }
}
