/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import javax.swing.*;
import org.jtrim.access.*;
import org.jtrim.access.task.*;

import static org.jtrim.access.AccessTokens.*;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingRewExecutors {
    private SwingRewExecutors() {
        throw new AssertionError();
    }

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
            readAccess = new AccessResult<>(createSyncToken(requestID));
            writeAccess = accessManager.tryGetAccess(writeRequest);

            isAvailable = writeAccess.isAvailable();
            if (!isAvailable) {
                writeAccess.shutdown();

                if (!resolver.canContinue(readAccess, writeAccess)) {
                    return false;
                }

                unblockResults(writeAccess);
            }
        } while (!isAvailable);

        executor.executeNowAndRelease(task,
                readAccess.getAccessToken(),
                writeAccess.getAccessToken());

        return true;
    }
}
