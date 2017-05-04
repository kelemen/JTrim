package org.jtrim2.concurrent.async;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.SyncTaskExecutor;
import org.jtrim2.concurrent.TaskExecutorService;
import org.jtrim2.concurrent.TaskFuture;
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
public class AsyncDataTransformerTest {

    public AsyncDataTransformerTest() {
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

    @SuppressWarnings("unchecked")
    private static <DataType> DataTransformer<DataType> mockTransformer() {
        return mock(DataTransformer.class);
    }

    private static <DataType> AsyncDataTransformer<DataType> create(
            DataTransformer<DataType> transformer,
            TaskExecutorService executor) {
        return new AsyncDataTransformer<>(transformer, executor);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null, mock(TaskExecutorService.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mockTransformer(), null);
    }

    /**
     * Test of submit method, of class AsyncDataTransformer.
     */
    @Test
    public void testSubmit() {
        TaskExecutorService executor = new SyncTaskExecutor();
        DataTransformer<Object> transformer = mockTransformer();

        Object input = new Object();
        Object output = new Object();

        stub(transformer.transform(any())).toReturn(output);

        AsyncDataTransformer<Object> asyncTransformer = new AsyncDataTransformer<>(transformer, executor);

        TaskFuture<Object> future = asyncTransformer.submit(Cancellation.UNCANCELABLE_TOKEN, input);
        verify(transformer).transform(same(input));

        assertSame(output, future.tryGetResult());

        verifyNoMoreInteractions(transformer);
    }

    /**
     * Test of getTransformer method, of class AsyncDataTransformer.
     */
    @Test
    public void testGetTransformer() {
        TaskExecutorService executor = mock(TaskExecutorService.class);
        DataTransformer<Object> transformer = mockTransformer();
        AsyncDataTransformer<Object> asyncTransformer = new AsyncDataTransformer<>(transformer, executor);

        assertSame(transformer, asyncTransformer.getTransformer());
    }

    /**
     * Test of getExecutor method, of class AsyncDataTransformer.
     */
    @Test
    public void testGetExecutor() {
        TaskExecutorService executor = mock(TaskExecutorService.class);
        DataTransformer<Object> transformer = mockTransformer();
        AsyncDataTransformer<Object> asyncTransformer = new AsyncDataTransformer<>(transformer, executor);

        assertSame(executor, asyncTransformer.getExecutor());
    }

    /**
     * Test of toString method, of class AsyncDataTransformer.
     */
    @Test
    public void testToString() {
        AsyncDataTransformer<Object> asyncTransformer
                = new AsyncDataTransformer<>(mockTransformer(), mock(TaskExecutorService.class));
        assertNotNull(asyncTransformer.toString());
    }
}
