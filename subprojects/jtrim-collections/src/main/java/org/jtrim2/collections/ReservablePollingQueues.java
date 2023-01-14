package org.jtrim2.collections;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines factory methods for {@link ReservablePollingQueue}.
 *
 * <P>
 * This class cannot be instantiated or inherited.
 */
public final class ReservablePollingQueues {
    private static final ReservablePollingQueue<?> EMPTY_QUEUE = new ZeroCapacityPollingQueue<>();

    /**
     * Returns a queue to which no elements can be added, and will never contain
     * any elements.
     * <P>
     * The returned list is serializable.
     *
     * <h4>Thread safety</h4>
     * The returned queue is safe to be used by multiple threads concurrently.
     *
     * @param <T> the type of the hypothetical elements of the return queue
     * @return a zero capacity queue. This method never returns {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ReservablePollingQueue<T> zeroCapacityQueue() {
        // Safe because no actual instances will be referenced by this queue.
        return (ReservablePollingQueue<T>) EMPTY_QUEUE;
    }

    /**
     * Returns an empty queue with the given maximum capacity and FIFO retrieving order.
     * <P>
     * This method is effectively equivalent to calling
     * {@link #createFifoQueue(int, int) createFifoQueue(maxCapacity, maxCapacity)}.
     * <P>
     * The returned list is serializable.
     *
     * <h4>Thread safety</h4>
     * The returned queue may not be used concurrently from multiple threads. If such access
     * is needed, the access must be externally synchronized.
     *
     * @param <T> the type of the elements the returned queue can store
     * @param maxCapacity the maximum elements the returned queue may hold. This argument must be
     *   greater than or equal to zero.
     *
     * @return an empty FIFO queue with the given maximum capacity. This method never returns {@code null}.
     */
    public static <T> ReservablePollingQueue<T> createFifoQueue(int maxCapacity) {
        return createFifoQueue(maxCapacity, maxCapacity);
    }

    /**
     * Returns an empty queue with the given maximum capacity and FIFO retrieving order.
     * <P>
     * The returned list is serializable.
     *
     * <h4>Thread safety</h4>
     * The returned queue may not be used concurrently from multiple threads. If such access
     * is needed, the access must be externally synchronized.
     *
     * @param <T> the type of the elements the returned queue can store
     * @param maxCapacity the maximum elements the returned queue may hold. This argument must be
     *   greater than or equal to zero.
     * @param initialQueueCapacity an initial capacity the returned queue should reserve initially. Setting
     *   this parameter to any allowed value is only an optimization and carry no defined semantics. This
     *   argument must be greater than or equal to zero, but no greater than {@code maxCapacity}.
     *
     * @return an empty FIFO queue with the given maximum capacity. This method never returns {@code null}.
     */
    public static <T> ReservablePollingQueue<T> createFifoQueue(int maxCapacity, int initialQueueCapacity) {
        ExceptionHelper.checkArgumentInRange(maxCapacity, 0, Integer.MAX_VALUE, "maxCapacity");
        ExceptionHelper.checkArgumentInRange(initialQueueCapacity, 0, maxCapacity, "initialQueueCapacity");

        switch (maxCapacity) {
            case 0:
                return zeroCapacityQueue();
            case 1:
                return new SingleEntryPollingQueue<>();
            default:
                return new WrapperPollingQueue<>(new ArrayDeque<>(initialQueueCapacity), maxCapacity);
        }
    }

    /**
     * Returns an empty queue with the given maximum capacity and LIFO retrieving order.
     * <P>
     * This method is effectively equivalent to calling
     * {@link #createLifoQueue(int, int) createLifoQueue(maxCapacity, maxCapacity)}.
     * <P>
     * The returned list is serializable.
     *
     * <h4>Thread safety</h4>
     * The returned queue may not be used concurrently from multiple threads. If such access
     * is needed, the access must be externally synchronized.
     *
     * @param <T> the type of the elements the returned queue can store
     * @param maxCapacity the maximum elements the returned queue may hold. This argument must be
     *   greater than or equal to zero.
     *
     * @return an empty LIFO queue with the given maximum capacity. This method never returns {@code null}.
     */
    public static <T> ReservablePollingQueue<T> createLifoQueue(int maxCapacity) {
        return createLifoQueue(maxCapacity, maxCapacity);
    }

    /**
     * Returns an empty queue with the given maximum capacity and LIFO retrieving order.
     *
     * <h4>Thread safety</h4>
     * The returned queue may not be used concurrently from multiple threads. If such access
     * is needed, the access must be externally synchronized.
     * <P>
     * The returned list is serializable.
     *
     * @param <T> the type of the elements the returned queue can store
     * @param maxCapacity the maximum elements the returned queue may hold. This argument must be
     *   greater than or equal to zero.
     * @param initialQueueCapacity an initial capacity the returned queue should reserve initially. Setting
     *   this parameter to any allowed value is only an optimization and carry no defined semantics. This
     *   argument must be greater than or equal to zero, but no greater than {@code maxCapacity}.
     *
     * @return an empty LIFO queue with the given maximum capacity. This method never returns {@code null}.
     */
    public static <T> ReservablePollingQueue<T> createLifoQueue(int maxCapacity, int initialQueueCapacity) {
        ExceptionHelper.checkArgumentInRange(maxCapacity, 0, Integer.MAX_VALUE, "maxCapacity");
        ExceptionHelper.checkArgumentInRange(initialQueueCapacity, 0, maxCapacity, "initialQueueCapacity");

        switch (maxCapacity) {
            case 0:
                return zeroCapacityQueue();
            case 1:
                return new SingleEntryPollingQueue<>();
            default:
                return new WrapperPollingQueue<>(
                        Collections.asLifoQueue(new ArrayDeque<>(initialQueueCapacity)),
                        maxCapacity
                );
        }
    }

    /**
     * Returns an empty queue with the given maximum capacity and with the ordering defined by the given
     * {@code Comparator}. The head of the queue is the least element as defined by the comparator.
     * <P>
     * This method is effectively equivalent to calling
     * {@link #createOrderedQueue(int, int, Comparator) createOrderedQueue(maxCapacity, maxCapacity, comparator)}.
     * <P>
     * The returned list is serializable.
     *
     * <h4>Thread safety</h4>
     * The returned queue may not be used concurrently from multiple threads. If such access
     * is needed, the access must be externally synchronized.
     *
     * @param <T> the type of the elements the returned queue can store
     * @param maxCapacity the maximum elements the returned queue may hold. This argument must be
     *   greater than or equal to zero.
     * @param comparator the comparator defining the order of the returned queue. Can be {@code null}, which
     *   means using the natural ordering of the elements (i.e., the elements must implement
     *   {@link Comparable Comparable}.
     *
     * @return an empty LIFO queue with the given maximum capacity. This method never returns {@code null}.
     */
    public static <T> ReservablePollingQueue<T> createOrderedQueue(int maxCapacity, Comparator<? super T> comparator) {
        return createOrderedQueue(maxCapacity, maxCapacity, comparator);
    }

    /**
     * Returns an empty queue with the given maximum capacity and with the ordering defined by the given
     * {@code Comparator}. The head of the queue is the least element as defined by the comparator.
     * <P>
     * The returned list is serializable.
     *
     * <h4>Thread safety</h4>
     * The returned queue may not be used concurrently from multiple threads. If such access
     * is needed, the access must be externally synchronized.
     *
     * @param <T> the type of the elements the returned queue can store
     * @param maxCapacity the maximum elements the returned queue may hold. This argument must be
     *   greater than or equal to zero.
     * @param initialQueueCapacity an initial capacity the returned queue should reserve initially. Setting
     *   this parameter to any allowed value is only an optimization and carry no defined semantics. This
     *   argument must be greater than or equal to zero, but no greater than {@code maxCapacity}.
     * @param comparator the comparator defining the order of the returned queue. Can be {@code null}, which
     *   means using the natural ordering of the elements (i.e., the elements must implement
     *   {@link Comparable Comparable}).
     *
     * @return an empty LIFO queue with the given maximum capacity. This method never returns {@code null}.
     */
    public static <T> ReservablePollingQueue<T> createOrderedQueue(
            int maxCapacity,
            int initialQueueCapacity,
            Comparator<? super T> comparator) {

        ExceptionHelper.checkArgumentInRange(maxCapacity, 0, Integer.MAX_VALUE, "maxCapacity");
        ExceptionHelper.checkArgumentInRange(initialQueueCapacity, 0, maxCapacity, "initialQueueCapacity");

        switch (maxCapacity) {
            case 0:
                return zeroCapacityQueue();
            case 1:
                return new SingleEntryPollingQueue<>();
            default:
                return new WrapperPollingQueue<>(
                        new PriorityQueue<T>(Math.max(1, initialQueueCapacity), comparator),
                        maxCapacity
                );
        }
    }

    private ReservablePollingQueues() {
        throw new AssertionError();
    }
}
