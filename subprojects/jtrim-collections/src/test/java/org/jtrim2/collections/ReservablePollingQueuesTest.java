package org.jtrim2.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.jtrim2.collections.ReservablePollingQueues.*;
import static org.jtrim2.collections.SerializationHelper.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class ReservablePollingQueuesTest {
    public static class ZeroCapacityQueueTestFifo1 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestFifo1() {
            super(() -> createFifoQueue(0));
        }
    }

    public static class ZeroCapacityQueueTestFifo2 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestFifo2() {
            super(() -> createFifoQueue(0, 0));
        }
    }

    public static class ZeroCapacityQueueTestLifo1 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestLifo1() {
            super(() -> createLifoQueue(0));
        }
    }

    public static class ZeroCapacityQueueTestLifo2 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestLifo2() {
            super(() -> createLifoQueue(0, 0));
        }
    }

    public static class ZeroCapacityQueueTestNaturalOrder2 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestNaturalOrder2() {
            super(() -> createOrderedQueue(0, null));
        }
    }

    public static class ZeroCapacityQueueTestNaturalOrder3 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestNaturalOrder3() {
            super(() -> createOrderedQueue(0, 0, null));
        }
    }

    public static class ZeroCapacityQueueTestCustomOrder2 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestCustomOrder2() {
            super(() -> createOrderedQueue(0, Comparator.reverseOrder()));
        }
    }

    public static class ZeroCapacityQueueTestCustomOrder3 extends AbstractZeroCapacityQueueTest {
        public ZeroCapacityQueueTestCustomOrder3() {
            super(() -> createOrderedQueue(0, 0, Comparator.reverseOrder()));
        }
    }

    public static class FifoQueueTest1Capacity0 extends AbstractFifoQueueTest1 {
        public FifoQueueTest1Capacity0() {
            super(0);
        }
    }

    public static class FifoQueueTest2Capacity0 extends AbstractFifoQueueTest2 {
        public FifoQueueTest2Capacity0() {
            super(0, 0);
        }
    }

    public static class FifoQueueTest1Capacity1 extends AbstractFifoQueueTest1 {
        public FifoQueueTest1Capacity1() {
            super(1);
        }
    }

    public static class FifoQueueTest2Capacity1 extends AbstractFifoQueueTest2 {
        public FifoQueueTest2Capacity1() {
            super(1, 0);
        }
    }

    public static class FifoQueueTest2Capacity1InitMax extends AbstractFifoQueueTest2 {
        public FifoQueueTest2Capacity1InitMax() {
            super(1, 1);
        }
    }

    public static class FifoQueueTest1Capacity5 extends AbstractFifoQueueTest1 {
        public FifoQueueTest1Capacity5() {
            super(5);
        }
    }

    public static class FifoQueueTest2Capacity5 extends AbstractFifoQueueTest2 {
        public FifoQueueTest2Capacity5() {
            super(5, 0);
        }
    }

    public static class FifoQueueTest2Capacity5InitMax extends AbstractFifoQueueTest2 {
        public FifoQueueTest2Capacity5InitMax() {
            super(5, 5);
        }
    }

    public abstract static class AbstractFifoQueueTest1 extends AbstractFifoQueueTest {
        public AbstractFifoQueueTest1(int maxCapacity) {
            super(maxCapacity, () -> createFifoQueue(maxCapacity));
        }
    }

    public abstract static class AbstractFifoQueueTest2 extends AbstractFifoQueueTest {
        public AbstractFifoQueueTest2(int maxCapacity, int initialCapacity) {
            super(maxCapacity, () -> createFifoQueue(maxCapacity, initialCapacity));
        }
    }

    public abstract static class AbstractFifoQueueTest extends AbstractCustomCapacityQueueTest {
        public AbstractFifoQueueTest(int maxCapacity, Supplier<ReservablePollingQueue<Integer>> queueFactory) {
            super(maxCapacity, queueFactory, Function.identity());
        }
    }

    public static class LifoQueueTest1Capacity0 extends AbstractLifoQueueTest1 {
        public LifoQueueTest1Capacity0() {
            super(0);
        }
    }

    public static class LifoQueueTest2Capacity0 extends AbstractLifoQueueTest2 {
        public LifoQueueTest2Capacity0() {
            super(0, 0);
        }
    }

    public static class LifoQueueTest1Capacity1 extends AbstractLifoQueueTest1 {
        public LifoQueueTest1Capacity1() {
            super(1);
        }
    }

    public static class LifoQueueTest2Capacity1 extends AbstractLifoQueueTest2 {
        public LifoQueueTest2Capacity1() {
            super(1, 0);
        }
    }

    public static class LifoQueueTest2Capacity1InitMax extends AbstractLifoQueueTest2 {
        public LifoQueueTest2Capacity1InitMax() {
            super(1, 1);
        }
    }

    public static class LifoQueueTest1Capacity5 extends AbstractLifoQueueTest1 {
        public LifoQueueTest1Capacity5() {
            super(5);
        }
    }

    public static class LifoQueueTest2Capacity5 extends AbstractLifoQueueTest2 {
        public LifoQueueTest2Capacity5() {
            super(5, 0);
        }
    }

    public static class LifoQueueTest2Capacity5InitMax extends AbstractLifoQueueTest2 {
        public LifoQueueTest2Capacity5InitMax() {
            super(5, 5);
        }
    }

    public abstract static class AbstractLifoQueueTest1 extends AbstractLifoQueueTest {
        public AbstractLifoQueueTest1(int maxCapacity) {
            super(maxCapacity, () -> createLifoQueue(maxCapacity));
        }
    }

    public abstract static class AbstractLifoQueueTest2 extends AbstractLifoQueueTest {
        public AbstractLifoQueueTest2(int maxCapacity, int initialCapacity) {
            super(maxCapacity, () -> createLifoQueue(maxCapacity, initialCapacity));
        }
    }

    public abstract static class AbstractLifoQueueTest extends AbstractCustomCapacityQueueTest {
        public AbstractLifoQueueTest(int maxCapacity, Supplier<ReservablePollingQueue<Integer>> queueFactory) {
            super(maxCapacity, queueFactory, src -> {
                List<Integer> result = new ArrayList<>(src);
                Collections.reverse(result);
                return result;
            });
        }
    }

    public static class OrderedQueueTest1Capacity0 extends AbstractOrderedQueueTest1 {
        public OrderedQueueTest1Capacity0() {
            super(0, null);
        }
    }

    public static class OrderedQueueTest2Capacity0 extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity0() {
            super(0, 0, null);
        }
    }

    public static class OrderedQueueTest1Capacity1 extends AbstractOrderedQueueTest1 {
        public OrderedQueueTest1Capacity1() {
            super(1, null);
        }
    }

    public static class OrderedQueueTest2Capacity1 extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity1() {
            super(1, 0, null);
        }
    }

    public static class OrderedQueueTest2Capacity1InitMax extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity1InitMax() {
            super(1, 1, null);
        }
    }

    public static class OrderedQueueTest1Capacity5 extends AbstractOrderedQueueTest1 {
        public OrderedQueueTest1Capacity5() {
            super(5, null);
        }
    }

    public static class OrderedQueueTest2Capacity5 extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity5() {
            super(5, 0, null);
        }
    }

    public static class OrderedQueueTest2Capacity5InitMax extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity5InitMax() {
            super(5, 5, null);
        }
    }

    public static class OrderedQueueTest1Capacity0CustomOrder extends AbstractOrderedQueueTest1 {
        public OrderedQueueTest1Capacity0CustomOrder() {
            super(0, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest2Capacity0CustomOrder extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity0CustomOrder() {
            super(0, 0, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest1Capacity1CustomOrder extends AbstractOrderedQueueTest1 {
        public OrderedQueueTest1Capacity1CustomOrder() {
            super(1, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest2Capacity1CustomOrder extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity1CustomOrder() {
            super(1, 0, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest2Capacity1InitMaxCustomOrder extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity1InitMaxCustomOrder() {
            super(1, 1, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest1Capacity5CustomOrder extends AbstractOrderedQueueTest1 {
        public OrderedQueueTest1Capacity5CustomOrder() {
            super(5, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest2Capacity5CustomOrder extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity5CustomOrder() {
            super(5, 0, Comparator.reverseOrder());
        }
    }

    public static class OrderedQueueTest2Capacity5InitMaxCustomOrder extends AbstractOrderedQueueTest2 {
        public OrderedQueueTest2Capacity5InitMaxCustomOrder() {
            super(5, 5, Comparator.reverseOrder());
        }
    }

    public abstract static class AbstractOrderedQueueTest1 extends AbstractOrderedQueueTest {
        public AbstractOrderedQueueTest1(int maxCapacity, Comparator<Integer> comparator) {
            super(maxCapacity, comparator, () -> createOrderedQueue(maxCapacity, comparator));
        }
    }

    public abstract static class AbstractOrderedQueueTest2 extends AbstractOrderedQueueTest {
        public AbstractOrderedQueueTest2(int maxCapacity, int initialCapacity, Comparator<Integer> comparator) {
            super(maxCapacity, comparator, () -> createOrderedQueue(maxCapacity, initialCapacity, comparator));
        }
    }

    public abstract static class AbstractOrderedQueueTest extends AbstractCustomCapacityQueueTest {
        public AbstractOrderedQueueTest(
                int maxCapacity,
                Comparator<Integer> comparator,
                Supplier<ReservablePollingQueue<Integer>> queueFactory) {

            super(maxCapacity, queueFactory, src -> {
                List<Integer> result = new ArrayList<>(src);
                result.sort(comparator);
                return result;
            });
        }
    }

    public abstract static class AbstractCustomCapacityQueueTest {
        private final int maxCapacity;
        private final Supplier<ReservablePollingQueue<Integer>> queueFactory;
        private final Function<List<Integer>, List<Integer>> queueOrderer;

        public AbstractCustomCapacityQueueTest(
                int maxCapacity,
                Supplier<ReservablePollingQueue<Integer>> queueFactory,
                Function<List<Integer>, List<Integer>> queueOrderer) {

            this.maxCapacity = maxCapacity;
            this.queueFactory = queueFactory;
            this.queueOrderer = queueOrderer;
        }

        @Test
        public void testToString() {
            ReservablePollingQueue<Integer> queue = queueFactory.get();
            assertNotNull("queue", queue.toString());
        }

        private static Integer tryLenientToInt(String str) {
            StringBuilder normStr = new StringBuilder(str.length());
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if ((ch >= '0' && ch <= '9') || ch == '-') {
                    normStr.append(ch);
                }
            }

            if (normStr.length() == 0) {
                return null;
            }

            return Integer.parseInt(normStr.toString());
        }

        @Test
        public void testToStringContainsAllElementsOfTheQueue() {
            assumeTrue(maxCapacity > 0);

            ReservablePollingQueue<Integer> queue = queueFactory.get();
            List<Integer> actualElements = generateTestList(maxCapacity);
            actualElements.forEach(queue::offer);

            String str = queue.toString();
            Set<Integer> toStringElements = CollectionsEx.newHashSet(maxCapacity);
            for (String part : str.split("[^0-9\\-]+")) {
                Integer element = tryLenientToInt(part);
                if (element != null) {
                    toStringElements.add(element);
                }
            }
            assertEquals(new HashSet<>(actualElements), toStringElements);
        }

        private void testSerializableManyElements(List<Integer> values) {
            List<Integer> expectedPolled = queueOrderer.apply(values);

            ReservablePollingQueue<Integer> queue = queueFactory.get();
            for (Integer value : values) {
                queue.offer(value);
            }

            ReservablePollingQueue<Integer> deserializedQueue = serializeDeserialize(queue);

            int valueCount = values.size();
            List<Integer> actual = new ArrayList<>(valueCount);
            for (int i = 0; i < valueCount; i++) {
                actual.add(deserializedQueue.poll());
            }

            assertEquals(expectedPolled, actual);
        }

        @Test
        public void testSerializableSingleElement() {
            assumeTrue(maxCapacity >= 1);
            testSerializableManyElements(Arrays.asList(34));
        }

        @Test
        public void testSerializableTwoElements() {
            assumeTrue(maxCapacity >= 2);
            testSerializableManyElements(Arrays.asList(34, 46));
        }

        @Test
        public void testSerializableFullQueue() {
            assumeTrue(maxCapacity > 2);
            testSerializableManyElements(generateTestList(maxCapacity));
        }

        @Test
        public void testOfferSingleThenClear() {
            assumeTrue(maxCapacity > 0);

            ReservablePollingQueue<Integer> queue = queueFactory.get();

            assertTrue("offer. before", queue.offer(5));
            queue.clear();
            assertNull("poll. after", queue.poll());
        }

        @Test
        public void testOfferManyThenClear() {
            assumeTrue(maxCapacity > 1);

            ReservablePollingQueue<Integer> queue = queueFactory.get();

            int elements = Math.min(maxCapacity, 5);
            for (int i = 0; i < elements; i++) {
                assertTrue("offer-" + i + ". before", queue.offer(i));
            }
            queue.clear();
            assertNull("poll. after", queue.poll());
        }

        @Test
        public void testOfferThenPoll() {
            assumeTrue(maxCapacity > 0);

            ReservablePollingQueue<Integer> queue = queueFactory.get();

            assertTrue("isEmpty. before", queue.isEmpty());
            assertTrue("isEmptyAndNoReserved. before", queue.isEmptyAndNoReserved());
            assertTrue("offer", queue.offer(12));
            assertEquals("poll", 12, (Object) queue.poll());
            assertTrue("isEmpty after", queue.isEmpty());
            assertTrue("isEmptyAndNoReserved after", queue.isEmptyAndNoReserved());
        }

        @Test
        public void testSingleElementManipulations() {
            assumeTrue(maxCapacity >= 1);

            ReservablePollingQueue<Integer> queue = queueFactory.get();

            assertTrue("isEmpty. before", queue.isEmpty());
            assertTrue("isEmptyAndNoReserved. before", queue.isEmptyAndNoReserved());
            assertTrue("offer", queue.offer(12));

            assertFalse("isEmpty between offer and poll", queue.isEmpty());
            assertFalse("isEmptyAndNoReserved between offer and poll", queue.isEmptyAndNoReserved());

            ReservedElementRef<Integer> elementRef = queue.pollButKeepReserved();
            assertNotNull("elementRef", elementRef);
            assertTrue("isEmpty after", queue.isEmpty());
            assertFalse("isEmptyAndNoReserved after", queue.isEmptyAndNoReserved());

            assertEquals("elementRef.element", 12, (Object) elementRef.element());

            if (maxCapacity > 1) {
                assertTrue("offer", queue.offer(16));
                assertEquals("poll", 16, (Object) queue.poll());
            } else {
                assertFalse("offer", queue.offer(16));
                assertNull("poll", queue.poll());
            }

            elementRef.release();
            assertTrue("isEmpty final", queue.isEmpty());
            assertTrue("isEmptyAndNoReserved final", queue.isEmptyAndNoReserved());

            assertTrue("offer", queue.offer(17));
            assertEquals("poll", 17, (Object) queue.poll());
        }

        private void pollAllAndAssert(
                ReservablePollingQueue<Integer> queue,
                List<Integer> expected) {

            int elementCount = expected.size();
            List<Integer> polledElements = new ArrayList<>(elementCount);
            for (int i = 0; i < elementCount; i++) {
                polledElements.add(queue.poll());
            }
            assertNull("final poll", queue.poll());
            assertEquals(queueOrderer.apply(expected), polledElements);
        }

        @Test
        public void testPollOrder() {
            ReservablePollingQueue<Integer> queue = queueFactory.get();
            List<Integer> actualElements = generateTestList(maxCapacity);
            actualElements.forEach(queue::offer);

            pollAllAndAssert(queue, actualElements);
        }

        private List<Integer> removePolledElements(int polledCount, List<Integer> initialElements) {
            assertTrue(
                    "Test configuration error: Can't poll more elements than what was added",
                    polledCount <= initialElements.size()
            );

            List<Integer> expectedPollOrder = queueOrderer.apply(initialElements);

            Set<Integer> polled = new HashSet<>(expectedPollOrder.subList(0, polledCount));

            List<Integer> result = new ArrayList<>(initialElements.size() - polledCount);
            for (Integer element : initialElements) {
                if (!polled.contains(element)) {
                    result.add(element);
                }
            }
            return result;
        }

        @Test
        public void testDoubleReleaseIsNotDoubleCountedSingleCapacity() {
            assumeTrue(maxCapacity == 1);

            ReservablePollingQueue<Integer> queue = queueFactory.get();
            List<Integer> actualElements = generateTestList(maxCapacity);
            actualElements.forEach(queue::offer);

            ReservedElementRef<Integer> elementRef = queue.pollButKeepReserved();
            assertNotNull("elementRef", elementRef);

            elementRef.release();
            elementRef.release();

            assertTrue("offer 1", queue.offer(5000));
            assertFalse("offer 2", queue.offer(5001));

            pollAllAndAssert(queue, Collections.singletonList(5000));
        }

        @Test
        public void testDoubleReleaseIsNotDoubleCounted() {
            assumeTrue(maxCapacity >= 2);

            ReservablePollingQueue<Integer> queue = queueFactory.get();
            List<Integer> actualElements = generateTestList(maxCapacity);
            actualElements.forEach(queue::offer);

            ReservedElementRef<Integer> elementRef = queue.pollButKeepReserved();
            assertNotNull("elementRef", elementRef);

            elementRef.release();
            elementRef.release();

            assertTrue("offer 1", queue.offer(5000));
            assertFalse("offer 2", queue.offer(5001));

            pollAllAndAssert(queue, CollectionsEx.viewConcatList(
                    removePolledElements(1, actualElements),
                    Collections.singletonList(5000)
            ));
        }

        @Test
        public void testPollWithoutOffer() {
            ReservablePollingQueue<Integer> queue = queueFactory.get();

            assertTrue("isEmpty", queue.isEmpty());
            assertTrue("isEmptyAndNoReserved", queue.isEmptyAndNoReserved());
            assertNull("poll", queue.poll());
            assertNull("poll", queue.pollButKeepReserved());
        }

        @Test
        public void testCannotAddToFullQueue() {
            ReservablePollingQueue<Integer> queue = queueFactory.get();
            List<Integer> actualElements = generateTestList(maxCapacity);
            actualElements.forEach(queue::offer);

            assertFalse("extra offer", queue.offer(5002));
            pollAllAndAssert(queue, actualElements);
        }
    }

    public abstract static class AbstractZeroCapacityQueueTest  extends AbstractCustomCapacityQueueTest {
        private final Supplier<ReservablePollingQueue<Integer>> queueFactory;

        public AbstractZeroCapacityQueueTest(Supplier<ReservablePollingQueue<Integer>> queueFactory) {
            super(0, queueFactory, src -> Collections.emptyList());
            this.queueFactory = queueFactory;
        }

        @Test
        public void testSerializationKeepsIdentity() {
            ReservablePollingQueue<Integer> queue = queueFactory.get();
            assertSame("Must keep singleton property", queue, serializeDeserialize(queue));
        }

        @Test
        public void testClearZeroCapacity() {
            ReservablePollingQueue<Integer> queue = queueFactory.get();

            assertFalse("offer-before-clear", queue.offer(1));

            queue.clear();

            assertFalse("offer-after-clear", queue.offer(2));
            assertNull("poll-after-clear", queue.poll());
            assertSame("Must keep singleton property", queue, serializeDeserialize(queue));
        }
    }

    private static List<Integer> generateTestList(int numberOfElements) {
        return IntStream
                .range(0, numberOfElements)
                .mapToObj(value -> 2 * value > numberOfElements ? 2 * value : -2 * value + 1)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> ReservablePollingQueue<T> serializeDeserialize(ReservablePollingQueue<T> queue) {
        try {
            return (ReservablePollingQueue<T>) deserializeObject(serializeObject(queue));
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(ReservablePollingQueues.class);
    }
}
