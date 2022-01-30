package org.jtrim2.stream;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ReservablePollingQueues;
import org.jtrim2.concurrent.collections.TerminableQueue;
import org.jtrim2.concurrent.collections.TerminableQueues;
import org.jtrim2.concurrent.collections.TerminatedQueueException;
import org.jtrim2.utils.ExceptionHelper;

final class DefaultAsyncElementSource<T> implements PollableElementSource<T>, AsyncElementSink<T> {
    private final TerminableQueue<T> queue;
    private final AtomicReference<Throwable> resultRef;

    public DefaultAsyncElementSource(int maxQueueSize, int initialQueueCapacity) {
        ExceptionHelper.checkArgumentInRange(maxQueueSize, 1, Integer.MAX_VALUE, "maxQueueSize");
        ExceptionHelper.checkArgumentInRange(initialQueueCapacity, 0, maxQueueSize, "initialQueueCapacity");

        this.queue = TerminableQueues.withWrappedQueue(
                ReservablePollingQueues.createFifoQueue(maxQueueSize, initialQueueCapacity)
        );
        this.resultRef = new AtomicReference<>(null);
    }

    private void failIfNeeded() throws Exception {
        ExceptionHelper.rethrowCheckedIfNotNull(resultRef.get(), Exception.class);
    }

    @Override
    public boolean tryPut(CancellationToken cancelToken, T element) throws Exception {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(element, "element");

        try {
            queue.put(cancelToken, element);
        } catch (TerminatedQueueException ex) {
            failIfNeeded();
            return false;
        }
        return true;
    }

    @Override
    public T getNext(CancellationToken cancelToken) throws Exception {
        try {
            return queue.take(cancelToken);
        } catch (TerminatedQueueException ex) {
            failIfNeeded();
            return null;
        }
    }

    @Override
    public void finish(Throwable error) {
        resultRef.compareAndSet(null, error);
        queue.shutdown();
        if (error != null) {
            queue.clear();
        }
    }
}
