package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.executor.MonitorableTaskExecutorService;
import org.jtrim2.executor.ThreadPoolBuilder;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.RepeatTest;
import org.jtrim2.testutils.RepeatTestRule;
import org.jtrim2.testutils.UnsafeRunnable;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncProducerRefTest {
    @Rule
    public final RepeatTestRule repeatRule = new RepeatTestRule();

    @SuppressWarnings("unchecked")
    private <T> AsyncElementSink<T> mockSink() {
        return (AsyncElementSink<T>) (AsyncElementSink<?>) mock(AsyncElementSink.class);
    }

    @SuppressWarnings("unchecked")
    private <T> ElementConsumer<T> mockConsumer() {
        return (ElementConsumer<T>) (ElementConsumer<?>) mock(ElementConsumer.class);
    }

    @Test
    public void testBasicFactories() {
        AsyncElementSink<String> sink = mockSink();
        TestProducerFactory producerFactory = new TestProducerFactory();

        AsyncProducerRef<String> ref = new AsyncProducerRef<>(sink, producerFactory);

        assertSame(producerFactory, ref.getProducerFactory());
        SeqProducer<String> producer = ref.newSeqProducer();

        List<MockProducer> createdProducers = producerFactory.getCreatedProducers();
        createdProducers.forEach(mock -> verifyNoInteractions(mock.getMock()));

        assertEquals(Arrays.asList(producer), CollectionsEx.mapToNewList(createdProducers, MockProducer::getProducer));

        verifyNoInteractions(sink);
    }

    private void testParallelFactories(int threadCount) throws Exception {
        AsyncElementSink<String> sink = mockSink();
        TestProducerFactory producerFactory = new TestProducerFactory();

        AsyncProducerRef<String> ref = new AsyncProducerRef<>(sink, producerFactory);

        MonitorableTaskExecutorService executor = ThreadPoolBuilder.create("testParallelFactories", config -> {
            config.setMaxThreadCount(Math.max(1, threadCount - 1));
        });

        CancellationToken cancelToken = Cancellation.createCancellationSource().getToken();
        ElementConsumer<String> consumer = mockConsumer();

        try {
            SeqGroupProducer<String> producer = ref.newSeqGroupProducer(executor, threadCount);

            CountDownLatch workerLatch = new CountDownLatch(threadCount);
            producer.transferAll(cancelToken, (consumerCancelToken, seqProducer) -> {
                workerLatch.countDown();
                workerLatch.await();

                seqProducer.transferAll(consumerCancelToken, consumer);
            });
        } finally {
            GenericExecutorServiceTests.shutdownTestExecutor(executor);
        }

        producerFactory.verifyCreatedWorkers(threadCount, cancelToken, consumer);
    }

    @Test(timeout = 10000)
    @RepeatTest(20)
    public void testParallelFactoriesSingleThread() throws Exception {
        testParallelFactories(1);
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testParallelFactoriesManyThreads() throws Exception {
        testParallelFactories(JTrimTests.getThreadCount());
    }

    private void testFailProcess(boolean failSyncThread) throws Exception {
        Thread syncThread = Thread.currentThread();
        Exception expectedException = new Exception("testFailBackground");

        AsyncElementSink<String> sink = mockSink();
        TestProducerFactory producerFactory = new TestProducerFactory(() -> {
            if ((Thread.currentThread() == syncThread) == failSyncThread) {
                throw expectedException;
            }
        });

        AsyncProducerRef<String> ref = new AsyncProducerRef<>(sink, producerFactory);

        MonitorableTaskExecutorService executor = ThreadPoolBuilder.create("testFailSyncThread", config -> {
            config.setMaxThreadCount(2);
        });

        CancellationToken cancelToken = Cancellation.createCancellationSource().getToken();
        ElementConsumer<String> consumer = mockConsumer();

        try {
            SeqGroupProducer<String> producer = ref.newSeqGroupProducer(executor, 2);

            try {
                producer.transferAll(cancelToken, (consumerCancelToken, seqProducer) -> {
                    seqProducer.transferAll(consumerCancelToken, consumer);
                });
                fail("Expected failure.");
            } catch (Exception ex) {
                assertSame(expectedException, ex);
            }
        } finally {
            GenericExecutorServiceTests.shutdownTestExecutor(executor);
        }

        verifyNoInteractions(sink);
        producerFactory.verifyCreatedWorkers(2, cancelToken, consumer);
    }

    @Test(timeout = 10000)
    public void testFailSyncThread() throws Exception {
        testFailProcess(true);
    }

    @Test(timeout = 10000)
    public void testFailBackgroundThread() throws Exception {
        testFailProcess(false);
    }

    @SuppressWarnings("unchecked")
    private static <T> ElementConsumer<T> anyConsumer() {
        return (ElementConsumer<T>) (ElementConsumer<?>) any(ElementConsumer.class);
    }

    private static final class TestProducerFactory implements Supplier<SeqProducer<String>> {
        private final UnsafeRunnable transferTask;
        private final List<MockProducer> producers;

        public TestProducerFactory() {
            this(() -> { });
        }

        public TestProducerFactory(UnsafeRunnable transferTask) {
            this.transferTask = Objects.requireNonNull(transferTask, "transferTask");
            this.producers = Collections.synchronizedList(new ArrayList<>());
        }

        @Override
        public SeqProducer<String> get() {
            MockProducer mockProducer = new MockProducer();
            try {
                doAnswer(invocation -> {
                    mockProducer.setThreadId(Thread.currentThread().getId());
                    transferTask.run();
                    return null;
                }).when(mockProducer.getMock())
                        .transferAll(any(CancellationToken.class), anyConsumer());

                producers.add(mockProducer);
            } catch (Exception ex) {
                throw ExceptionHelper.throwUnchecked(ex);
            }
            return mockProducer.getProducer();
        }

        public List<MockProducer> getCreatedProducers() {
            synchronized (producers) {
                return new ArrayList<>(producers);
            }
        }

        public void verifyCreatedWorkers(
                int threadCount,
                CancellationToken cancelToken,
                ElementConsumer<String> consumer) throws Exception {

            List<MockProducer> createdProducers = getCreatedProducers();
            assertEquals("producer count", threadCount, createdProducers.size());

            Set<Long> receivedThreadIds = new HashSet<>();
            for (MockProducer createdProducer: createdProducers) {
                verify(createdProducer.mock)
                        .transferAll(same(cancelToken), same(consumer));
                receivedThreadIds.add(createdProducer.getThreadId());
            }
            assertEquals("thread count", threadCount, receivedThreadIds.size());
            assertTrue("current thread must work", receivedThreadIds.contains(Thread.currentThread().getId()));
        }
    }

    private static <T> SeqProducer<T> wrap(SeqProducer<? extends T> producer) {
        return producer::transferAll;
    }

    private static final class MockProducer {
        private final SeqProducer<String> mock;
        private final SeqProducer<String> producer;
        private volatile long threadId;

        @SuppressWarnings("unchecked")
        public MockProducer() {
            this.mock = (SeqProducer<String>) (SeqProducer<?>) mock(SeqProducer.class);
            // We need to do this indirection so that default method implementations are kept.
            this.producer = wrap(this.mock);
        }

        public SeqProducer<String> getProducer() {
            return producer;
        }

        public SeqProducer<String> getMock() {
            return mock;
        }

        public void setThreadId(long threadId) {
            this.threadId = threadId;
        }

        public long getThreadId() {
            return threadId;
        }
    }
}
