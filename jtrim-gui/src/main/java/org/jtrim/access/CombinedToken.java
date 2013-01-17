package org.jtrim.access;

import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AccessTokens#combineTokens(AccessToken, AccessToken)
 *
 * @author Kelemen Attila
 */
final class CombinedToken<IDType1, IDType2>
extends
        AbstractAccessToken<MultiAccessID<IDType1, IDType2>> {

    private final MultiAccessID<IDType1, IDType2> accessID;
    private final AccessToken<IDType1> token1;
    private final AccessToken<IDType2> token2;

    private final ContextAwareTaskExecutor protectExecutor;

    private volatile boolean released1;
    private volatile boolean released2;
    private final WaitableSignal releaseSignal;

    public CombinedToken(
            AccessToken<IDType1> token1, AccessToken<IDType2> token2) {

        ExceptionHelper.checkNotNullArgument(token1, "token1");
        ExceptionHelper.checkNotNullArgument(token2, "token2");

        this.accessID = new MultiAccessID<>(
                token1.getAccessID(), token2.getAccessID());

        this.token1 = token1;
        this.token2 = token2;

        this.protectExecutor = token1.createExecutor(
                token2.createExecutor(SyncTaskExecutor.getSimpleExecutor()));

        this.releaseSignal = new WaitableSignal();
        this.released1 = false;
        this.released2 = false;
    }

    public void listenForReleaseToken() {
        token1.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                released1 = true;
                if (released2) {
                    notifyReleaseListeners();
                }
            }
        });
        token2.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                released2 = true;
                if (released1) {
                    notifyReleaseListeners();
                }
            }
        });
    }

    @Override
    public MultiAccessID<IDType1, IDType2> getAccessID() {
        return accessID;
    }

    @Override
    public boolean isReleased() {
        return releaseSignal.isSignaled();
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
        releaseSignal.waitSignal(cancelToken);
    }

    @Override
    public boolean tryAwaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return releaseSignal.tryWaitSignal(cancelToken, timeout, unit);
    }

    @Override
    public ContextAwareTaskExecutor createExecutor(final TaskExecutor executor) {
        return new ContextAwareTaskExecutor() {
            @Override
            public boolean isExecutingInThis() {
                return protectExecutor.isExecutingInThis();
            }

            @Override
            public void execute(
                    CancellationToken cancelToken,
                    final CancelableTask task,
                    CleanupTask cleanupTask) {
                ExceptionHelper.checkNotNullArgument(task, "task");

                protectExecutor.execute(cancelToken, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) throws Exception {
                        task.execute(cancelToken);
                    }
                }, cleanupTask);
            }
        };
    }
}
