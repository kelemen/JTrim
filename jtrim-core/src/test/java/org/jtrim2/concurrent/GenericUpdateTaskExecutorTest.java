package org.jtrim2.concurrent;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class GenericUpdateTaskExecutorTest {

    public GenericUpdateTaskExecutorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static GenericUpdateTaskExecutor create(TaskExecutor executor, boolean usePlainExecutor) {
        return usePlainExecutor
                ? new GenericUpdateTaskExecutor(ExecutorConverter.asExecutor(executor))
                : new GenericUpdateTaskExecutor(executor);
    }

    @Test
    public void testMultipleExecute() {
        for (boolean usePlainExecutor: Arrays.asList(false, true)) {
            ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
            UpdateTaskExecutor executor = create(wrapped, usePlainExecutor);

            for (int i = 0; i < 5; i++) {
                Runnable task = mock(Runnable.class);
                executor.execute(task);

                wrapped.executeCurrentlySubmitted();

                verify(task).run();
            }
        }
    }

    @Test
    public void testExecuteOverwrite() {
        for (boolean usePlainExecutor: Arrays.asList(false, true)) {
            ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
            UpdateTaskExecutor executor = create(wrapped, usePlainExecutor);

            Runnable task = mock(Runnable.class);
            Runnable task2 = mock(Runnable.class);
            executor.execute(task);
            executor.execute(task2);

            wrapped.executeCurrentlySubmitted();

            verifyZeroInteractions(task);
            verify(task2).run();
        }
    }

    @Test
    public void testExecuteAfterShutdown() {
        for (boolean usePlainExecutor: Arrays.asList(false, true)) {
            ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
            UpdateTaskExecutor executor = create(wrapped, usePlainExecutor);

            executor.shutdown();
            Runnable task = mock(Runnable.class);
            executor.execute(task);

            wrapped.executeCurrentlySubmitted();

            verifyZeroInteractions(task);
        }
    }

    /**
     * Test of toString method, of class GenericUpdateTaskExecutor.
     */
    @Test
    public void testToString() {
        assertNotNull(new GenericUpdateTaskExecutor(SyncTaskExecutor.getSimpleExecutor()).toString());
    }
}
