package org.jtrim2.testutils.executor;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.utils.ExceptionHelper;
import org.mockito.Matchers;
import org.mockito.Mockito;

public interface MockFunction<V> {
    public V execute(boolean canceled) throws Exception;

    public static <V> MockFunction<V> mock(V result) {
        @SuppressWarnings("unchecked")
        MockFunction<V> function = Mockito.mock(MockFunction.class);
        try {
            Mockito.doReturn(result)
                    .when(function)
                    .execute(Matchers.anyBoolean());
        } catch (Exception ex) {
            throw ExceptionHelper.throwUnchecked(ex);
        }
        return function;
    }

    public static <V> CancelableFunction<V> toFunction(MockFunction<V> mockFunction) {
        return (CancellationToken cancelToken) -> {
            return mockFunction.execute(cancelToken.isCanceled());
        };
    }
}
