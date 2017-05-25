package org.jtrim2.executor;

import java.util.Collection;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.executor.AfterTerminate;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.MockTask;
import org.jtrim2.testutils.executor.MockTaskResult;
import org.junit.Test;

import static org.jtrim2.testutils.executor.GenericExecutorTests.*;
import static org.mockito.Mockito.*;

public abstract class BackgroundExecutorTests extends GenericExecutorServiceTests {
    public BackgroundExecutorTests(Collection<Supplier<TaskExecutorService>> factories) {
        super(factories);
    }

    @Test//(timeout = 10000)
    public void testDoesntTerminateBeforeTaskCompletes2() throws Exception {
        testAllCreated(this::testDoesntTerminateBeforeTaskCompletes2);
    }

    private AfterTerminate testDoesntTerminateBeforeTaskCompletes2(
            TaskExecutorService executor) throws Exception {

        WaitableSignal mayRunTaskSignal = new WaitableSignal();

        MockTask task = mock(MockTask.class);
        MockTaskResult taskResult = MockTask.stubNonFailing(task, (canceled) -> {
            mayRunTaskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            Thread.sleep(50);
        });

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task));
        executor.shutdown();
        mayRunTaskSignal.signal();

        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        taskResult.verifySuccess();
        return null;
    }

    @Test(timeout = 10000)
    public void testInterruptDoesntBreakExecutor() throws Exception {
        testAllCreated(this::testInterruptDoesntBreakExecutor);
    }

    private AfterTerminate testInterruptDoesntBreakExecutor(
            TaskExecutorService executor) throws Exception {

        MockTask task2 = mock(MockTask.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            Thread.currentThread().interrupt();
        });

        Thread.sleep(50);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task2));

        return () -> verify(task2).execute(false);
    }
}
