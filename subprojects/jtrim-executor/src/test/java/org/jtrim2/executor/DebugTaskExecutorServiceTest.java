package org.jtrim2.executor;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class DebugTaskExecutorServiceTest extends AbstractDebugTaskExecutorTest {
    public DebugTaskExecutorServiceTest() {
        super(() -> new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance()));
    }

    @Test
    public void testUsingRightExecutor() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(true);
        DebugTaskExecutorService executor = new DebugTaskExecutorService(TaskExecutors.upgradeToStoppable(wrapped));

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verifyZeroInteractions(task);
        wrapped.executeCurrentlySubmitted();
        verify(task).execute(any(CancellationToken.class));
    }
}
