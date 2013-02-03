package org.jtrim.access;

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
final class CombinedToken<IDType> implements AccessToken<IDType> {
    private final IDType accessID;
    private final AccessToken<?>[] tokens;

    private final TaskExecutor allContextExecutor;
    private final ContextAwareWrapper sharedContext;

    public CombinedToken(IDType id, AccessToken<?>... tokens) {
        ExceptionHelper.checkNotNullArgument(id, "id");
        ExceptionHelper.checkArgumentInRange(tokens.length, 1, Integer.MAX_VALUE, "tokens.length");

        this.accessID = id;
        this.tokens = tokens.clone();

        ExceptionHelper.checkNotNullElements(this.tokens, "tokens");

        this.sharedContext = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());

        TaskExecutor context = sharedContext;
        for (int i = tokens.length - 1; i >= 0; i--) {
            context = this.tokens[i].createExecutor(context);
        }
        this.allContextExecutor = context;
    }

    @Override
    public ListenerRef addReleaseListener(Runnable listener) {
        return AccessTokens.addReleaseAnyListener(ArraysEx.viewAsList(tokens), listener);
    }

    @Override
    public IDType getAccessID() {
        return accessID;
    }

    @Override
    public boolean isReleased() {
        for (AccessToken<?> token: tokens) {
            if (token.isReleased()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void release() {
        for (AccessToken<?> token: tokens) {
            token.release();
        }
    }

    @Override
    public void releaseAndCancel() {
        for (AccessToken<?> token: tokens) {
            token.releaseAndCancel();
        }
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

        // This check makes this method to be safely callable from within a
        // release listener of a subtoken.
        if (isReleased()) {
            return true;
        }

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
                allContextExecutor.execute(cancelToken, task, cleanupTask);
            }
        };
    }

    @Override
    public boolean isExecutingInThis() {
        return sharedContext.isExecutingInThis();
    }
}
