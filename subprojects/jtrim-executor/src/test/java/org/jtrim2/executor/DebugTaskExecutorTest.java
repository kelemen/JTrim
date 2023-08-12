package org.jtrim2.executor;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class DebugTaskExecutorTest extends AbstractDebugTaskExecutorTest {
    public DebugTaskExecutorTest() {
        super(() -> new DebugTaskExecutor(SyncTaskExecutor.getDefaultInstance()));
    }

    @Test
    public void testUsingRightExecutor() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(true);
        DebugTaskExecutor executor = new DebugTaskExecutor(TaskExecutors.upgradeToStoppable(wrapped));

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verifyNoInteractions(task);
        wrapped.executeCurrentlySubmitted();
        verify(task).execute(any(CancellationToken.class));
    }
}
