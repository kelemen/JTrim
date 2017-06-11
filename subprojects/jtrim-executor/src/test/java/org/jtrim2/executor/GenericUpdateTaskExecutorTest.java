package org.jtrim2.executor;

import java.util.concurrent.Executor;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GenericUpdateTaskExecutorTest {
    private static GenericUpdateTaskExecutor create(Executor executor) {
        return new GenericUpdateTaskExecutor(executor);
    }

    @Test
    public void testMultipleExecute() {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        UpdateTaskExecutor executor = create(wrapped);

        for (int i = 0; i < 5; i++) {
            Runnable task = mock(Runnable.class);
            executor.execute(task);

            wrapped.executeCurrentlySubmitted();

            verify(task).run();
        }
    }

    @Test
    public void testExecuteOverwrite() {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        UpdateTaskExecutor executor = create(wrapped);

        Runnable task = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);
        executor.execute(task);
        executor.execute(task2);

        wrapped.executeCurrentlySubmitted();

        verifyZeroInteractions(task);
        verify(task2).run();
    }

    @Test
    public void testToString() {
        assertNotNull(new GenericUpdateTaskExecutor(SyncTaskExecutor.getSimpleExecutor()).toString());
    }
}
