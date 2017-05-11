package org.jtrim2.ui.concurrent;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;

public abstract class AbstractUiExecutorProvider implements UiExecutorProvider {
    private final ContextAwareTaskExecutor executor;

    public AbstractUiExecutorProvider(TaskExecutor uiExecutor) {
        this.executor = TaskExecutors.contextAware(TaskExecutors.inOrderSimpleExecutor(uiExecutor));
    }

    public final void runOnUi(Runnable task) {
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> task.run(), null);
    }

    public final boolean isInContext() {
        return executor.isExecutingInThis();
    }

    @Override
    public final TaskExecutor getSimpleExecutor(boolean alwaysExecuteLater) {
        return getStrictExecutor(alwaysExecuteLater);
    }

    @Override
    public final TaskExecutor getStrictExecutor(boolean alwaysExecuteLater) {
        return executor;
    }
}
