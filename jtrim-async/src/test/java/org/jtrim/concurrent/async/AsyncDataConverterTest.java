package org.jtrim.concurrent.async;

import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.TaskFuture;
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
public class AsyncDataConverterTest {

    public AsyncDataConverterTest() {
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
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockConverter() {
        return mock(DataConverter.class);
    }

    /**
     * Test of submit method, of class AsyncDataConverter.
     */
    @Test
    public void testSubmit() {
        DataConverter<Object, Object> wrappedConverter = mockConverter();
        SyncTaskExecutor executor = new SyncTaskExecutor();

        Object input = new Object();
        Object output = new Object();
        stub(wrappedConverter.convertData(same(input))).toReturn(output);

        AsyncDataConverter<Object, Object> converter =
                new AsyncDataConverter<>(wrappedConverter, executor);

        TaskFuture<Object> future = converter.submit(Cancellation.UNCANCELABLE_TOKEN, input);
        assertSame(output, future.tryGetResult());
    }

    /**
     * Test of getConverter method, of class AsyncDataConverter.
     */
    @Test
    public void testGetConverter() {
        DataConverter<Object, Object> wrappedConverter = mockConverter();
        AsyncDataConverter<Object, Object> converter = new AsyncDataConverter<>(
                wrappedConverter, mock(TaskExecutorService.class));

        assertSame(wrappedConverter, converter.getConverter());
    }

    /**
     * Test of getExecutor method, of class AsyncDataConverter.
     */
    @Test
    public void testGetExecutor() {
        TaskExecutorService executor = mock(TaskExecutorService.class);
        AsyncDataConverter<Object, Object> converter = new AsyncDataConverter<>(
                mockConverter(), executor);

        assertSame(executor, converter.getExecutor());
    }

    /**
     * Test of toString method, of class AsyncDataConverter.
     */
    @Test
    public void testToString() {
        AsyncDataConverter<Object, Object> converter = new AsyncDataConverter<>(
                mockConverter(), mock(TaskExecutorService.class));

        assertNotNull(converter.toString());
    }

}