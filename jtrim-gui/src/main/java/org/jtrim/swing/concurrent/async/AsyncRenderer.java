package org.jtrim.swing.concurrent.async;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.async.AsyncDataLink;

/**
 *
 * @author Kelemen Attila
 */
public interface AsyncRenderer {
    public <DataType> RenderingState render(
            Object renderingKey,
            CancellationToken cancelToken,
            AsyncDataLink<DataType> dataLink,
            DataRenderer<? super DataType> renderer);
}
