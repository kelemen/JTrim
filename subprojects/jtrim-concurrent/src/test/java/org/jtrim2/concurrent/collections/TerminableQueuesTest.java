package org.jtrim2.concurrent.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.cancel.OperationTimeoutException;
import org.jtrim2.collections.ReservablePollingQueues;
import org.jtrim2.collections.ReservedElementRef;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;

import static org.junit.Assert.*;

public class TerminableQueuesTest {
    private static <T> TerminableQueue<T> createFifoQueue(int capacity) {
        return TerminableQueues.withWrappedQueue(ReservablePollingQueues.createFifoQueue(capacity));
    }

    private static int testThreadCount() {
        return 2 * Runtime.getRuntime().availableProcessors();
    }

    private void testPutAndTakeWithMaxCapacity(
            int capacity,
            NonFullPutMethod putMethod,
            TakeAlwaysMethod takeMethod) {

        TerminableQueue<Integer> queue = createFifoQueue(capacity);

        List<AtomicReference<Integer>> taken = new ArrayList<>(capacity);
        List<Runnable> tasks = new ArrayList<>(2 * capacity);

        for (int i = 0; i < capacity; i++) {
            int addedElement = i;
            AtomicReference<Integer> resultRef = new AtomicReference<>();
            taken.add(resultRef);

            tasks.add(() -> {
                putMethod.put(queue, addedElement);
            });
            tasks.add(() -> {
                resultRef.set(takeMethod.take(queue));
            });
        }

        Tasks.runConcurrently(tasks);

        Set<Integer> expected = IntStream.range(0, capacity)
                .mapToObj(value -> value)
                .collect(Collectors.toCollection(() -> new TreeSet<>()));
        Set<Integer> result = taken.stream()
                .map(resultRef -> resultRef.get())
                .filter(value -> value != null)
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        assertEquals("result", expected, result);
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(TerminableQueues.class);
    }

    @Test(timeout = 30000)
    public void testPutAndTake1() {
        for (int i = 0; i < 5; i++) {
            for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
                for (TakeAlwaysMethod takeMethod : TakeAlwaysMethod.values()) {
                    testPutAndTakeWithMaxCapacity(1, putMethod, takeMethod);
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void testPutAndTakeMany() {
        int threadCount = testThreadCount();
        for (int i = 0; i < 5; i++) {
            for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
                for (TakeAlwaysMethod takeMethod : TakeAlwaysMethod.values()) {
                    testPutAndTakeWithMaxCapacity(threadCount, putMethod, takeMethod);
                }
            }
        }
    }

    private void testPutAndTakeWithDoubleMaxCapacity(
            int capacity,
            PutAlwaysMethod putMethod,
            TakeAlwaysMethod takeMethod) {

        TerminableQueue<Integer> queue = createFifoQueue(capacity);

        List<AtomicReference<Integer>> taken = new ArrayList<>(capacity);
        List<Runnable> tasks = new ArrayList<>(3 * capacity);
        List<Runnable> tasks2 = new ArrayList<>(capacity);

        Set<Integer> expected = new TreeSet<>();
        for (int i = 0; i < capacity; i++) {
            AtomicReference<Integer> resultRef1 = new AtomicReference<>();
            AtomicReference<Integer> resultRef2 = new AtomicReference<>();

            taken.add(resultRef1);
            taken.add(resultRef2);

            int addedElement1 = 2 * i;
            int addedElement2 = 2 * i + 1;

            expected.add(addedElement1);
            expected.add(addedElement2);

            tasks.add(() -> {
                putMethod.put(queue, addedElement1);
            });
            tasks.add(() -> {
                putMethod.put(queue, addedElement2);
            });
            tasks.add(() -> {
                resultRef1.set(takeMethod.take(queue));
            });
            tasks2.add(() -> {
                resultRef2.set(takeMethod.take(queue));
            });
        }

        Tasks.runConcurrently(tasks);
        Tasks.runConcurrently(tasks2);

        Set<Integer> result = taken.stream()
                .map(resultRef -> resultRef.get())
                .filter(value -> value != null)
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        assertEquals("result", expected, result);
    }

    @Test(timeout = 30000)
    public void testPutAndTakeOverCapacity1() {
        for (int i = 0; i < 5; i++) {
            for (PutAlwaysMethod putMethod : PutAlwaysMethod.values()) {
                for (TakeAlwaysMethod takeMethod : TakeAlwaysMethod.values()) {
                    testPutAndTakeWithDoubleMaxCapacity(1, putMethod, takeMethod);
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void testPutAndTakeOverCapacityMany() {
        int threadCount = testThreadCount();
        for (int i = 0; i < 5; i++) {
            for (PutAlwaysMethod putMethod : PutAlwaysMethod.values()) {
                for (TakeAlwaysMethod takeMethod : TakeAlwaysMethod.values()) {
                    testPutAndTakeWithDoubleMaxCapacity(threadCount, putMethod, takeMethod);
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void testTryTakeFromEmpty() {
        for (TakeNowMethod takeMethod : TakeNowMethod.values()) {
            TerminableQueue<Integer> queue = createFifoQueue(1);
            assertNull("result of empty", takeMethod.tryTake(queue));
        }
    }

    @Test(timeout = 30000)
    public void testTryTakeFromEmptyWithTimeout1() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        assertNull(queue.tryTake(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS));
    }

    @Test(timeout = 30000)
    public void testTryTakeFromEmptyWithTimeout2() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        assertNull(queue.tryTakeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS));
    }

    private void testTakeWorksAfterShutdown(
            int concurrentTakeCount,
            NonFullPutMethod putMethod,
            ShutdownMethod shutdownMethod,
            TakeAnyMethod takeMethod) {

        TerminableQueue<Integer> queue = createFifoQueue(concurrentTakeCount);
        Set<Integer> expected = new TreeSet<>();
        for (int i = 0; i < concurrentTakeCount; i++) {
            expected.add(i);
            putMethod.put(queue, i);
        }

        List<Runnable> tasks = new ArrayList<>(concurrentTakeCount + 1);
        tasks.add(() -> {
            shutdownMethod.shutdown(queue);
        });

        List<AtomicReference<Integer>> taken = new ArrayList<>(concurrentTakeCount);
        for (int i = 0; i < concurrentTakeCount; i++) {
            AtomicReference<Integer> resultRef = new AtomicReference<>();
            taken.add(resultRef);
            tasks.add(() -> {
                resultRef.set(takeMethod.take(queue));
            });
        }

        Tasks.runConcurrently(tasks);

        Set<Integer> result = taken.stream()
                .map(resultRef -> resultRef.get())
                .filter(value -> value != null)
                .collect(Collectors.toCollection(() -> new TreeSet<>()));
        assertEquals("result", expected, result);
    }

    @Test(timeout = 30000)
    public void testTakeWorksAfterShutdown() {
        int concurrentTakeCount = testThreadCount();
        for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
            for (ShutdownMethod shutdownMethod : ShutdownMethod.values()) {
                for (TakeAnyMethod takeMethod : TakeAnyMethod.values()) {
                    testTakeWorksAfterShutdown(concurrentTakeCount, putMethod, shutdownMethod, takeMethod);
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void testTakeWorksAfterShutdownSimple() {
        for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
            for (TakeAnyMethod takeMethod : TakeAnyMethod.values()) {
                TerminableQueue<Integer> queue = createFifoQueue(1);
                putMethod.put(queue, 524);
                queue.shutdown();
                assertEquals(524, (Object) takeMethod.take(queue));
            }
        }
    }

    @Test(timeout = 30000)
    public void testReservePreventsAdding() {
        for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
            for (TakeButReserveMethod takeMethod : TakeButReserveMethod.values()) {
                TerminableQueue<Integer> queue = createFifoQueue(1);
                putMethod.put(queue, 524);

                ReservedElementRef<Integer> elementRef = takeMethod.take(queue);
                assertNotNull("elementRef", elementRef);
                assertFalse("offer on reserved", queue.offer(1000));
                assertNull("tryTake on reserved", queue.tryTake());

                assertEquals(524, (Object) elementRef.element());

                elementRef.release();
                assertTrue("offer on empty", queue.offer(2000));
                assertEquals("tryTake", 2000, (Object) queue.tryTake());
            }
        }
    }

    @Test(timeout = 30000)
    public void testReleaseIsNotDoubleCounted() {
        for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
            for (TakeButReserveMethod takeMethod : TakeButReserveMethod.values()) {
                TerminableQueue<Integer> queue = createFifoQueue(3);
                putMethod.put(queue, 524);
                putMethod.put(queue, 525);
                putMethod.put(queue, 526);

                ReservedElementRef<Integer> elementRef = takeMethod.take(queue);
                assertNotNull("elementRef", elementRef);

                elementRef.release();
                elementRef.release();

                assertTrue("offer1", queue.offer(1000));
                assertFalse("offer2", queue.offer(1001));

                assertEquals("take 1", 525, (Object) queue.tryTake());
                assertEquals("take 2", 526, (Object) queue.tryTake());
                assertEquals("take 3", 1000, (Object) queue.tryTake());
            }
        }
    }

    @Test(timeout = 30000)
    public void testAddingAfterShutdownFails() {
        for (ShutdownMethod shutdownMethod : ShutdownMethod.values()) {
            for (NonFullPutMethod putMethod : NonFullPutMethod.values()) {
                TerminableQueue<Integer> queue = createFifoQueue(1);
                shutdownMethod.shutdown(queue);

                try {
                    putMethod.put(queue, 524);
                    throw new AssertionError("Expected TerminatedQueueException");
                } catch (TerminatedQueueException ex) {
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void testTakeAfterShutdownFailsOnEmpty() {
        for (ShutdownMethod shutdownMethod : ShutdownMethod.values()) {
            for (TakeNowMethod takeMethod : TakeNowMethod.values()) {
                TerminableQueue<Integer> queue = createFifoQueue(1);
                shutdownMethod.shutdown(queue);

                try {
                    takeMethod.tryTake(queue);
                    throw new AssertionError("Expected TerminatedQueueException");
                } catch (TerminatedQueueException ex) {
                }
            }
        }
    }

    private void expectCanellation(Runnable task) {
        try {
            task.run();
            throw new AssertionError("Expected OperationCanceledException");
        } catch (OperationCanceledException ex) {
            assertFalse("Not TerminatedQueueException", ex instanceof TerminatedQueueException);
            assertFalse("Not OperationTimeoutException", ex instanceof OperationTimeoutException);
        }
    }

    @Test(timeout = 30000)
    public void testPutDetectsImmediateCancellation() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        expectCanellation(() -> {
            queue.put(Cancellation.CANCELED_TOKEN, 54);
        });

        expectCanellation(() -> {
            queue.put(Cancellation.CANCELED_TOKEN, 55, 60, TimeUnit.SECONDS);
        });

        assertEquals(53, (Object) queue.tryTake());
        assertNull("take on empty", queue.tryTake());
    }

    private void runWithDelayedCancellation(CancelableTask task) {
        AtomicReference<Throwable> resultEx = new AtomicReference<>();

        CancellationSource cs = Cancellation.createCancellationSource();
        Runnable taskWrapper = () -> {
            try {
                task.execute(cs.getToken());
            } catch (Exception ex) {
                resultEx.set(ex);
            }
        };
        Runnable cancelerTask = () -> {
            CancelableWaits.sleep(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS);
            cs.getController().cancel();
        };

        Tasks.runConcurrently(taskWrapper, cancelerTask);

        ExceptionHelper.rethrowIfNotNull(resultEx.get());
    }

    @Test(timeout = 30000)
    public void testPutDetectsDelayedCancellation() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> queue.put(cancelToken, 54));
        });

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.put(cancelToken, 55, 60, TimeUnit.SECONDS);
            });
        });

        assertEquals(53, (Object) queue.tryTake());
        assertNull("take on empty", queue.tryTake());
    }

    @Test(timeout = 30000)
    public void testTakeDetectsImmediateCancellation() {
        TerminableQueue<Integer> queue = createFifoQueue(1);

        expectCanellation(() -> {
            queue.take(Cancellation.CANCELED_TOKEN);
        });

        expectCanellation(() -> {
            queue.tryTake(Cancellation.CANCELED_TOKEN, 60, TimeUnit.SECONDS);
        });

        expectCanellation(() -> {
            queue.takeButKeepReserved(Cancellation.CANCELED_TOKEN);
        });

        expectCanellation(() -> {
            queue.tryTakeButKeepReserved(Cancellation.CANCELED_TOKEN, 60, TimeUnit.SECONDS);
        });
    }

    @Test(timeout = 30000)
    public void testTakeDetectsDelayedCancellation() {
        TerminableQueue<Integer> queue = createFifoQueue(1);

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.take(cancelToken);
            });
        });

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.tryTake(cancelToken, 60, TimeUnit.SECONDS);
            });
        });

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.takeButKeepReserved(cancelToken);
            });
        });

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.tryTakeButKeepReserved(cancelToken, 60, TimeUnit.SECONDS);
            });
        });
    }

    @Test(timeout = 30000)
    public void testPutDetectsTimeout() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        assertFalse("put", queue.put(Cancellation.UNCANCELABLE_TOKEN, 54, 5, TimeUnit.MILLISECONDS));

        assertEquals(53, (Object) queue.tryTake());
        assertNull("take on empty", queue.tryTake());
    }

    @Test(timeout = 30000)
    public void testShutdownAndWaitDetectsTimeout() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        assertFalse("shutdown",
                queue.shutdownAndTryWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS));

        assertEquals(53, (Object) queue.tryTake());

        try {
            assertNull("take on empty", queue.tryTake());
            throw new AssertionError("Expected TerminatedQueueException");
        } catch (TerminatedQueueException ex) {
        }
    }

    @Test(timeout = 30000)
    public void testShutdownAndWaitDetectsImmediateCancellation() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        expectCanellation(() -> {
            queue.shutdownAndWaitUntilEmpty(Cancellation.CANCELED_TOKEN);
        });

        expectCanellation(() -> {
            queue.shutdownAndTryWaitUntilEmpty(Cancellation.CANCELED_TOKEN, 60, TimeUnit.SECONDS);
        });

        assertEquals(53, (Object) queue.tryTake());

        try {
            assertNull("take on empty", queue.tryTake());
            throw new AssertionError("Expected TerminatedQueueException");
        } catch (TerminatedQueueException ex) {
        }
    }

    @Test(timeout = 30000)
    public void testShutdownAndWaitDetectsDelayedCancellation() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.shutdownAndWaitUntilEmpty(cancelToken);
            });
        });

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.shutdownAndTryWaitUntilEmpty(cancelToken, 60, TimeUnit.SECONDS);
            });
        });

        assertEquals(53, (Object) queue.tryTake());

        try {
            assertNull("take on empty", queue.tryTake());
            throw new AssertionError("Expected TerminatedQueueException");
        } catch (TerminatedQueueException ex) {
        }
    }

    @Test(timeout = 30000)
    public void testShutdownAndWaitDoesNotReturnBeforeReleaseNoTimeout() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        ReservedElementRef<Integer> elementRef = queue.takeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN);

        expectCanellation(() -> {
            runWithDelayedCancellation(cancelToken -> {
                queue.shutdownAndWaitUntilEmpty(cancelToken);
            });
        });

        elementRef.release();
        queue.shutdownAndWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 30000)
    public void testShutdownAndWaitDoesNotReturnBeforeReleaseWithTimeout() {
        TerminableQueue<Integer> queue = createFifoQueue(1);
        queue.put(Cancellation.UNCANCELABLE_TOKEN, 53);

        ReservedElementRef<Integer> elementRef = queue.takeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN);

        assertFalse("shutdownAndTryWaitUntilEmpty before release",
                queue.shutdownAndTryWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS));

        elementRef.release();
        assertTrue(
                "shutdownAndTryWaitUntilEmpty after release",
                queue.shutdownAndTryWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS));
    }

    private enum ShutdownMethod {
        SHUTDOWN {
            @Override
            public void shutdown(TerminableQueue<?> queue) {
                queue.shutdown();
            }
        },
        SHUTDOWN_WAIT {
            @Override
            public void shutdown(TerminableQueue<?> queue) {
                queue.shutdownAndWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN);
            }
        },
        SHUTDOWN_TRY_WAIT {
            @Override
            public void shutdown(TerminableQueue<?> queue) {
                queue.shutdownAndTryWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS);
            }
        };

        public abstract void shutdown(TerminableQueue<?> queue);
    }

    private enum TakeButReserveMethod {
        TAKE_RESERVE {
            @Override
            public <T> ReservedElementRef<T> take(TerminableQueue<T> queue) {
                ReservedElementRef<T> result = queue.takeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN);
                assertNotNull("elementRef", result);
                return result;
            }
        },
        TAKE_RESERVE_TIMEOUT {
            @Override
            public <T> ReservedElementRef<T> take(TerminableQueue<T> queue) {
                ReservedElementRef<T> result = queue
                        .tryTakeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS);
                assertNotNull("elementRef", result);
                return result;
            }
        },
        TAKE_NOW_RESERVE {
            @Override
            public <T> ReservedElementRef<T> take(TerminableQueue<T> queue) {
                ReservedElementRef<T> result = queue.tryTakeButKeepReserved();
                assertNotNull("elementRef", result);
                return result;
            }
        };

        public abstract <T> ReservedElementRef<T> take(TerminableQueue<T> queue);
    }

    private enum TakeAnyMethod {
        TAKE_NOW {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return TakeNowMethod.TAKE_NOW.tryTake(queue);
            }
        },
        TAKE_NOW_RESERVE {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return TakeNowMethod.TAKE_NOW_RESERVE.tryTake(queue);
            }
        },
        TAKE {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return TakeAlwaysMethod.TAKE.take(queue);
            }
        },
        TAKE_TIMEOUT {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return TakeAlwaysMethod.TAKE_TIMEOUT.take(queue);
            }
        },
        TAKE_RESERVE {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return TakeAlwaysMethod.TAKE_RESERVE.take(queue);
            }
        },
        TAKE_RESERVE_TIMEOUT {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return TakeAlwaysMethod.TAKE_RESERVE_TIMEOUT.take(queue);
            }
        };

        public abstract <T> T take(TerminableQueue<T> queue);
    }

    private enum TakeNowMethod {
        TAKE_NOW {
            @Override
            public <T> T tryTake(TerminableQueue<T> queue) {
                return queue.tryTake();
            }
        },
        TAKE_NOW_RESERVE {
            @Override
            public <T> T tryTake(TerminableQueue<T> queue) {
                ReservedElementRef<T> resultRef = queue.tryTakeButKeepReserved();
                if (resultRef == null) {
                    return null;
                }
                resultRef.release();
                return resultRef.element();
            }
        },
        TAKE_NOW_WITH_TIMEOUT {
            @Override
            public <T> T tryTake(TerminableQueue<T> queue) {
                return queue.tryTake(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS);
            }
        },
        TAKE_NOW_RESERVE_WITH_TIMEOUT {
            @Override
            public <T> T tryTake(TerminableQueue<T> queue) {
                ReservedElementRef<T> resultRef = queue
                        .tryTakeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.MILLISECONDS);
                if (resultRef == null) {
                    return null;
                }
                resultRef.release();
                return resultRef.element();
            }
        };

        public abstract <T> T tryTake(TerminableQueue<T> queue);
    }

    private enum TakeAlwaysMethod {
        TAKE {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return queue.take(Cancellation.UNCANCELABLE_TOKEN);
            }
        },
        TAKE_TIMEOUT {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                return queue.tryTake(Cancellation.UNCANCELABLE_TOKEN, 60, TimeUnit.SECONDS);
            }
        },
        TAKE_RESERVE {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                ReservedElementRef<T> resultRef = queue.takeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN);
                assertNotNull("elementRef", resultRef);
                resultRef.release();
                return resultRef.element();
            }
        },
        TAKE_RESERVE_TIMEOUT {
            @Override
            public <T> T take(TerminableQueue<T> queue) {
                ReservedElementRef<T> resultRef = queue
                        .tryTakeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN, 60, TimeUnit.SECONDS);
                assertNotNull("elementRef", resultRef);
                resultRef.release();
                return resultRef.element();
            }
        };

        public abstract <T> T take(TerminableQueue<T> queue);
    }

    public enum PutAlwaysMethod {
        PUT {
            @Override
            public <T> void put(TerminableQueue<T> queue, T element) {
                queue.put(Cancellation.UNCANCELABLE_TOKEN, element);
            }
        },
        TRY_PUT {
            @Override
            public <T> void put(TerminableQueue<T> queue, T element) {
                boolean succeed = queue.put(Cancellation.UNCANCELABLE_TOKEN, element, 60, TimeUnit.SECONDS);
                assertTrue("tryPut", succeed);
            }
        };

        public abstract <T> void put(TerminableQueue<T> queue, T element);
    }

    public enum NonFullPutMethod {
        PUT {
            @Override
            public <T> void put(TerminableQueue<T> queue, T element) {
                queue.put(Cancellation.UNCANCELABLE_TOKEN, element);
            }
        },
        TRY_PUT {
            @Override
            public <T> void put(TerminableQueue<T> queue, T element) {
                boolean succeed = queue.put(Cancellation.UNCANCELABLE_TOKEN, element, 5, TimeUnit.MILLISECONDS);
                assertTrue("tryPut", succeed);
            }
        },
        OFFER {
            @Override
            public <T> void put(TerminableQueue<T> queue, T element) {
                boolean succeed = queue.offer(element);
                assertTrue("offer", succeed);
            }
        };

        public abstract <T> void put(TerminableQueue<T> queue, T element);
    }
}
