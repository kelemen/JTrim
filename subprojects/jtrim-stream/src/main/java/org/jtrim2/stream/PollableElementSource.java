package org.jtrim2.stream;

import org.jtrim2.cancel.CancellationToken;

interface PollableElementSource<T> {
    public T getNext(CancellationToken cancelToken) throws Exception;
    public void finish(Throwable error);
}
