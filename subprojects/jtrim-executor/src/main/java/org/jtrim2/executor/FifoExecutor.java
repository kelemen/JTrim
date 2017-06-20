package org.jtrim2.executor;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks if executor implementations executing tasks in submittation order.
 * That is, this is used by {@link TaskExecutors#inOrderExecutor(TaskExecutor)}
 * to detect that an executor already executes tasks in the order they were
 * submitted to the executor.
 *
 * @see TaskExecutors#inOrderExecutor(TaskExecutor)
 * @see TaskExecutors#inOrderSimpleExecutor(TaskExecutor)
 */
final class FifoExecutor {
    // This class is tested by TaskExecutorsTest

    private static final Map<Class<?>, FifoTester> CLASS_TEST_MAP;

    static {
        CLASS_TEST_MAP = new HashMap<>();

        CLASS_TEST_MAP.put(InOrderTaskExecutor.class, AlwaysFifo.INSTANCE);
        CLASS_TEST_MAP.put(SingleThreadedExecutor.class, AlwaysFifo.INSTANCE);
        CLASS_TEST_MAP.put(InOrderTaskExecutor.class, AlwaysFifo.INSTANCE);
        CLASS_TEST_MAP.put(DelegatedTaskExecutorService.class, DelegateFifoTester.INSTANCE);
        CLASS_TEST_MAP.put(UnstoppableTaskExecutor.class, DelegateFifoTester.INSTANCE);
    }

    static boolean isFifoExecutor(TaskExecutor executor) {
        FifoTester tester = CLASS_TEST_MAP.get(executor.getClass());
        return tester != null ? tester.isFifo(executor) : false;
    }

    private static final class DelegateFifoTester implements FifoTester {
        private static final FifoTester INSTANCE = new DelegateFifoTester();

        @Override
        public boolean isFifo(TaskExecutor executor) {
            return isFifoExecutor(((DelegatedTaskExecutorService) executor).wrappedExecutor);
        }

    }

    private static final class AlwaysFifo implements FifoTester {
        private static final FifoTester INSTANCE = new AlwaysFifo();

        @Override
        public boolean isFifo(TaskExecutor executor) {
            return true;
        }
    }

    private interface FifoTester {
        public boolean isFifo(TaskExecutor executor);
    }

    private FifoExecutor() {
        throw new AssertionError();
    }
}
