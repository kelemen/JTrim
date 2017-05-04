package org.jtrim2.testutils;

import java.util.logging.Level;
import org.jtrim2.utils.LogCollector;

import static org.junit.Assert.*;

public final class LogTests {
    public static LogCollector startCollecting() {
        return LogCollector.startCollecting("org.jtrim2");
    }

    public static void verifyLogCount(
            Class<? extends Throwable> cl,
            Level level,
            int expectedCount,
            LogCollector collector,
            Throwable... additional) {

        Throwable[] collected = LogCollector.extractThrowables(cl, collector.getExceptions(level));
        int additionalCount = LogCollector.extractThrowables(cl, additional).length;

        int actualCount = collected.length + additionalCount;
        if (expectedCount != actualCount) {
            fail("Expected " + expectedCount + " " + level + " logs but found: " + actualCount);
        }
    }

    private LogTests() {
        throw new AssertionError();
    }
}
