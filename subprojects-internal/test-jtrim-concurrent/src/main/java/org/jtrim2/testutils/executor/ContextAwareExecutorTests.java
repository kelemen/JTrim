package org.jtrim2.testutils.executor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class ContextAwareExecutorTests<E extends ContextAwareTaskExecutor> extends AbstractExecutorTests<E> {
    public ContextAwareExecutorTests(Collection<? extends TestExecutorFactory<? extends E>> factories) {
        super(factories);
    }

    @Test(timeout = 10000)
    public final void testContextAwarenessInTask() throws Exception {
        testAllCreated(this::testContextAwarenessInTask);
    }

    private AfterTerminate testContextAwarenessInTask(ContextAwareTaskExecutor executor) throws Exception {
        assertFalse("ExecutingInThis", executor.isExecutingInThis());

        final WaitableSignal taskSignal = new WaitableSignal();
        final AtomicBoolean inContext = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContext.set(executor.isExecutingInThis());
            taskSignal.signal();
        });

        taskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue("ExecutingInThis", inContext.get());
        return null;
    }
}
