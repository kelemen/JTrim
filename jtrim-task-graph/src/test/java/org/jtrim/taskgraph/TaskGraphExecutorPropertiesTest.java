package org.jtrim.taskgraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.jtrim.utils.LogCollector;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class TaskGraphExecutorPropertiesTest {
    private static TaskNodeKey<Integer, String> nodeKey(String key) {
        return new TaskNodeKey<>(new TaskFactoryKey<>(Integer.class, String.class), key);
    }

    @Test
    public void testDefaultValues() {
        TaskGraphExecutorProperties.Builder builder = new TaskGraphExecutorProperties.Builder();
        TaskGraphExecutorProperties properties = builder.build();

        assertTrue("getResultNodeKeys", properties.getResultNodeKeys().isEmpty());
        assertFalse("isDeliverResultOnFailure", properties.isDeliverResultOnFailure());
        assertFalse("isDeliverResultOnFailure", properties.isStopOnFailure());

        TaskNodeKey<?, ?> nodeKey = nodeKey("F-ARG");
        RuntimeException error = new RuntimeException("Test-Error");

        try (LogCollector logs = LogCollector.startCollecting("org.jtrim")) {
            properties.getComputeErrorHandler().onError(nodeKey, error);

            Throwable[] errors = logs.getExceptions(Level.SEVERE);
            boolean hasError = Arrays.stream(errors)
                    .filter(currentError -> currentError == error)
                    .findAny()
                    .isPresent();
            assertTrue("Exception must have been logged.", hasError);

            String nodeKeyStr = nodeKey.toString();
            boolean hasMessage = Arrays.stream(logs.getLogs())
                    .filter(record -> record.getLevel() == Level.SEVERE)
                    .filter(record -> record.getMessage().contains(nodeKeyStr))
                    .findAny()
                    .isPresent();
            assertTrue("The log message must contain the node key.", hasMessage);
        }
    }

    @Test
    public void testCopy() {
        TaskErrorHandler errorHandler = Mockito.mock(TaskErrorHandler.class);
        Set<TaskNodeKey<?, ?>> nodeKeys = new HashSet<>(Arrays.asList(
                nodeKey("T-NODE-1"),
                nodeKey("T-NODE-2"),
                nodeKey("T-NODE-3")));

        TaskGraphExecutorProperties.Builder srcBuilder = new TaskGraphExecutorProperties.Builder();
        srcBuilder.setStopOnFailure(true);
        srcBuilder.setDeliverResultOnFailure(true);
        srcBuilder.setComputeErrorHandler(errorHandler);
        srcBuilder.addResultNodeKeys(Collections.unmodifiableSet(nodeKeys));
        TaskGraphExecutorProperties srcProperties = srcBuilder.build();

        TaskGraphExecutorProperties.Builder builder = new TaskGraphExecutorProperties.Builder(srcProperties);
        TaskGraphExecutorProperties properties = builder.build();

        assertEquals("getResultNodeKeys", nodeKeys, properties.getResultNodeKeys());
        assertEquals("isDeliverResultOnFailure", true, properties.isDeliverResultOnFailure());
        assertEquals("isStopOnFailure", true, properties.isStopOnFailure());
        assertSame("getComputeErrorHandler", errorHandler, properties.getComputeErrorHandler());

    }

    @Test
    public void testIsStopOnFailure() {
        TaskGraphExecutorProperties.Builder builder = new TaskGraphExecutorProperties.Builder();
        builder.setStopOnFailure(true);
        TaskGraphExecutorProperties properties = builder.build();

        assertTrue(properties.isStopOnFailure());
    }

    @Test
    public void testIsDeliverResultOnFailure() {
        TaskGraphExecutorProperties.Builder builder = new TaskGraphExecutorProperties.Builder();
        builder.setDeliverResultOnFailure(true);
        TaskGraphExecutorProperties properties = builder.build();

        assertTrue(properties.isDeliverResultOnFailure());
    }

    @Test
    public void testGetResultNodeKeys() {
        TaskNodeKey<?, ?> node1 = nodeKey("T-NODE-1");
        TaskNodeKey<?, ?> node2 = nodeKey("T-NODE-2");
        TaskNodeKey<?, ?> node3 = nodeKey("T-NODE-3");

        TaskGraphExecutorProperties.Builder builder = new TaskGraphExecutorProperties.Builder();
        builder.addResultNodeKey(node1);
        builder.addResultNodeKeys(Arrays.asList(node2, node3));
        TaskGraphExecutorProperties properties = builder.build();

        assertEquals(new HashSet<>(Arrays.asList(node1, node2, node3)), properties.getResultNodeKeys());
    }

    @Test
    public void testComputeErrorHandler() {
        TaskErrorHandler errorHandler = Mockito.mock(TaskErrorHandler.class);

        TaskGraphExecutorProperties.Builder builder = new TaskGraphExecutorProperties.Builder();
        builder.setComputeErrorHandler(errorHandler);
        TaskGraphExecutorProperties properties = builder.build();

        assertSame(errorHandler, properties.getComputeErrorHandler());
    }
}
