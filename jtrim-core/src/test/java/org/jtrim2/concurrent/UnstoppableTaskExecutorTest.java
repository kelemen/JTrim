package org.jtrim2.concurrent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class UnstoppableTaskExecutorTest {

    public UnstoppableTaskExecutorTest() {
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
