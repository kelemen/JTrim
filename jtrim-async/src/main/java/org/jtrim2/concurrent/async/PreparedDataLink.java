package org.jtrim2.concurrent.async;

import org.jtrim2.cancel.CancellationToken;

/**
 * @see AsyncLinks#createPreparedLink(Object, AsyncDataState)
 *
 * @author Kelemen Attila
 */
final class PreparedDataLink<DataType> implements AsyncDataLink<DataType> {

    private final DataType data;
    private final AsyncDataController controller;

    public PreparedDataLink(DataType data, AsyncDataState currentState) {
        this(data, currentState, null);
    }

    public PreparedDataLink(DataType data, AsyncDataController controller) {
        this(data, null, controller);
    }

    private PreparedDataLink(DataType data,
            final AsyncDataState currentState,
            AsyncDataController controller) {
        this.data = data;

        if (controller != null) {
            this.controller = controller;
        }
        else {
            this.controller = new PreparedLinkController(currentState);
        }
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super DataType> dataListener) {

        try {
            dataListener.onDataArrive(data);
        } finally {
            dataListener.onDoneReceive(AsyncReport.SUCCESS);
        }

        return controller;
    }

    @Override
    public String toString() {
        return "Data = " + data;
    }

    private static class PreparedLinkController implements AsyncDataController {
        private final AsyncDataState currentState;

        public PreparedLinkController(AsyncDataState currentState) {
            this.currentState = currentState;
        }

        @Override
        public void controlData(Object controlArg) {
        }

        @Override
        public AsyncDataState getDataState() {
            return currentState;
        }
    }
}
