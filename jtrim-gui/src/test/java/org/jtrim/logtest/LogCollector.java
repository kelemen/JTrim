package org.jtrim.logtest;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class LogCollector implements AutoCloseable {
    public static LogCollector startCollecting() {
        return startCollecting("org.jtrim");
    }

    public static LogCollector startCollecting(String loggerName) {
        return new LogCollector(loggerName);
    }

    public static void extractThrowables(
            Class<? extends Throwable> cl,
            Throwable[] logs,
            Set<Throwable> result) {

        for (Throwable log: logs) {
            if (log != null) {
                extractThrowables(cl, new Throwable[]{log.getCause()}, result);
                extractThrowables(cl, log.getSuppressed(), result);
                if (cl.isAssignableFrom(log.getClass())) {
                    result.add(log);
                }
            }
        }
    }

    public static Throwable[] extractThrowables(
            Class<? extends Throwable> cl,
            Throwable... logs) {
        ExceptionHelper.checkNotNullArgument(cl, "cl");

        Set<Throwable> result = CollectionsEx.newIdentityHashSet(logs.length);
        extractThrowables(cl, logs, result);

        return result.toArray(new Throwable[result.size()]);
    }

    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    private final CollectorHandler handler;

    private LogCollector(String loggerName) {
        ExceptionHelper.checkNotNullArgument(loggerName, "loggerName");

        this.handler = new CollectorHandler();
        this.logger = Logger.getLogger(loggerName);
        this.logger.addHandler(handler);
    }

    public int getNumberOfLogs() {
        return handler.getNumberOfLogs();
    }

    public int getNumberOfLogs(Level level) {
        return handler.getNumberOfLogs(level);
    }

    public LogRecord[] getLogs() {
        return handler.getLogs();
    }

    public Throwable[] getExceptions(Level level) {
        return handler.getExceptions(level);
    }

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
