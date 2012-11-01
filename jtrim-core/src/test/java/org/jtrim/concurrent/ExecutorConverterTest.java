package org.jtrim.concurrent;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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
public class ExecutorConverterTest {

    public ExecutorConverterTest() {
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

    @Test
    public void testExecutorConvertBack() {
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        assertSame(taskExecutor, ExecutorConverter.asTaskExecutor(ExecutorConverter.asExecutor(taskExecutor)));

        Executor executor = mock(Executor.class);
        assertSame(executor, ExecutorConverter.asExecutor(ExecutorConverter.asTaskExecutor(executor)));
    }

    @Test
    public void testExecutorSerivceConvertBack() {
        TaskExecutorService taskExecutor = mock(TaskExecutorService.class);
        assertSame(taskExecutor, ExecutorConverter.asTaskExecutorService(ExecutorConverter.asExecutorService(taskExecutor)));
        assertSame(taskExecutor, ExecutorConverter.asTaskExecutorService(ExecutorConverter.asExecutorService(taskExecutor, false)));
        assertSame(taskExecutor, ExecutorConverter.asTaskExecutorService(ExecutorConverter.asExecutorService(taskExecutor, true)));

        ExecutorService executor = mock(ExecutorService.class);
        assertSame(executor, ExecutorConverter.asExecutorService(ExecutorConverter.asTaskExecutorService(executor)));
        assertSame(executor, ExecutorConverter.asExecutorService(ExecutorConverter.asTaskExecutorService(executor), false));
        assertSame(executor, ExecutorConverter.asExecutorService(ExecutorConverter.asTaskExecutorService(executor), true));
    }

    // The following tests only verifies that they create an executor with
    // the appropriate type. More through tests are done in the test of that
    // class.

    /**
     * Test of asExecutorService method, of class ExecutorConverter.
     */
    @Test
    public void testAsExecutorService_TaskExecutorService() {
        TaskExecutorService taskExecutor = mock(TaskExecutorService.class);
        ExecutorService executor = ExecutorConverter.asExecutorService(taskExecutor);
        assertTrue(executor instanceof TaskExecutorServiceAsExecutorService);
        assertSame(taskExecutor, ((TaskExecutorServiceAsExecutorService)executor).executor);
    }

    /**
     * Test of asExecutorService method, of class ExecutorConverter.
     */
    @Test
    public void testAsExecutorService_TaskExecutorService_boolean() {
        for (Boolean mayInterrupt: Arrays.asList(false, true)) {
            TaskExecutorService taskExecutor = mock(TaskExecutorService.class);
            ExecutorService executor = ExecutorConverter.asExecutorService(taskExecutor, mayInterrupt);
            assertTrue(executor instanceof TaskExecutorServiceAsExecutorService);
            assertSame(taskExecutor, ((TaskExecutorServiceAsExecutorService)executor).executor);
        }
    }

    /**
     * Test of asTaskExecutorService method, of class ExecutorConverter.
     */
    @Test
    public void testAsTaskExecutorService() {
        ExecutorService executor = mock(ExecutorService.class);
        TaskExecutorService taskExecutor = ExecutorConverter.asTaskExecutorService(executor);
        assertTrue(taskExecutor instanceof ExecutorServiceAsTaskExecutorService);
        assertSame(executor, ((ExecutorServiceAsTaskExecutorService)taskExecutor).executor);
    }

    /**
     * Test of asTaskExecutor method, of class ExecutorConverter.
     */
    @Test
    public void testAsTaskExecutor() {
        Executor executor = mock(Executor.class);
        TaskExecutor taskExecutor = ExecutorConverter.asTaskExecutor(executor);
        assertTrue(taskExecutor instanceof ExecutorAsTaskExecutor);
        assertSame(executor, ((ExecutorAsTaskExecutor)taskExecutor).executor);
    }

    /**
     * Test of asExecutor method, of class ExecutorConverter.
     */
    @Test
    public void testAsExecutor() {
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        Executor executor = ExecutorConverter.asExecutor(taskExecutor);
        assertTrue(executor instanceof TaskExecutorAsExecutor);
        assertSame(taskExecutor, ((TaskExecutorAsExecutor)executor).executor);
    }

}