package org.jtrim2.concurrent.query;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.testutils.FutureUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncDataConverterTest {
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
        when(wrappedConverter.convertData(same(input))).thenReturn(output);

        AsyncDataConverter<Object, Object> converter =
                new AsyncDataConverter<>(wrappedConverter, executor);

        CompletionStage<Object> future = converter.submit(Cancellation.UNCANCELABLE_TOKEN, input);
        assertSame(output, FutureUtils.tryGetResult(future));
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
