package org.jtrim2.testutils.cancel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.junit.Assert;

/**
 *
 * @author Kelemen Attila
 */
public final class TestCancellationToken implements CancellationToken {
    private final CancellationToken wrapped;
    private final AtomicLong regCount;

    public TestCancellationToken(CancellationToken wrapped) {
        Objects.requireNonNull(wrapped, "wrapped");
        this.wrapped = wrapped;
        this.regCount = new AtomicLong(0);
    }

    public void checkRegistrationCount(long expected) {
        Assert.assertEquals(expected, regCount.get());
    }

    public void checkNoRegistration() {
        checkRegistrationCount(0);
    }

    public long getRegCount() {
        return regCount.get();
    }

    @Override
    public ListenerRef addCancellationListener(Runnable listener) {
        final ListenerRef result = wrapped.addCancellationListener(listener);
        final Runnable unregTask = Tasks.runOnceTask(regCount::decrementAndGet, false);

        regCount.incrementAndGet();
        return new ListenerRef() {
            @Override
            public boolean isRegistered() {
                return result.isRegistered();
            }

            @Override
            public void unregister() {
                unregTask.run();
                result.unregister();
            }
        };
    }

    @Override
    public boolean isCanceled() {
        return wrapped.isCanceled();
    }

    @Override
    public void checkCanceled() {
        wrapped.checkCanceled();
    }
}
