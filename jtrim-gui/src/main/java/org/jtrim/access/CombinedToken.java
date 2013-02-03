package org.jtrim.access;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.ArraysEx;
import org.jtrim.concurrent.*;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AccessTokens#combineTokens(AccessToken, AccessToken)
 *
 * @author Kelemen Attila
 */
final class CombinedToken<IDType1, IDType2>
implements
        AccessToken<MultiAccessID<IDType1, IDType2>> {

    private final MultiAccessID<IDType1, IDType2> accessID;

    private final AccessToken<IDType1> token1;
    private final AccessToken<IDType2> token2;
    private final Collection<AccessToken<?>> tokens;

    private final TaskExecutor allContextExecutor;
    private final ContextAwareWrapper sharedContext;

    public CombinedToken(
            AccessToken<IDType1> token1, AccessToken<IDType2> token2) {

        ExceptionHelper.checkNotNullArgument(token1, "token1");
        ExceptionHelper.checkNotNullArgument(token2, "token2");

        this.accessID = new MultiAccessID<>(
                token1.getAccessID(), token2.getAccessID());

        this.token1 = token1;
        this.token2 = token2;
        this.tokens = ArraysEx.viewAsList(new AccessToken<?>[]{token1, token2});

        this.sharedContext = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
        this.allContextExecutor
                = token1.createExecutor(token2.createExecutor(sharedContext));
    }

    @Override
    public ListenerRef addReleaseListener(Runnable listener) {
        return AccessTokens.addReleaseAnyListener(tokens, listener);
    }

    @Override
    public MultiAccessID<IDType1, IDType2> getAccessID() {
        return accessID;
    }

    @Override
    public boolean isReleased() {
        return token1.isReleased() || token2.isReleased();
    }

    @Override
    public void release() {
        token1.release();
        token2.release();
    }

    @Override
    public void releaseAndCancel() {
        token1.releaseAndCancel();
        token2.releaseAndCancel();
    }

    @Override
    public void awaitRelease(CancellationToken cancelToken) {
        while (!tryAwaitRelease(cancelToken, Long.MAX_VALUE, TimeUnit.DAYS)) {
            // Do nothing but loop
        }
    }

    @Override
    public boolean tryAwaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkArgumentInRange(timeout, 0, Long.MAX_VALUE, "timeout");
        ExceptionHelper.checkNotNullArgument(unit, "unit");

        final WaitableSignal releaseSignal = new WaitableSignal();
        ListenerRef listenerRef = addReleaseListener(new Runnable() {
            @Override
            public void run() {
                releaseSignal.signal();
            }
        });
        try {
            return releaseSignal.tryWaitSignal(cancelToken, timeout, unit);
        } finally {
            listenerRef.unregister();
        }
    }

    @Override
    public TaskExecutor createExecutor(final TaskExecutor executor) {
        return new TaskExecutor() {
            @Override
            public void execute(
                    CancellationToken cancelToken,
                    final CancelableTask task,
                    CleanupTask cleanupTask) {
                ExceptionHelper.checkNotNullArgument(task, "task");

                allContextExecutor.execute(cancelToken, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) throws Exception {
                        task.execute(cancelToken);
                    }
                }, cleanupTask);
            }
        };
    }

    @Override
    public boolean isExecutingInThis() {
        return sharedContext.isExecutingInThis();
    }
}
