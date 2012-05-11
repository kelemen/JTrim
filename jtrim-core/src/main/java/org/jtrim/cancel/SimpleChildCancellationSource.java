package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see Cancellation#createChildCancellationSource(CancellationToken)
 *
 * @author Kelemen Attila
 */
final class SimpleChildCancellationSource implements ChildCancellationSource {
    private final CancellationToken parentToken;
    private final CancellationSource cancelSource;
    private final AtomicReference<ListenerRef> cancelRef;

    public SimpleChildCancellationSource(CancellationToken parentToken) {
        ExceptionHelper.checkNotNullArgument(parentToken, "parentToken");
        this.parentToken = parentToken;
        this.cancelSource = Cancellation.createCancellationSource();
        this.cancelRef = new AtomicReference<>(null);
    }

    public void attachToParent() {
        ListenerRef currentRef = parentToken.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                cancelSource.getController().cancel();
            }
        });

        if (!cancelRef.compareAndSet(null, currentRef)) {
            currentRef.unregister();
        }
    }

    @Override
    public void detachFromParent() {
        ListenerRef currentRef = cancelRef.getAndSet(null);
        if (currentRef != null) {
            currentRef.unregister();
        }
    }

    @Override
    public CancellationToken getParentToken() {
        return parentToken;
    }

    @Override
    public CancellationController getController() {
        return cancelSource.getController();
    }

    @Override
    public CancellationToken getToken() {
        return cancelSource.getToken();
    }
}
