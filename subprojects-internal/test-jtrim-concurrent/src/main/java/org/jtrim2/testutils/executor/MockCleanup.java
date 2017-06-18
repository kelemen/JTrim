package org.jtrim2.testutils.executor;

import java.util.function.BiConsumer;

public interface MockCleanup {
    public void cleanup(Object result, Throwable error);

    public static <V> BiConsumer<V, Throwable> toCleanupTask(MockCleanup mockCleanup) {
        return (V result, Throwable error) -> {
            mockCleanup.cleanup(result, error);
        };
    }
}
