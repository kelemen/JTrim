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

    private static final Map<Class<?>, FifoTester<?>> CLASS_TEST_MAP;

    static {
        CLASS_TEST_MAP = new HashMap<>();

        addTest(InOrderTaskExecutor.class, AlwaysFifo.INSTANCE);
        addTest(SingleThreadedExecutor.class, AlwaysFifo.INSTANCE);
        addTest(DelegatedTaskExecutorService.class, DelegateFifoTester.INSTANCE);
        addTest(UnstoppableTaskExecutor.class, DelegateFifoTester.INSTANCE);
        addTest(SimpleThreadPoolTaskExecutor.class, SimpleThreadPoolTaskExecutor::isFifo);
    }

    private static <E extends TaskExecutor> void addTest(
            Class<? extends E>  testedExecutorType,
            FifoTester<? super E> tester) {

        CLASS_TEST_MAP.put(testedExecutorType, tester);
    }

    static boolean isFifoExecutor(TaskExecutor executor) {
        return isFifoExecutor0(executor);
    }

    private static <E extends TaskExecutor> boolean isFifoExecutor0(E executor) {
        @SuppressWarnings("unchecked")
        FifoTester<? super E> tester = (FifoTester<? super E>) CLASS_TEST_MAP.get(executor.getClass());
        return tester != null && tester.isFifo(executor);
    }

    private static final class DelegateFifoTester implements FifoTester<DelegatedTaskExecutorService> {
        private static final FifoTester<DelegatedTaskExecutorService> INSTANCE = new DelegateFifoTester();

        @Override
        public boolean isFifo(DelegatedTaskExecutorService executor) {
            return isFifoExecutor(executor.wrappedExecutor);
        }

    }

    private static final class AlwaysFifo implements FifoTester<TaskExecutor> {
        private static final FifoTester<TaskExecutor> INSTANCE = new AlwaysFifo();

        @Override
        public boolean isFifo(TaskExecutor executor) {
            return true;
        }
    }

    private interface FifoTester<E extends TaskExecutor> {
        public boolean isFifo(E executor);
    }

    private FifoExecutor() {
        throw new AssertionError();
    }
}
