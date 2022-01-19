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

    @Test(timeout = 10000)
    public final void testNotInContextOfDifferentExecutor() throws Exception {
        testAll(factory -> {
            factory.runTest(executor1 -> {
                factory.runTest(executor2 -> {
                    testNotInContextOfDifferentExecutor(executor1, executor2);
                    return null;
                });
                return null;
            });
        });
    }

    private void testNotInContextOfDifferentExecutor(
            ContextAwareTaskExecutor executor1,
            ContextAwareTaskExecutor executor2) {

        if (executor1 == executor2) {
            return;
        }

        assertFalse("ExecutingInThis1", executor1.isExecutingInThis());
        assertFalse("ExecutingInThis2", executor2.isExecutingInThis());

        final WaitableSignal taskSignal = new WaitableSignal();
        final AtomicBoolean inContext = new AtomicBoolean(true);

        executor1.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContext.set(executor2.isExecutingInThis());
            taskSignal.signal();
        });

        taskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        assertFalse("ExecutingInThis", inContext.get());
    }
}
