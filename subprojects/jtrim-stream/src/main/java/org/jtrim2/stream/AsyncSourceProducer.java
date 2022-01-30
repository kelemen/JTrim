package org.jtrim2.stream;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.utils.ExceptionHelper;

final class AsyncSourceProducer<T> implements SeqProducer<T> {
    private final PollableElementSource<? extends T> source;

    public AsyncSourceProducer(PollableElementSource<? extends T> source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public void transferAll(CancellationToken cancelToken, ElementConsumer<? super T> consumer) throws Exception {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(consumer, "consumer");

        Throwable transferFailure = null;
        try {
            T element;
            while ((element = source.getNext(cancelToken)) != null) {
                consumer.processElement(element);
            }
        } catch (Throwable ex) {
            transferFailure = ex;
        }
        source.finish(transferFailure);
        ExceptionHelper.rethrowCheckedIfNotNull(transferFailure, Exception.class);
    }
}
