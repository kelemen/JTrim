package org.jtrim2.collections;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReservablePollingQueueTest {

    @Test
    public void testPollAfterNull() {
        ReservablePollingQueue<?> mock = new MockReservablePollingQueue<>(null);

        assertNull("poll", mock.poll());
    }

    @Test
    public void testPollAfterNonNull() {
        Object element = new TestObject("Test-Queue-Element");
        AtomicInteger releaseCount = new AtomicInteger(0);

        ReservablePollingQueue<Object> mock = new MockReservablePollingQueue<>(new ReservedElementRef<Object>() {
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

    public class MockReservablePollingQueue<T> implements ReservablePollingQueue<T> {
        private final ReservedElementRef<T> elementRef;
        private final AtomicBoolean polled;

        public MockReservablePollingQueue(ReservedElementRef<T> elementRef) {
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
}
