package org.jtrim.utils;

import java.util.logging.Level;
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
public class ObjectFinalizerTest {
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

    /**
     * Test of markFinalized method, of class ObjectFinalizer.
     */
    @Test
    public void testMarkFinalized() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.markFinalized();
        assertFalse(finalizer.doFinalize());

        verifyZeroInteractions(task);
    }

    /**
     * Test of doFinalize method, of class ObjectFinalizer.
     */
    @Test
    public void testDoFinalize() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        assertTrue(finalizer.doFinalize());

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test(expected = TestException.class)
    public void testDoFinalizePropagatesException() {
        Runnable task = mock(Runnable.class);
        doThrow(TestException.class).when(task).run();

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.doFinalize();
    }

    @Test
    public void testMultipleDoFinalize() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        assertTrue(finalizer.doFinalize());
        assertFalse(finalizer.doFinalize());

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    /**
     * Test of isFinalized method, of class ObjectFinalizer.
     */
    @Test
    public void testIsFinalized1() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        assertFalse(finalizer.isFinalized());

        assertTrue(finalizer.doFinalize());

        assertTrue(finalizer.isFinalized());
    }

    @Test
    public void testIsFinalized2() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        assertFalse(finalizer.isFinalized());

        finalizer.markFinalized();

        assertTrue(finalizer.isFinalized());
    }

    /**
     * Test of checkNotFinalized method, of class ObjectFinalizer.
     */
    @Test(expected = IllegalStateException.class)
    public void testCheckNotFinalized1() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.doFinalize();
        finalizer.checkNotFinalized();
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckNotFinalized2() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.markFinalized();
        finalizer.checkNotFinalized();
    }

    @Test
    public void testCheckNotFinalized3() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.checkNotFinalized();
    }

    @SuppressWarnings("FinalizeCalledExplicitly")
    private static void finalizeObject(ObjectFinalizer finalizer) {
        finalizer.finalize();
    }

    @Test
    public void testFinalizeBeforeDoFinalize() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");

        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            finalizeObject(finalizer);
            assertEquals(1, logs.getNumberOfLogs(Level.SEVERE));
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testFinalizeBeforeDoFinalizeTaskFails() {
        Runnable task = mock(Runnable.class);
        doThrow(new TestException()).when(task).run();

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");

        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            finalizeObject(finalizer);
            LogCollectorTest.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testFinalizeBeforeDoFinalizeInheritMessage() {
        Runnable task = mock(Runnable.class);
        stub(task.toString()).toReturn("DESCRIPTION");

        ObjectFinalizer finalizer = new ObjectFinalizer(task);
        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            finalizeObject(finalizer);
            assertEquals(1, logs.getNumberOfLogs());
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testFinalizeAfterDoFinalize() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.doFinalize();
        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            finalizeObject(finalizer);
            assertEquals(0, logs.getNumberOfLogs());
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testFinalizeAfterMarkFinalized() {
        Runnable task = mock(Runnable.class);

        ObjectFinalizer finalizer = new ObjectFinalizer(task, "DESCRIPTION");
        finalizer.markFinalized();
        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            finalizeObject(finalizer);
            assertEquals(0, logs.getNumberOfLogs());
        }

        verifyZeroInteractions(task);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -7461262938744477494L;
    }
}
