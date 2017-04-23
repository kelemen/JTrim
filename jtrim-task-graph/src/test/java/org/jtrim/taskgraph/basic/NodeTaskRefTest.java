package org.jtrim.taskgraph.basic;

import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.ManualTaskExecutor;
import org.jtrim.taskgraph.TaskNodeProperties;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class NodeTaskRefTest {
    private static TaskNodeProperties properties() {
        TaskNodeProperties.Builder result = new TaskNodeProperties.Builder();
        result.setExecutor(new ManualTaskExecutor(true));
        return result.build();
    }

    @SuppressWarnings("unchecked")
    private static <V> CancelableFunction<V> mockFunction() {
        return mock(CancelableFunction.class);
    }

    @Test
    public void testCompute() throws Exception {
        TaskNodeProperties properties = properties();
        Object result = "MY-TEST-RESULT";

        CancelableFunction<Object> function = mockFunction();
        doReturn(result).when(function).execute(any(CancellationToken.class));

        NodeTaskRef<Object> nodeTaskRef = new NodeTaskRef<>(properties, function);

        CancellationToken token = Cancellation.createCancellationSource().getToken();

        verifyZeroInteractions(function);
        Object computedResult = nodeTaskRef.compute(token);

        verify(function).execute(same(token));
        assertSame(result, computedResult);
    }

    @Test
    public void testProperties() {
        TaskNodeProperties properties = properties();
        CancelableFunction<Object> function = mockFunction();

        NodeTaskRef<Object> nodeTaskRef = new NodeTaskRef<>(properties, function);

        assertSame(properties, nodeTaskRef.getProperties());
    }
}
