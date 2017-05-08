package org.jtrim2.executor;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class UnstoppableTaskExecutorTest {
    private static UnstoppableTaskExecutor create(TaskExecutorService executor) {
        return new UnstoppableTaskExecutor(executor);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor() {
        create(null);
    }

    /**
     * Test of shutdown method, of class UnstoppableTaskExecutor.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testShutdown() {
        TaskExecutorService subExecutor = mock(TaskExecutorService.class);
        try {
            create(subExecutor).shutdown();
        } finally {
            verifyZeroInteractions(subExecutor);
        }
    }

    /**
     * Test of shutdownAndCancel method, of class UnstoppableTaskExecutor.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testShutdownAndCancel() {
        TaskExecutorService subExecutor = mock(TaskExecutorService.class);
        try {
            create(subExecutor).shutdownAndCancel();
        } finally {
            verifyZeroInteractions(subExecutor);
        }
    }
}
