package org.jtrim2.testutils.executor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.utils.ExceptionHelper;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public interface MockTask {
    public void execute(boolean canceled);

    public static CancelableTask toTask(MockTask mockTask) {
        return (CancellationToken cancelToken) -> {
            mockTask.execute(cancelToken.isCanceled());
        };
    }

    public static MockTaskResult stubCancelableNonFailing(CancelableTask mockTask, CancelableTask action) {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicInteger callCount = new AtomicInteger(0);
        MockTaskResult result = new MockTaskResult() {
            @Override
            public Throwable getThrownError() {
                return errorRef.get();
            }

            @Override
            public int getCallCount() {
                return callCount.get();
            }
        };

        CancelableTask stubbedTask = Mockito.doAnswer((invocation) -> {
            try {
                action.execute((CancellationToken) invocation.getArguments()[0]);
                callCount.incrementAndGet();
            } catch (Throwable ex) {
                errorRef.set(ex);
            }
            return null;
        }).when(mockTask);

        try {
            stubbedTask.execute(ArgumentMatchers.any(CancellationToken.class));
        } catch (Exception ex) {
            throw ExceptionHelper.throwUnchecked(ex);
        }

        return result;
    }

    public static MockTaskResult stubNonFailing(MockTask mockTask, UnsafeMockTask action) {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicInteger callCount = new AtomicInteger(0);
        MockTaskResult result = new MockTaskResult() {
            @Override
            public Throwable getThrownError() {
                return errorRef.get();
            }

            @Override
            public int getCallCount() {
                return callCount.get();
            }
        };

        Mockito.doAnswer((invocation) -> {
            try {
                action.execute((Boolean) invocation.getArguments()[0]);
                callCount.incrementAndGet();
            } catch (Throwable ex) {
                errorRef.set(ex);
            }
            return null;
        }).when(mockTask).execute(ArgumentMatchers.anyBoolean());

        return result;
    }
}
