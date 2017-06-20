package org.jtrim2.concurrent.query;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jtrim2.cancel.CancellationToken;

public final class ManualDataLink<DataType>
implements
        AsyncDataLink<DataType>, AsyncDataListener<DataType> {

    private final Queue<AsyncDataListener<? super DataType>> listeners;
    private volatile CancellationToken lastCancelToken;
    private final Queue<Object> receivedControlArgs;
    private final AsyncDataState dataState;

    public ManualDataLink() {
        this(null);
    }

    public ManualDataLink(AsyncDataState dataState) {
        this.listeners = new ConcurrentLinkedQueue<>();
        this.receivedControlArgs = new ConcurrentLinkedQueue<>();
        this.lastCancelToken = null;
        this.dataState = dataState;
    }

    public boolean hasLastRequestBeenCanceled() {
        CancellationToken cancelToken = lastCancelToken;
        return cancelToken != null ? cancelToken.isCanceled() : false;
    }

    public List<Object> getReceivedControlArgs() {
        return new ArrayList<>(receivedControlArgs);
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super DataType> dataListener) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(dataListener, "dataListener");
        lastCancelToken = cancelToken;
        listeners.add(dataListener);
        return new AsyncDataController() {
            @Override
            public void controlData(Object controlArg) {
                receivedControlArgs.add(controlArg);
            }

            @Override
            public AsyncDataState getDataState() {
                return dataState;
            }
        };
    }

    @Override
    public void onDataArrive(DataType data) {
        for (AsyncDataListener<? super DataType> listener : listeners) {
            listener.onDataArrive(data);
        }
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        List<AsyncDataListener<? super DataType>> doneListeners = new LinkedList<>();
        while (true) {
            AsyncDataListener<? super DataType> listener = listeners.poll();
            if (listener != null) {
                doneListeners.add(listener);
            } else {
                break;
            }
        }
        for (AsyncDataListener<? super DataType> listener : doneListeners) {
            listener.onDoneReceive(report);
        }
    }
}
