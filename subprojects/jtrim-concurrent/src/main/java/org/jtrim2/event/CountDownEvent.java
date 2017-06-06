package org.jtrim2.event;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines event which triggers after a counter reaches zero. That is, the user might increase
 * or decrease the counter and once the counter is zero, an event is immediately triggered.
 * It is the user's responsibility to ensure that once the counter reaches zero, it will
 * not adjust the counter anymore. The event handler will never be notified more than once.
 *
 * <h3>Thread safety</h3>
 * This class can be safely used by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Decrementing the counter is not <I>synchronization transparent</I> as it may call
 * the event handler. However, other methods are  <I>synchronization transparent</I>.
 */
public final class CountDownEvent {
    private final AtomicInteger counter;
    private final Runnable callback;

    /**
     * Creates a new {@code CountDownEvent} with the given initial counter value and event handler.
     *
     * @param initialCount the initial value of the underlying counter. This value must be
     *   greater than or equal to 1.
     * @param callback the event handler to be called when the counter reaches zero.
     *   This argument cannot be {@code null}.
     */
    public CountDownEvent(int initialCount, Runnable callback) {
        ExceptionHelper.checkArgumentInRange(initialCount, 1, Integer.MAX_VALUE, "initialCount");
        Objects.requireNonNull(callback, "callback");

        this.counter = new AtomicInteger(initialCount);
        this.callback = Tasks.runOnceTask(callback);
    }

    /**
     * Decrements the counter by 1. If the counter reaches zero, the associated
     * event handler is called synchronously in this method.
     * <P>
     * This method may not be called once the counter have reached zero.
     *
     * @throws IllegalStateException maybe thrown if the counter already reached zero.
     */
    public void dec() {
        int newCounter = counter.decrementAndGet();
        if (newCounter < 0) {
            counter.incrementAndGet();
            throw new IllegalStateException("Decrementing counter below zero.");
        }
        if (newCounter == 0) {
            callback.run();
        }
    }

    /**
     * Increments the counter by 1.
     * <P>
     * This method may not be called once the counter have reached zero.
     */
    public void inc() {
        if (counter.getAndIncrement() <= 0) {
            counter.getAndDecrement();
            throw new IllegalStateException("Incrementig counter after it has reached zero.");
        }
    }

    /**
     * Returns the current value of the counter.
     *
     * @return the current value of the counter. If users of this class do not break the
     *   contract of this method, this method may never return a negative value.
     */
    public int getCounter() {
        return counter.get();
    }
}
