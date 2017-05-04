package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class UnstoppableExecutorTest {

    public UnstoppableExecutorTest() {
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

    private static UnstoppableExecutor create(ExecutorService executor) {
        return new UnstoppableExecutor(executor);
    }

    /**
     * Test of execute method, of class UnstoppableExecutor.
     */
    @Test
    public void testExecute() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Runnable task = mock(Runnable.class);

        create(subExecutor).execute(task);
        verify(subExecutor).execute(task);

        verifyZeroInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of submit method, of class UnstoppableExecutor.
     */
    @Test
    public void testSubmit_Runnable() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Runnable task = mock(Runnable.class);
        Future<?> future = mock(Future.class);

        Mockito.<Object>stub(subExecutor.submit(any(Runnable.class))).toReturn(future);

        assertSame(future, create(subExecutor).submit(task));
        verify(subExecutor).submit(task);

        verifyZeroInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of submit method, of class UnstoppableExecutor.
     */
    @Test
    public void testSubmit_Runnable_GenericType() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Runnable task = mock(Runnable.class);
        Future<?> future = mock(Future.class);
        Object result = new Object();

        Mockito.<Object>stub(subExecutor.submit(any(Runnable.class), any())).toReturn(future);

        assertSame(future, create(subExecutor).submit(task, result));

        verify(subExecutor).submit(task, result);

        verifyZeroInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of submit method, of class UnstoppableExecutor.
     */
    @Test
    public void testSubmit_Callable() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Callable<?> task = mock(Callable.class);
        Future<?> future = mock(Future.class);

        Mockito.<Object>stub(subExecutor.submit((Callable<?>)any(Callable.class)))
                .toReturn(future);

        assertSame(future, create(subExecutor).submit(task));
        verify(subExecutor).submit(task);

        verifyZeroInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of shutdownNow method, of class UnstoppableExecutor.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testShutdownNow() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        try {
            create(subExecutor).shutdownNow();
        } finally {
            verifyZeroInteractions(subExecutor);
        }
    }

    /**
     * Test of shutdown method, of class UnstoppableExecutor.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testShutdown() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        try {
            create(subExecutor).shutdown();
        } finally {
            verifyZeroInteractions(subExecutor);
        }
    }

    /**
     * Test of isTerminated method, of class UnstoppableExecutor.
     */
    @Test
    public void testIsTerminated() {
        for (Boolean result: Arrays.asList(false, true)) {
            ExecutorService subExecutor = mock(ExecutorService.class);

            stub(subExecutor.isTerminated()).toReturn(result);

            assertEquals(result, create(subExecutor).isTerminated());
            verify(subExecutor).isTerminated();
            verifyNoMoreInteractions(subExecutor);
        }
    }

    /**
     * Test of isShutdown method, of class UnstoppableExecutor.
     */
    @Test
    public void testIsShutdown() {
        for (Boolean result: Arrays.asList(false, true)) {
            ExecutorService subExecutor = mock(ExecutorService.class);

            stub(subExecutor.isShutdown()).toReturn(result);

            assertEquals(result, create(subExecutor).isShutdown());
            verify(subExecutor).isShutdown();
            verifyNoMoreInteractions(subExecutor);
        }
    }

    @Test
    public void testInvokeAny_3args() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);
        long timeout = 54645375432L;
        TimeUnit unit = TimeUnit.MINUTES;
        Object result = new Object();

        Mockito.<Object>stub(subExecutor.invokeAny(same(tasks), anyLong(), any(TimeUnit.class)))
                .toReturn(result);

        assertSame(result, create(subExecutor).invokeAny(tasks, timeout, unit));

        verify(subExecutor).invokeAny(tasks, timeout, unit);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInvokeAny_Collection() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);
        Object result = new Object();

        Mockito.<Object>stub(subExecutor.invokeAny(tasks))
                .toReturn(result);

        assertSame(result, create(subExecutor).invokeAny(tasks));

        verify(subExecutor).invokeAny(tasks);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInvokeAll_3args() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);
        long timeout = 54645375432L;
        TimeUnit unit = TimeUnit.MINUTES;

        List<Future<Object>> result = new LinkedList<>();

        Mockito.<Object>stub(subExecutor.invokeAll(same(tasks), anyLong(), any(TimeUnit.class)))
                .toReturn(result);

        assertSame(result, create(subExecutor).invokeAll(tasks, timeout, unit));

        verify(subExecutor).invokeAll(tasks, timeout, unit);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInvokeAll_Collection() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);

        List<Future<Object>> result = new LinkedList<>();

        Mockito.<Object>stub(subExecutor.invokeAll(tasks))
                .toReturn(result);

        assertSame(result, create(subExecutor).invokeAll(tasks));

        verify(subExecutor).invokeAll(tasks);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testAwaitTermination() throws Exception {
        for (Boolean result: Arrays.asList(false, true)) {
            ExecutorService subExecutor = mock(ExecutorService.class);

            long timeout = 54645375432L;
            TimeUnit unit = TimeUnit.MINUTES;

            stub(subExecutor.awaitTermination(anyLong(), any(TimeUnit.class)))
                    .toReturn(result);

            assertEquals(result, create(subExecutor).awaitTermination(timeout, unit));

            verify(subExecutor).awaitTermination(timeout, unit);
            verifyNoMoreInteractions(subExecutor);
        }
    }

    /**
     * Test of toString method, of class UnstoppableExecutor.
     */
    @Test
    public void testToString() {
        ExecutorService subExecutor = mock(ExecutorService.class);

        stub(subExecutor.toString()).toReturn("STR-VALUE");

        assertNotNull(create(subExecutor).toString());
    }
}
