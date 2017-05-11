package org.jtrim2.testutils.concurrent;

import org.jtrim2.executor.ManualTaskExecutor;

public final class ManualUiExecutorProvider extends AbstractUiExecutorProvider {
    private final ManualTaskExecutor manualExecutor;

    public ManualUiExecutorProvider(boolean eagerCancel) {
        this(new ManualTaskExecutor(eagerCancel));
    }

    private ManualUiExecutorProvider(ManualTaskExecutor manualExecutor) {
        super(manualExecutor);
        this.manualExecutor = manualExecutor;
    }

    public void executeAll() {
        while (executeCurrentlySubmitted() > 0) {
            // Loop until there is no more.
        }
    }

    public boolean tryExecuteOne() {
        return manualExecutor.tryExecuteOne();
    }

    public int executeCurrentlySubmitted() {
        return manualExecutor.executeCurrentlySubmitted();
    }
}
