/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public final class AsyncReport {
    public static final AsyncReport SUCCESS = new AsyncReport(null, false);
    public static final AsyncReport CANCELED = new AsyncReport(null, true);

    public static AsyncReport getReport(Throwable exception,
            boolean canceled) {

        if (exception == null) {
            return canceled ? CANCELED : SUCCESS;
        }
        else {
            DataTransferException transferException;
            transferException = AsyncDatas.getTransferException(exception);
            return new AsyncReport(transferException, canceled);
        }
    }

    private final boolean canceled;
    private final DataTransferException exception;

    private AsyncReport(DataTransferException exception, boolean canceled) {
        this.exception = exception;
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public DataTransferException getException() {
        return exception;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(128);

        result.append("AsyncReport: ");
        if (canceled) {
            result.append("CANCELED");
        }

        if (exception != null) {
            if (canceled) {
                result.append(", ");
            }
            result.append("Exception=");
            result.append(exception);
        }

        return result.toString();
    }
}
