package org.jtrim.swing.component;

import java.awt.image.BufferedImage;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.async.AsyncReport;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageRenderer<DataType, ResultType> {
    public RenderingResult<ResultType> startRendering(
            CancellationToken cancelToken, BufferedImage drawingSurface);

    public boolean willDoSignificantRender(DataType data);

    public RenderingResult<ResultType> render(
            CancellationToken cancelToken, DataType data, BufferedImage drawingSurface);

    public RenderingResult<ResultType> finishRendering(
            CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface);
}
