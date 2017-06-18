package org.jtrim2.testutils.executor;

import org.jtrim2.utils.ExceptionHelper;

public interface MockTaskResult {
    public Throwable getThrownError();
    public int getCallCount();

    public default void verifySuccess() {
        ExceptionHelper.rethrowIfNotNull(getThrownError());

        int callCount = getCallCount();
        if (callCount != 1) {
            throw new AssertionError("Expected to be called exactly once but was called " + callCount + " times.");
        }
    }
}
