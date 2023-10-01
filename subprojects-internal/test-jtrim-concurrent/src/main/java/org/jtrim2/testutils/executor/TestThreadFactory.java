package org.jtrim2.testutils.executor;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import org.jtrim2.executor.ExecutorsEx;

public final class TestThreadFactory implements ThreadFactory {
    private final ThreadFactory wrappedFactory;
    private final ThreadLocal<Boolean> inContext;

    public TestThreadFactory(String name) {
        this(new ExecutorsEx.NamedThreadFactory(false, name));
    }

    public TestThreadFactory(ThreadFactory wrappedFactory) {
        this.wrappedFactory = Objects.requireNonNull(wrappedFactory, "wrappedFactory");
        this.inContext = new ThreadLocal<>();
    }

    @Override
    public Thread newThread(Runnable task) {
        Objects.requireNonNull(task, "task");
        return wrappedFactory.newThread(() -> {
            inContext.set(true);
            try {
                task.run();
            } finally {
                inContext.remove();
            }
        });
    }

    public boolean isExecutingInThis() {
        Boolean value = inContext.get();
        if (value == null) {
            inContext.remove();
            return false;
        } else {
            return true;
        }
    }
}
