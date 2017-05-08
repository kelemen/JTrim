package org.jtrim2.logs;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jtrim2.collections.CollectionsEx;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class LogCollectorTest {
    private static final String TEST_LOGGER_NAME = LogCollector.class.getName();
    private static final Logger TEST_LOGGER = Logger.getLogger(TEST_LOGGER_NAME);

    @BeforeClass
    public static void setUpClass() {
        TEST_LOGGER.setLevel(Level.ALL);
    }

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

    private static Level[] getLevels() {
        return new Level[] {
            Level.FINEST,
            Level.FINER,
            Level.FINE,
            Level.CONFIG,
            Level.INFO,
            Level.WARNING,
            Level.SEVERE
        };
    }

    @Test
    public void testCollectOne() {
        for (Level level: getLevels()) {
            try (LogCollector logs = startCollecting()) {
                TestException passedError = new TestException();

                TEST_LOGGER.log(level, "", passedError);

                Throwable[] exceptions = logs.getExceptions(level);
                assertArrayEquals(new Throwable[]{passedError}, exceptions);

                LogRecord[] records = logs.getLogs();
                assertEquals(1, records.length);
                assertSame(passedError, records[0].getThrown());

                assertEquals(1, logs.getNumberOfLogs());
                assertEquals(1, logs.getNumberOfLogs(level));
            }
        }
    }

    @Test
    public void testCollectDifferentTypes() {
        try (LogCollector logs = startCollecting()) {
            TestException errorWarn = new TestException();
            TestException errorSevere1 = new TestException();
            TestException errorSevere2 = new TestException();

            TEST_LOGGER.log(Level.SEVERE, "", errorSevere1);
            TEST_LOGGER.log(Level.WARNING, "", errorWarn);
            TEST_LOGGER.log(Level.SEVERE, "", errorSevere2);

            Throwable[] exceptions0 = logs.getExceptions(Level.CONFIG);
            assertEquals(0, exceptions0.length);

            Throwable[] exceptions1 = logs.getExceptions(Level.WARNING);
            assertArrayEquals(new Throwable[]{errorWarn}, exceptions1);

            Throwable[] exceptions2 = logs.getExceptions(Level.SEVERE);
            assertArrayEquals(new Throwable[]{errorSevere1, errorSevere2}, exceptions2);

            LogRecord[] records = logs.getLogs();
            assertEquals(3, records.length);
            assertArrayEquals(
                    new Throwable[]{errorSevere1, errorWarn, errorSevere2},
                    new Throwable[]{records[0].getThrown(), records[1].getThrown(), records[2].getThrown()});

            assertEquals(3, logs.getNumberOfLogs());
            assertEquals(0, logs.getNumberOfLogs(Level.FINE));
            assertEquals(1, logs.getNumberOfLogs(Level.WARNING));
            assertEquals(2, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    @Test
    public void testNoCollectAfterClose() {
        LogCollector logs = startCollecting();
        logs.close();

        TEST_LOGGER.log(Level.SEVERE, "", new TestException());

        assertEquals(0, logs.getLogs().length);
        assertEquals(0, logs.getExceptions(Level.SEVERE).length);
        assertEquals(0, logs.getNumberOfLogs());
        assertEquals(0, logs.getNumberOfLogs(Level.SEVERE));
    }

    private static Set<Throwable> toSet(Throwable[] exceptions) {
        Set<Throwable> allExceptions = CollectionsEx.newIdentityHashSet(exceptions.length);
        allExceptions.addAll(Arrays.asList(exceptions));
        return allExceptions;
    }

    @Test
    public void testExtractThrowables() {
        TestException cause1 = new TestException();
        TestException cause2 = new TestException();

        TestException suppressed1 = new TestException();
        TestException suppressed2 = new TestException();
        TestException suppressed3 = new TestException();
        TestException suppressed4 = new TestException();

        TestException ex1 = new TestException();
        TestException ex2 = new TestException(cause1);
        TestException ex3 = new TestException(cause2);
        TestException ex4 = new TestException();

        ex4.addSuppressed(suppressed1);
        cause2.addSuppressed(suppressed2);
        cause2.addSuppressed(suppressed3);
        suppressed3.addSuppressed(suppressed4);

        Set<Throwable> allExceptions = CollectionsEx.newIdentityHashSet(10);
        allExceptions.add(cause1);
        allExceptions.add(cause2);
        allExceptions.add(suppressed1);
        allExceptions.add(suppressed2);
        allExceptions.add(suppressed3);
        allExceptions.add(suppressed4);
        allExceptions.add(ex1);
        allExceptions.add(ex2);
        allExceptions.add(ex3);
        allExceptions.add(ex4);

        Throwable[] extracted1 = LogCollector.extractThrowables(TestException.class, ex1, ex2, ex3, ex4, ex1);
        assertEquals(allExceptions.size(), extracted1.length);
        assertEquals(allExceptions, toSet(extracted1));

        Throwable[] extracted2 = LogCollector.extractThrowables(Exception.class, ex1, ex2, ex3, ex4);
        assertEquals(allExceptions.size(), extracted2.length);
        assertEquals(allExceptions, toSet(extracted2));

        Throwable[] extracted3 = LogCollector.extractThrowables(NullPointerException.class, ex1, ex2, ex3, ex4);
        assertEquals(0, extracted3.length);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -7948557089941739392L;

        public TestException() {
        }

        public TestException(Throwable cause) {
            super(cause);
        }
    }
}
