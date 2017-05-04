package org.jtrim2.taskgraph;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class TaskGraphBuilderPropertiesTest {
    private void verifySyncExecutor(TaskExecutor executor) {
        AtomicReference<Thread> taskThreadRef = new AtomicReference<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            taskThreadRef.set(Thread.currentThread());
        }, null);
        assertSame(Thread.currentThread(), taskThreadRef.get());
    }

    @Test
    public void testDefaultValues() {
        TaskGraphBuilderProperties.Builder builder = new TaskGraphBuilderProperties.Builder();
        TaskGraphBuilderProperties properties = builder.build();

        verifySyncExecutor(properties.getDefaultFactoryProperties().getFactoryExecutor());
        verifySyncExecutor(properties.getDefaultFactoryProperties().getDefaultNodeProperties().getExecutor());

        TestTaskErrorHandlers.verifyLogsAsError("org.jtrim2", properties.getNodeCreateErrorHandler());
    }

    @Test
    public void testCopy() {
        TaskErrorHandler errorHandler = Mockito.mock(TaskErrorHandler.class);
        ManualTaskExecutor factoryExecutor = new ManualTaskExecutor(true);
        ManualTaskExecutor nodeExecutor = new ManualTaskExecutor(false);

        TaskGraphBuilderProperties.Builder srcBuilder = new TaskGraphBuilderProperties.Builder();
        srcBuilder.setNodeCreateErrorHandler(errorHandler);
        srcBuilder.defaultFactoryProperties().setFactoryExecutor(factoryExecutor);
        srcBuilder.defaultFactoryProperties().defaultNodeProperties().setExecutor(nodeExecutor);
        TaskGraphBuilderProperties srcProperties = srcBuilder.build();

        TaskGraphBuilderProperties.Builder builder = new TaskGraphBuilderProperties.Builder(srcProperties);
        TaskGraphBuilderProperties properties = builder.build();

        assertSame("getFactoryExecutor",
                nodeExecutor,
                properties.getDefaultFactoryProperties().getDefaultNodeProperties().getExecutor());
        assertSame("getFactoryExecutor",
                factoryExecutor,
                properties.getDefaultFactoryProperties().getFactoryExecutor());
        assertSame("getNodeCreateErrorHandler", errorHandler, properties.getNodeCreateErrorHandler());
    }

    @Test
    public void testNodeCreateErrorHandler() {
        TaskErrorHandler errorHandler = Mockito.mock(TaskErrorHandler.class);

        TaskGraphBuilderProperties.Builder builder = new TaskGraphBuilderProperties.Builder();
        builder.setNodeCreateErrorHandler(errorHandler);
        TaskGraphBuilderProperties properties = builder.build();

        assertSame(errorHandler, properties.getNodeCreateErrorHandler());
    }

    @Test
    public void testFactoryExecutor() {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        TaskGraphBuilderProperties.Builder builder = new TaskGraphBuilderProperties.Builder();
        builder.defaultFactoryProperties().setFactoryExecutor(executor);
        TaskGraphBuilderProperties properties = builder.build();

        assertSame(executor, properties.getDefaultFactoryProperties().getFactoryExecutor());
    }

    @Test
    public void testNodeExecutor() {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        TaskGraphBuilderProperties.Builder builder = new TaskGraphBuilderProperties.Builder();
        builder.defaultFactoryProperties().defaultNodeProperties().setExecutor(executor);
        TaskGraphBuilderProperties properties = builder.build();

        assertSame(executor, properties.getDefaultFactoryProperties().getDefaultNodeProperties().getExecutor());
    }
}
