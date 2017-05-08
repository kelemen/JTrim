package org.jtrim2.access;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ArraysEx;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.ContextAwareWrapper;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AccessTokens#combineTokens(AccessToken, AccessToken)
 */
final class CombinedToken<IDType> implements AccessToken<IDType> {
    private final IDType accessID;
    private final AccessToken<?>[] tokens;

    private final TaskExecutor allContextExecutor;
    private final ContextAwareWrapper sharedContext;

    public CombinedToken(IDType id, AccessToken<?>... tokens) {
        Objects.requireNonNull(id, "id");
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
        Objects.requireNonNull(cancelToken, "cancelToken");
        ExceptionHelper.checkArgumentInRange(timeout, 0, Long.MAX_VALUE, "timeout");
        Objects.requireNonNull(unit, "unit");

        // This check makes this method to be safely callable from within a
        // release listener of a subtoken.
        if (isReleased()) {
            return true;
        }

        final WaitableSignal releaseSignal = new WaitableSignal();
        ListenerRef listenerRef = addReleaseListener(releaseSignal::signal);
        try {
            return releaseSignal.tryWaitSignal(cancelToken, timeout, unit);
        } finally {
            listenerRef.unregister();
        }
    }

    @Override
    public TaskExecutor createExecutor(final TaskExecutor executor) {
        return allContextExecutor::execute;
    }

    @Override
    public boolean isExecutingInThis() {
        return sharedContext.isExecutingInThis();
    }
}
