package org.jtrim.access;

import org.jtrim.concurrent.IdempotentTask;
import org.jtrim.concurrent.Tasks;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AccessTokens#idempotentAccessListener(org.jtrim.access.AccessListener)
 * @author Kelemen Attila
 */
final class IdempotentAccessListener implements AccessListener {
    private final Runnable notifierTask;

    public IdempotentAccessListener(AccessListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.notifierTask = Tasks.runOnceTask(new Runnable() {
            @Override
            public void run() {
                onLostAccess();
            }
        }, false);
    }

    @Override
    public void onLostAccess() {
        notifierTask.run();
    }
}
