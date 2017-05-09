package org.jtrim2.swing.concurrent;

import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class SwingUpdateTaskExecutorTest {
    private static SwingUpdateTaskExecutor[] allExecutors() {
        return new SwingUpdateTaskExecutor[]{
            new SwingUpdateTaskExecutor(),
            new SwingUpdateTaskExecutor(false),
            new SwingUpdateTaskExecutor(true)
        };
    }

    private static SwingUpdateTaskExecutor[] lazyExecutors() {
        return new SwingUpdateTaskExecutor[]{
            new SwingUpdateTaskExecutor(),
            new SwingUpdateTaskExecutor(true)
        };
    }

    private static SwingUpdateTaskExecutor[] eagerExecutors() {
        return new SwingUpdateTaskExecutor[]{
            new SwingUpdateTaskExecutor(false)
        };
    }

    @Test
    public void testExecuteLazyFromEdt() throws Exception {
        for (final SwingUpdateTaskExecutor executor: lazyExecutors()) {
            final Runnable firstTask = mock(Runnable.class);
            final Runnable lastTask = mock(Runnable.class);

            SwingUtilities.invokeAndWait(() -> {
                executor.execute(firstTask);
                executor.execute(lastTask);
            });

            SwingUtilities.invokeAndWait(() -> {
                verifyZeroInteractions(firstTask);
                verify(lastTask).run();
            });
        }
    }

    @Test
    public void testExecuteEagerFromEdt() throws Exception {
        for (final SwingUpdateTaskExecutor executor: eagerExecutors()) {
            SwingUtilities.invokeAndWait(() -> {
                Runnable firstTask = mock(Runnable.class);
                Runnable lastTask = mock(Runnable.class);

                executor.execute(firstTask);
                verify(firstTask).run();

                executor.execute(lastTask);
                verify(lastTask).run();
            });
        }
    }

    @Test
    public void testExecuteAfterShutdown() throws Exception {
        for (final SwingUpdateTaskExecutor executor: allExecutors()) {
            final Runnable task = mock(Runnable.class);
            executor.shutdown();
            executor.execute(task);

            SwingUtilities.invokeAndWait(() -> {
                verifyZeroInteractions(task);
            });
        }
    }

    @Test
    public void testExecuteAfterShutdownFromEdt() throws Exception {
        for (final SwingUpdateTaskExecutor executor: allExecutors()) {
            final Runnable task = mock(Runnable.class);
            SwingUtilities.invokeAndWait(() -> {
                executor.shutdown();
                executor.execute(task);
            });

            SwingUtilities.invokeAndWait(() -> {
                verifyZeroInteractions(task);
            });
        }
    }
}
