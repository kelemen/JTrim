package org.jtrim2.logs;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jtrim2.collections.CollectionsEx;

/**
 * Defines a class which is able to collect logs written to a
 * {@code java.util.logging.Logger}. This class is generally useful when testing
 * if a particular code emitted log message or not.
 * <P>
 * To instantiate this class call the {@link #startCollecting(String)} factory
 * method.
 * <P>
 * <B>Warning</B>: Instances of this class need to be closed in order to
 * unregister log handlers. Failing to call the {@link #close() close} method
 * will cause a memory leak.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safe to be used by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I> unless otherwise
 * noted.
 *
 * @see #startCollecting(String)
 */
public final class LogCollector implements AutoCloseable {
    /**
     * Creats a new {@code LogCollector} which immediately starts listening
     * for logs passed to the specified logger.
     * <P>
     * This method will add a log handler to the specified logger and to
     * remove this handler you have to call the {@link #close() close} method
     * of the returned {@code LogCollector}.
     *
     * @param loggerName the name of the logger whose logs are to be collected.
     *   This argument cannot be {@code null}.
     * @return the {@code LogCollector} which will collect logs passed to the
     *   given logger. This method never returns {@code null}.
     */
    public static LogCollector startCollecting(String loggerName) {
        return new LogCollector(loggerName);
    }

    private static void extractThrowables(
            Class<? extends Throwable> cl,
            Throwable[] exceptions,
            Set<Throwable> result) {

        for (Throwable ex: exceptions) {
            if (ex != null) {
                extractThrowables(cl, new Throwable[]{ex.getCause()}, result);
                extractThrowables(cl, ex.getSuppressed(), result);
                if (cl.isAssignableFrom(ex.getClass())) {
                    result.add(ex);
                }
            }
        }
    }

    /**
     * Collects all the throwable extending the given class from the given
     * {@code Throwable} instances. This method will also find exceptions within
     * the causes and the suppressed exceptions of the passed exceptions
     * (recursively).
     *
     * @param cl the class of the exceptions to be returned. This method will
     *   also return exceptions with exactly the same type. That is, calling
     *   this method with {@code Throwable.class} will return every possible
     *   exception. This argument cannot be {@code null}.
     * @param exceptions the exceptions to be searched for the specified type
     *   of exceptions. This array cannot be {@code null} but its elements
     *   can be {@code null}, {@code null} elements will be ignored.
     * @return the exceptions implementing the given type. The returned array
     *   will not contain the same (reference comparison) exception multiple
     *   times. This method never returns {@code null}.
     */
    public static Throwable[] extractThrowables(
            Class<? extends Throwable> cl,
            Throwable... exceptions) {
        Objects.requireNonNull(cl, "cl");
        Objects.requireNonNull(exceptions, "exceptions");

        Set<Throwable> result = CollectionsEx.newIdentityHashSet(exceptions.length);
        extractThrowables(cl, exceptions, result);

        return result.toArray(new Throwable[result.size()]);
    }

    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    private final CollectorHandler handler;

    private LogCollector(String loggerName) {
        Objects.requireNonNull(loggerName, "loggerName");

        this.handler = new CollectorHandler();
        this.logger = Logger.getLogger(loggerName);
        this.logger.addHandler(handler);
    }

    /**
     * Returns the number of log records collected. This is effectively
     * equivalent to {@code getLogs().length} but is more efficient.
     *
     * @return the number of log records collected. This method always returns
     *   a value greater than or equal to zero.
     */
    public int getNumberOfLogs() {
        return handler.getNumberOfLogs();
    }

    /**
     * Returns the number of log records collected having the specified log
     * level. The level must have an exact match, so passing
     * {@code Level.SEVERE} will not count {@code Level.WARNING} logs.
     *
     * @param level the level of the logs to be counted. This argument cannot
     *   be {@code null}.
     * @return the number of log records collected having the specified log
     *   level. This method always returns a value greater than or equal to
     *   zero.
     */
    public int getNumberOfLogs(Level level) {
        return handler.getNumberOfLogs(level);
    }

    /**
     * Returns all the collected logs. This method returns the log records in
     * the order they were collected.
     *
     * @return all the collected logs. This method never returns {@code null}.
     */
    public LogRecord[] getLogs() {
        return handler.getLogs();
    }

    /**
     * Returns all the exceptions attached to the collected log records having
     * the specified log level. The exceptions are extracted from the
     * {@code thrown} property of the log records. The level must have an exact
     * match, so passing {@code Level.SEVERE} will not consider
     * {@code Level.WARNING} logs.
     *
     * @param level the level of the logs to be checked. This argument cannot
     *   be {@code null}.
     * @return all the exceptions attached to the collected log records having
     *   the specified log level. This method never returns {@code null}.
     */
    public Throwable[] getExceptions(Level level) {
        return handler.getExceptions(level);
    }

    /**
     * Removes the log handler the {@code LogCollector} uses and stops
     * collecting logs. Logging after this method call will have no effect on
     * this {@code LogCollector}.
     * <P>
     * This method is idempotent, so calling it multiple times has no further
     * effect.
     */
    @Override
    public void close() {
        handler.close();
        logger.removeHandler(handler);
    }

    private static class CollectorHandler extends Handler {
        private final Lock mainLock;
        private final List<LogRecord> records;
        private boolean closed;

        public CollectorHandler() {
            this.mainLock = new ReentrantLock();
            this.records = new LinkedList<>();
            this.closed = false;
        }

        @Override
        public void publish(LogRecord record) {
            mainLock.lock();
            try {
                if (!closed) {
                    records.add(record);
                }
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public void flush() {
        }

        public int getNumberOfLogs() {
            mainLock.lock();
            try {
                return records.size();
            } finally {
                mainLock.unlock();
            }
        }

        public int getNumberOfLogs(Level level) {
            Objects.requireNonNull(level, "level");

            int result = 0;
            mainLock.lock();
            try {
                for (LogRecord record: records) {
                    if (level.equals(record.getLevel())) {
                        result++;
                    }
                }
            } finally {
                mainLock.unlock();
            }
            return result;
        }

        public LogRecord[] getLogs() {
            mainLock.lock();
            try {
                return records.toArray(new LogRecord[records.size()]);
            } finally {
                mainLock.unlock();
            }
        }

        public Throwable[] getExceptions(Level level) {
            Objects.requireNonNull(level, "level");

            List<Throwable> errors = new LinkedList<>();

            mainLock.lock();
            try {
                for (LogRecord record: records) {
                    if (level.equals(record.getLevel())) {
                        Throwable error = record.getThrown();
                        if (error != null) {
                            errors.add(error);
                        }
                    }
                }
            } finally {
                mainLock.unlock();
            }
            return errors.toArray(new Throwable[errors.size()]);
        }

        @Override
        public void close() {
            mainLock.lock();
            try {
                closed = true;
            } finally {
                mainLock.unlock();
            }
        }
    }
}
