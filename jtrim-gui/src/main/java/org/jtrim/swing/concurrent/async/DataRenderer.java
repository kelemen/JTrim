package org.jtrim.swing.concurrent.async;

import org.jtrim.concurrent.async.AsyncReport;

/**
 *
 * @author Kelemen Attila
 */
public interface DataRenderer<DataType> {
    public boolean startRendering();
    public boolean willDoSignificantRender(DataType data);
    public boolean render(DataType data);
    public void finishRendering(AsyncReport report);
}
