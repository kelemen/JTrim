package org.jtrim2.ui.concurrent;

import org.jtrim2.executor.SyncTaskExecutor;

public final class SyncUiExecutorProvider extends AbstractUiExecutorProvider {
    public SyncUiExecutorProvider() {
        super(new SyncTaskExecutor());
    }
}
