package org.jtrim.concurrent.async;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import org.jtrim.cancel.Cancellation;

/**
 *
 * @author Kelemen Attila
 */
public final class TestQueryHelper {
    private TestQueryHelper() {
    }

    public static <QueryArgType, DataType> DataType queryAndWaitResult(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input)
            throws InterruptedException {

        List<DataType> results = queryAndWaitResults(query, input);
        if (results.size() != 1) {
            throw new IllegalStateException("The number of returned datas is not one: "
                    + results.size());
        }

        return results.get(0);
    }

    public static <QueryArgType, DataType> DataType queryAndWaitLastResult(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input)
            throws InterruptedException {

        List<DataType> results = queryAndWaitResults(query, input);
        if (results.isEmpty()) {
            throw new IllegalStateException("There was no async data.");
        }

        return results.get(results.size() - 1);
    }

    public static <QueryArgType, DataType> List<DataType> queryAndWaitResults(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input)
            throws InterruptedException {

        final Deque<DataType> results = new LinkedBlockingDeque<>();
        final CountDownLatch endSignal = new CountDownLatch(1);

        query.createDataLink(input).getData(Cancellation.UNCANCELABLE_TOKEN,
                new AsyncDataListener<DataType>() {
            @Override
            public void onDataArrive(DataType data) {
                results.addLast(data);
            }

            @Override
            public void onDoneReceive(AsyncReport report) {
                endSignal.countDown();
            }
        });

        endSignal.await();
        return new LinkedList<>(results);
    }
}
