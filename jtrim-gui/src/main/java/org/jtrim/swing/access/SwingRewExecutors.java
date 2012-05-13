package org.jtrim.swing.access;

import javax.swing.SwingUtilities;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResolver;
import org.jtrim.access.AccessResult;
import org.jtrim.access.task.RewTask;
import org.jtrim.access.task.RewTaskExecutor;
import org.jtrim.cancel.Cancellation;

import static org.jtrim.access.AccessTokens.createToken;
import static org.jtrim.access.AccessTokens.unblockResults;

/**
 * Contains convenience static methods to execute REW tasks of GUIs.
 * The methods will help retry acquiring access tokens if they are not
 * immediately available.
 * <P>
 * This class cannot be instantiated.
 *
 * @see RewTask
 * @see RewTaskExecutor
 * @author Kelemen Attila
 */
public final class SwingRewExecutors {
    /**
     * Tries to execute the specified REW task. This method first tries to
     * acquire the required write access token then if they are available it
     * will simply execute the specified REW task using the specified REW task
     * executor. In case the access token is not available this method
     * will call the provided user callback
     * ({@link AccessResolver#canContinue(AccessResult, AccessResult) AccessResolver.canContinue(AccessResult&lt;IDType&gt;, AccessResult&lt;IDType&gt;)})
     * and if the callback method returns {@code true} this method will shutdown
     * every access tokens conflicting the requests and retry
     * to acquire the access tokens (releasing any previously acquired tokens).
     * Note that the above steps may need to be repeated indefinitely because
     * new concurrent requests may have acquired conflicting tokens after they
     * were tried to be acquired.
     * <P>
     * The read access for the REW task is assumed to be always granted and the
     * read part of the task is executed synchronously in the current method
     * call.
     * <P>
     * In case this method cannot acquire the required access token and
     * the provided callback method returns {@code false} this method will
     * return immediately with {@code false}.
     * <P>
     * The acquired access tokens will be released as soon as possible.
     * <P>
     * <B>Note</B>: Since the read part of the REW task is executed in the
     * current call, this method usually must be called from the AWT event
     * dispatch thread.
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
            AccessRequest<IDType, ? extends RightType> writeRequest,
            AccessResolver<IDType> resolver) {

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method can only be called"
                    + " from the Event Dispatch Thread.");
        }

        AccessResult<IDType> readAccess;
        AccessResult<IDType> writeAccess;
        boolean isAvailable;

        IDType requestID = writeRequest.getRequestID();

        do {
            readAccess = new AccessResult<>(createToken(requestID));
            writeAccess = accessManager.tryGetAccess(writeRequest);

            isAvailable = writeAccess.isAvailable();
            if (!isAvailable) {
                writeAccess.release();

                if (!resolver.canContinue(readAccess, writeAccess)) {
                    return false;
                }

                // FIXME: This will probably throw an exception due to waiting
                //        on the EDT.
                unblockResults(Cancellation.UNCANCELABLE_TOKEN, writeAccess);
            }
        } while (!isAvailable);

        executor.executeNowAndRelease(task,
                readAccess.getAccessToken(),
                writeAccess.getAccessToken());

        return true;
    }

    private SwingRewExecutors() {
        throw new AssertionError();
    }
}
