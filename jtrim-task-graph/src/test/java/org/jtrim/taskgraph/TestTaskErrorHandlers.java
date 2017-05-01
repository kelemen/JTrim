package org.jtrim.taskgraph;

import java.util.Arrays;
import java.util.logging.Level;
import org.jtrim.taskgraph.basic.TestNodes;
import org.jtrim.utils.LogCollector;

import static org.junit.Assert.*;

public final class TestTaskErrorHandlers {
    public static void verifyLogsAsError(String loggerPrefix, TaskErrorHandler handler) {
        TaskNodeKey<?, ?> nodeKey = TestNodes.node("F-ARG");
        RuntimeException error = new RuntimeException("Test-Error");

        try (LogCollector logs = LogCollector.startCollecting(loggerPrefix)) {
            handler.onError(nodeKey, error);

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

    private TestTaskErrorHandlers() {
        throw new AssertionError();
    }
}
