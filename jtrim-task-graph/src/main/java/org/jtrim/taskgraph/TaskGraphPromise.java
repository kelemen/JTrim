package org.jtrim.taskgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TaskGraphPromise<R> {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphPromise.class.getName());

    private final Impl<R> impl;

    public TaskGraphPromise() {
        this.impl = new Impl<>();
    }

    public boolean setFailure(Throwable error) {
        return impl.setResult(null, error);
    }

    public boolean setResult(R result) {
        return impl.setResult(result, null);
    }

    public TaskGraphFuture<R> getFuture() {
        return impl;
    }

    private static final class Impl<R> implements TaskGraphFuture<R> {
        private final Lock listenersLock;
        private volatile Collection<BiConsumer<? super R, ? super Throwable>> listeners;

        private R result;
        private Throwable error;

        public Impl() {
            this.result = null;
            this.error = null;
            this.listenersLock = new ReentrantLock();
            this.listeners = new ArrayList<>();
        }

        public boolean setResult(R result, Throwable error) {
            Collection<BiConsumer<? super R, ? super Throwable>> currentListeners;
            listenersLock.lock();
            try {
                currentListeners = listeners;
                if (currentListeners == null) {
                    return false;
                }

                listeners = null;
            } finally {
                listenersLock.unlock();
            }

            this.result = result;
            this.error = error;

            currentListeners.forEach((BiConsumer<? super R, ? super Throwable> handler) -> {
                handler.accept(result, error);
            });
            return true;
        }

        private boolean tryAddListener(BiConsumer<? super R, ? super Throwable> listener) {
            if (listeners == null) {
                return false;
            }

            listenersLock.lock();
            try {
                Collection<BiConsumer<? super R, ? super Throwable>> currentListeners = listeners;
                if (currentListeners == null) {
                    return false;
                }
                currentListeners.add(listener);
                return true;
            } finally {
                listenersLock.unlock();
            }
        }

        @Override
        public <R2> TaskGraphFuture<R2> onComplete(
                BiFunction<? super R, ? super Throwable, ? extends TaskGraphFuture<R2>> handler) {

            Impl<R2> delayedFuture = new Impl<>();

            BiConsumer<? super R, ? super Throwable> listener = (R currentResult, Throwable currentError) -> {
                try {
                    TaskGraphFuture<R2> newFuture = handler.apply(currentResult, currentError);
                    newFuture.onCompleteEnd(delayedFuture::setResult);
                } catch (Throwable ex) {
                    delayedFuture.setResult(null, ex);
                }
            };

            if (!tryAddListener(listener)) {
                return handler.apply(result, error);
            }

            return delayedFuture;
        }

        @Override
        public void onCompleteEnd(BiConsumer<? super R, ? super Throwable> handler) {
            BiConsumer<? super R, ? super Throwable> safeHandler = (R currentResult, Throwable currentError) -> {
                try {
                    handler.accept(currentResult, currentError);
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Error in terminating handler.", ex);
                }
            };

            if (!tryAddListener(safeHandler)) {
                handler.accept(result, error);
            }
        }
    }
}
