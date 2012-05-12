package org.jtrim.concurrent.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class SafeCancelableListener<DataType> implements AsyncDataListener<DataType> {
    private final AsyncDataListener<DataType> safeListener;
    private final AtomicBoolean listenCancelCalled;
    private final AtomicReference<ListenerRef> cancelRef;

    public SafeCancelableListener(AsyncDataListener<? super DataType> safeListener) {
        this.safeListener = AsyncHelper.makeSafeListener(safeListener);
        this.cancelRef = new AtomicReference<>();
        this.listenCancelCalled = new AtomicBoolean(false);
    }

    public void listenForCancellation(
            CancellationToken cancelToken, final Runnable cancelTask) {
        if (listenCancelCalled.getAndSet(true)) {
            throw new IllegalStateException("Multiple cancellation listening attempts.");
        }

        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");

        ListenerRef ref = cancelToken.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                try {
                    if (cancelTask != null) {
                        cancelTask.run();
                    }
                } finally {
                    safeListener.onDoneReceive(AsyncReport.CANCELED);
                }
            }
        });

        if (!cancelRef.compareAndSet(null, ref)) {
            ref.unregister();
        }
    }

    @Override
    public boolean requireData() {
        return safeListener.requireData();
    }

    @Override
    public void onDataArrive(DataType data) {
        safeListener.onDataArrive(data);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        try {
            ListenerRef ref = cancelRef.getAndSet(UnregisteredListenerRef.INSTANCE);
            if (ref != null) {
                ref.unregister();
            }
        } finally {
            safeListener.onDoneReceive(report);
        }
    }
}
