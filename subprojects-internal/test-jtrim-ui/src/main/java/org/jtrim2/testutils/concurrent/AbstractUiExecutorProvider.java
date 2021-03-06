package org.jtrim2.testutils.concurrent;

import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.ui.concurrent.UiExecutorProvider;

public abstract class AbstractUiExecutorProvider implements UiExecutorProvider {
    private final ContextAwareTaskExecutor executor;

    public AbstractUiExecutorProvider(TaskExecutor uiExecutor) {
        this.executor = TaskExecutors.contextAware(TaskExecutors.inOrderSimpleExecutor(uiExecutor));
    }

    public final void runOnUi(Runnable task) {
        executor.execute(task);
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
