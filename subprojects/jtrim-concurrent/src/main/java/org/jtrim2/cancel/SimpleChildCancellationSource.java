package org.jtrim2.cancel;

import java.util.Objects;

/**
 * @see Cancellation#createChildCancellationSource(CancellationToken)
 *
 * @author Kelemen Attila
 */
final class SimpleChildCancellationSource implements CancellationSource {
    private final CancellationToken token;
    private final CancellationController controller;

    public SimpleChildCancellationSource(CancellationToken parentToken) {
        Objects.requireNonNull(parentToken, "parentToken");

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        this.token = Cancellation.anyToken(parentToken, cancelSource.getToken());
        this.controller = cancelSource.getController();
    }

    @Override
    public CancellationController getController() {
        return controller;
    }

    @Override
    public CancellationToken getToken() {
        return token;
    }
}
