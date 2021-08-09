package org.jtrim2.collections;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReservablePollingQueueTest {
    @Test
    public void testClearMany() {
        Queue<String> wrapped = new ArrayDeque<>(Arrays.asList("1", "2", "3"));
        ReservablePollingQueue<?> mock = new MockClearTestReservablePollingQueue<>(wrapped);
        mock.clear();
        assertEquals("remaining-size", 0, wrapped.size());
    }

    @Test
    public void testClearEmpty() {
        Queue<String> wrapped = new ArrayDeque<>();
        ReservablePollingQueue<?> mock = new MockClearTestReservablePollingQueue<>(wrapped);
        mock.clear();
        assertEquals("remaining-size", 0, wrapped.size());
    }

    @Test
    public void testPollAfterNull() {
        ReservablePollingQueue<?> mock = new MockTestReservablePollingQueue<>(null);

        assertNull("poll", mock.poll());
    }

    @Test
    public void testPollAfterNonNull() {
        Object element = new TestObject("Test-Queue-Element");
        AtomicInteger releaseCount = new AtomicInteger(0);

        ReservablePollingQueue<Object> mock = new MockTestReservablePollingQueue<>(new ReservedElementRef<Object>() {
            @Override
            public Object element() {
                return element;
            }

            @Override
            public void release() {
                releaseCount.incrementAndGet();
            }
        });

        assertSame(element, mock.poll());

        assertEquals("releaseCount", 1, releaseCount.get());
    }

    private class MockTestReservablePollingQueue<T> implements ReservablePollingQueue<T> {
        private final ReservedElementRef<T> elementRef;
        private final AtomicBoolean polled;

        public MockTestReservablePollingQueue(ReservedElementRef<T> elementRef) {
            this.elementRef = elementRef;
            this.polled = new AtomicBoolean(false);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean isEmptyAndNoReserved() {
            return false;
        }

        @Override
        public boolean offer(T entry) {
            return false;
        }

        @Override
        public ReservedElementRef<T> pollButKeepReserved() {
            if (!polled.compareAndSet(false, true)) {
                throw new AssertionError("May not poll multiple times.");
            }
            return elementRef;
        }
    }

    private class MockClearTestReservablePollingQueue<T> implements ReservablePollingQueue<T> {
        private final Queue<? extends T> elements;

        public MockClearTestReservablePollingQueue(Queue<? extends T> elements) {
            this.elements = elements;
        }

        @Override
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        @Override
        public boolean isEmptyAndNoReserved() {
            return elements.isEmpty();
        }

        @Override
        public boolean offer(T entry) {
            throw new AssertionError("Mock does not support offer.");
        }

        @Override
        public ReservedElementRef<T> pollButKeepReserved() {
            throw new AssertionError("Mock does not support reservation.");
        }

        @Override
        public T poll() {
            return elements.poll();
        }
    }
}
