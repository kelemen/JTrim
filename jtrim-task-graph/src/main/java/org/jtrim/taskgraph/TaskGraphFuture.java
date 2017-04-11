package org.jtrim.taskgraph;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

public interface TaskGraphFuture<R> {
    public static <R> TaskGraphFuture<R> constFuture(R result, Throwable error) {
        return new ConstTaskGraphFuture<>(result, error);
    }

    public static <R> TaskGraphFuture<R> constSuccess(R result) {
        return new ConstTaskGraphFuture<>(result, null);
    }

    public static <R> TaskGraphFuture<R> constFailure(Throwable error) {
        ExceptionHelper.checkNotNullArgument(error, "error");
        return new ConstTaskGraphFuture<>(null, error);
    }

    public static <R> TaskGraphFuture<R> returnCaughtException(Throwable error) {
        ExceptionHelper.checkNotNullArgument(error, "error");
        if (error instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new ConstTaskGraphFuture<>(null, error);
    }

    public <R2> TaskGraphFuture<R2> onComplete(
            BiFunction<? super R, ? super Throwable, ? extends TaskGraphFuture<R2>> handler);

    public default void onCompleteEnd(BiConsumer<? super R, ? super Throwable> handler) {
        ExceptionHelper.checkNotNullArgument(handler, "handler");
        onComplete((result, error) -> {
            try {
                handler.accept(result, error);
                return constSuccess(null);
            } catch (Throwable ex) {
                Logger.getLogger(TaskGraphFuture.class.getName())
                        .log(Level.SEVERE, "Error in terminating handler.", ex);
                return returnCaughtException(ex);
            }
        });
    }

    public default <R2> TaskGraphFuture<R2> onCompleteConst(
            BiFunction<? super R, ? super Throwable, ? extends R2> handler) {
        ExceptionHelper.checkNotNullArgument(handler, "handler");
        return onComplete((result, error) -> {
            try {
                R2 newResult = handler.apply(result, error);
                return constSuccess(newResult);
            } catch (Throwable ex) {
                return returnCaughtException(ex);
            }
        });
    }

    public default <R2> TaskGraphFuture<R2> onSuccess(
            Function<? super R, ? extends TaskGraphFuture<R2>> handler) {
        ExceptionHelper.checkNotNullArgument(handler, "handler");
        return onComplete((result, error) -> {
            if (error == null) {
                return handler.apply(result);
            }
            else {
                return constFailure(error);
            }
        });
    }

    public default void onSuccessEnd(Consumer<? super R> handler) {
        ExceptionHelper.checkNotNullArgument(handler, "handler");
        onCompleteEnd((result, error) -> {
            if (error == null) {
                handler.accept(result);
            }
        });
    }

    public default <R2> TaskGraphFuture<R2> onFailure(
            Function<? super Throwable, ? extends TaskGraphFuture<R2>> handler) {
        ExceptionHelper.checkNotNullArgument(handler, "handler");
        return onComplete((result, error) -> {
            if (error == null) {
                return constSuccess(null);
            }
            else {
                return handler.apply(error);
            }
        });
    }

    public default void onFailureEnd(Consumer<? super Throwable> handler) {
        ExceptionHelper.checkNotNullArgument(handler, "handler");
        onCompleteEnd((result, error) -> {
            if (error != null) {
                handler.accept(error);
            }
        });
    }
}
