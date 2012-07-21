package org.jtrim.swing.concurrent.async;

import org.jtrim.concurrent.async.AsyncReport;

/**
 *
 * @author Kelemen Attila
 */
public interface DataRenderer<DataType> {
    public void startRendering();
    public boolean render(DataType data);
    public void finishRendering(AsyncReport report);
}
