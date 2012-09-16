package org.jtrim.swing.component;

import java.awt.image.BufferedImage;
import org.jtrim.concurrent.async.AsyncReport;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageRenderer<DataType, ResultType> {
    public RenderingResult<ResultType> startRendering(BufferedImage drawingSurface);
    public boolean willDoSignificantRender(DataType data);
    public RenderingResult<ResultType> render(DataType data, BufferedImage drawingSurface);
    public RenderingResult<ResultType> finishRendering(AsyncReport report, BufferedImage drawingSurface);
}
