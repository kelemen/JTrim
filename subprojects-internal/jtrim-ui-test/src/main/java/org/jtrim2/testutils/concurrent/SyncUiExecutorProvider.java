package org.jtrim2.testutils.concurrent;

import org.jtrim2.executor.SyncTaskExecutor;

public final class SyncUiExecutorProvider extends AbstractUiExecutorProvider {
    public SyncUiExecutorProvider() {
        super(new SyncTaskExecutor());
    }
}
