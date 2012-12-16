package org.jtrim.concurrent.async;

import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationSource;

/**
 *
 * @author Kelemen Attila
 */
public final class TestCancellationSource implements CancellationSource {
    private final CancellationSource cancelSource;
    private final TestCancellationToken cancelToken;

    public TestCancellationSource() {
        this.cancelSource = Cancellation.createCancellationSource();
        this.cancelToken = new TestCancellationToken(cancelSource.getToken());
    }

    @Override
    public CancellationController getController() {
        return cancelSource.getController();
    }

    @Override
    public TestCancellationToken getToken() {
        return cancelToken;
    }

    public void checkRegistrationCount(long expected) {
        cancelToken.checkRegistrationCount(expected);
    }

    public void checkNoRegistration() {
        cancelToken.checkNoRegistration();
    }
}
