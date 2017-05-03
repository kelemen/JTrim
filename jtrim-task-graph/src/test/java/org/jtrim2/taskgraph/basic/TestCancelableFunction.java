package org.jtrim2.taskgraph.basic;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.CancelableFunction;
import org.jtrim2.utils.ExceptionHelper;

public final class TestCancelableFunction<V> implements CancelableFunction<V> {
    private final TestRunnable runnable;
    private final V result;

    public TestCancelableFunction(Object key, V result) {
        this.runnable = new TestRunnable(key);
        this.result = result;
    }

    public void verifyNotCalled() {
        runnable.verifyNotCalled();
    }

    public void verifyCalled() {
        runnable.verifyCalled();
    }

    @Override
    public V execute(CancellationToken cancelToken) throws Exception {
        runnable.run();

        if (cancelToken.isCanceled()) {
            throw new OperationCanceledException();
        }

        if (result instanceof Throwable) {
            throw ExceptionHelper.throwChecked((Throwable)result, Exception.class);
        }
        return result;
    }
}
