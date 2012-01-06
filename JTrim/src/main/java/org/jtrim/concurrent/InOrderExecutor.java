/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.*;

/**
 *
 * @author Kelemen Attila
 */
public final class InOrderExecutor implements Executor {
    private final Executor executor;
    private final InOrderScheduledSyncExecutor syncExecutor;

    public InOrderExecutor(Executor executor) {
        this.executor = executor;
        this.syncExecutor = new InOrderScheduledSyncExecutor();
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(new SyncTask(syncExecutor, command));
    }

    private static class SyncTask implements Runnable {
        private final Executor syncExecutor;
        private final Runnable command;

        public SyncTask(Executor syncExecutor, Runnable command) {
            this.syncExecutor = syncExecutor;
            this.command = command;
        }

        @Override
        public void run() {
            syncExecutor.execute(command);
        }
    }
}
