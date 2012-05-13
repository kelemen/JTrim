package org.jtrim.access.task;

import org.jtrim.access.*;
import org.jtrim.cancel.Cancellation;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains convenience static methods to execute REW tasks.
 * The methods will help retry acquiring access tokens if they are not
 * immediately available.
 * <P>
 * This class cannot be instantiated.
 *
 * @see RewTask
 * @see RewTaskExecutor
 * @author Kelemen Attila
 */
public final class RewTaskExecutors {
    private RewTaskExecutors() {
        throw new AssertionError();
    }

    private static <IDType, RightType> boolean tryExecute(
            AccessManager<IDType, RightType> accessManager,
            RewTaskExecutor executor,
            RewTask<?, ?> task,
            AccessRequest<IDType, ? extends RightType> readRequest,
            AccessRequest<IDType, ? extends RightType> writeRequest,
            AccessResolver<IDType> resolver,
            boolean readNow) {

        ExceptionHelper.checkNotNullArgument(accessManager, "accessManager");
        ExceptionHelper.checkNotNullArgument(task, "task");
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(resolver, "readRequest");
        ExceptionHelper.checkNotNullArgument(resolver, "writeRequest");
        ExceptionHelper.checkNotNullArgument(resolver, "resolver");

        AccessResult<IDType> readAccess;
        AccessResult<IDType> writeAccess;
        boolean isAvailable;

        do {
            readAccess = accessManager.tryGetAccess(readRequest);
            writeAccess = accessManager.tryGetAccess(writeRequest);

            isAvailable = readAccess.isAvailable() && writeAccess.isAvailable();
            if (!isAvailable) {
                readAccess.release();
                writeAccess.release();

                if (!resolver.canContinue(readAccess, writeAccess)) {
                    return false;
                }

                AccessTokens.unblockResults(Cancellation.UNCANCELABLE_TOKEN, readAccess, writeAccess);
            }
        } while (!isAvailable);

        if (readNow) {
            executor.executeNowAndRelease(task,
                    readAccess.getAccessToken(),
                    writeAccess.getAccessToken());
        }
        else {
            executor.executeAndRelease(task,
                    readAccess.getAccessToken(),
                    writeAccess.getAccessToken());
        }

        return true;
    }

    /**
     * Tries to execute the specified REW task. This method first tries to
     * acquire the required read and write access tokens then if they are
     * available it will simply execute the specified REW task using the
     * specified REW task executor. In case any of the access tokens is not
     * available this method will call the provided user callback
     * ({@link AccessResolver#canContinue(AccessResult, AccessResult) AccessResolver.canContinue(AccessResult&lt;IDType&gt;, AccessResult&lt;IDType&gt;)})
     * and if the callback method returns {@code true} this method will shutdown
     * every access tokens conflicting the requests and retry
     * to acquire the access tokens (releasing any previously acquired tokens).
     * Note that the above steps may need to be repeated indefinitely because
     * new concurrent requests may have acquired conflicting tokens after they
     * were tried to be acquired.
     * <P>
     * In case this method cannot acquire the required access tokens and
     * the provided callback method returns {@code false} this method will
     * return immediately with {@code false}.
     * <P>
     * The acquired access tokens will be released as soon as possible because
     * this method relies on the
     * {@link RewTaskExecutor#executeAndRelease(RewTask, AccessToken, AccessToken) RewTaskExecutor.executeAndRelease(RewTask&lt;?, ?&gt;, AccessToken&lt;?&gt;, AccessToken&lt;?&gt;)}
     * method of the REW task executor.
     *
     * @param <IDType> the type of the
     *   {@link AccessRequest#getRequestID() request ID}
     * @param <RightType> the type of the right of the requests
     * @param accessManager the {@code AccessManager} from which the access
     *   tokens will be tried to be acquired. This argument cannot be
     *   {@code null}.
     * @param executor the REW task executor to which to REW task will be
     *   submitted to.
     * @param task the REW task to be executed. This argument cannot be
     *   {@code null}.
     * @param readRequest the rights to be acquired for the read access token.
     *   This argument cannot be {@code null}.
     * @param writeRequest the rights to be acquired for the write access token.
     *   This argument cannot be {@code null}.
     * @param resolver the interface to be used to determine if executing the
     *   task should be abandoned in case the requested access tokens are
     *   not available.
     * @return {@code true} if the specified REW task was successfully submitted
     *   to the specified REW task executor, {@code false} otherwise
     *
     * @throws NullPointerException throw if any of the arguments is
     *   {@code null}
     */
    public static <IDType, RightType> boolean tryExecute(
            AccessManager<IDType, RightType> accessManager,
            RewTaskExecutor executor,
            RewTask<?, ?> task,
            AccessRequest<IDType, ? extends RightType> readRequest,
            AccessRequest<IDType, ? extends RightType> writeRequest,
            AccessResolver<IDType> resolver) {

        return tryExecute(accessManager, executor, task, readRequest,
                writeRequest, resolver, false);
    }

    /**
     * Tries to execute the specified REW task executing the read part of the
     * task on the current call stack immediately. This method first tries to
     * acquire the required read and write access tokens then if they are
     * available it will simply execute the specified REW task using the
     * specified REW task executor. In case any of the access tokens is not
     * available this method will call the provided user callback
     * ({@link AccessResolver#canContinue(org.jtrim.access.AccessResult, org.jtrim.access.AccessResult) AccessResolver.canContinue(AccessResult<IDType>, AccessResult<IDType>)})
     * and if the callback method returns {@code true} this method will shutdown
     * every access tokens conflicting the requests and retry
     * to acquire the access tokens (releasing any previously acquired tokens).
     * Note that the above steps may need to be repeated indefinitely because
     * new concurrent requests may have acquired conflicting tokens after they
     * were tried to be acquired.
     * <P>
     * In case this method cannot acquire the required access tokens and
     * the provided callback method returns {@code false} this method will
     * return immediately with {@code false}.
     * <P>
     * The acquired access tokens will be released as soon as possible because
     * this method relies on the
     * {@link RewTaskExecutor#executeNowAndRelease(org.jtrim.access.task.RewTask, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) RewTaskExecutor.executeNowAndRelease(RewTask<?, ?>, AccessToken<?>, AccessToken<?>)}
     * method of the REW task executor.
     *
     * @param <IDType> the type of the
     *   {@link AccessRequest#getRequestID() request ID}
     * @param <RightType> the type of the right of the requests
     * @param accessManager the {@code AccessManager} from which the access
     *   tokens will be tried to be acquired. This argument cannot be
     *   {@code null}.
     * @param executor the REW task executor to which to REW task will be
     *   submitted to.
     * @param task the REW task to be executed. This argument cannot be
     *   {@code null}.
     * @param readRequest the rights to be acquired for the read access token.
     *   This argument cannot be {@code null}.
     * @param writeRequest the rights to be acquired for the write access token.
     *   This argument cannot be {@code null}.
     * @param resolver the interface to be used to determine if executing the
     *   task should be abandoned in case the requested access tokens are
     *   not available.
     * @return {@code true} if the specified REW task was successfully submitted
     *   to the specified REW task executor, {@code false} otherwise
     *
     * @throws NullPointerException throw if any of the arguments is
     *   {@code null}
     */
    public static <IDType, RightType> boolean tryExecuteNow(
            AccessManager<IDType, RightType> accessManager,
            RewTaskExecutor executor,
            RewTask<?, ?> task,
            AccessRequest<IDType, ? extends RightType> readRequest,
            AccessRequest<IDType, ? extends RightType> writeRequest,
            AccessResolver<IDType> resolver) {

        return tryExecute(accessManager, executor, task, readRequest,
                writeRequest, resolver, true);
    }
}
