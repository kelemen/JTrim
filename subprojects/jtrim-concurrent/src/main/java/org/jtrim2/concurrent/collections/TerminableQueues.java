package org.jtrim2.concurrent.collections;

import org.jtrim2.collections.ReservablePollingQueue;

/**
 * Defines factory methods for {@link TerminableQueue}.
 *
 * <P>
 * This class cannot be instantiated or inherited.
 */
public final class TerminableQueues {
    /**
     * Returns a {@code TerminableQueue} using the given {@code ReservablePollingQueue} to store elements.
     * The returned {@code TerminableQueue} takes ownership of the passed {@code ReservablePollingQueue}, and
     * so the calling code may no longer use the passed queue.
     * <P>
     * Note: The passed queue does not need to be empty when passed, in which case of course, the returned
     * {@code TerminableQueue} will start out with the initial elements of the passed {@code ReservablePollingQueue}.
     *
     * @param <T> the type of the elements of the created queue
     * @param queue the backing queue which the returned {@code TerminableQueue} will use. Thus, the element
     *   order of the returned {@code TerminableQueue} is the same as the given {@code ReservablePollingQueue}
     *
     * @return a {@code TerminableQueue} using the given {@code ReservablePollingQueue} to store elements.
     *   This method never returns {@code null}.
     */
    public static <T> TerminableQueue<T> withWrappedQueue(ReservablePollingQueue<T> queue) {
        return new GenericTerminableQueue<>(queue);
    }

    private TerminableQueues() {
        throw new AssertionError();
    }
}
