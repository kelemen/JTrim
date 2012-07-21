package org.jtrim.swing.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.concurrent.async.AsyncDataState;

/**
 *
 * @author Kelemen Attila
 */
public interface RenderingState {
    public boolean isRenderingFinished();
    public long getRenderingTime(TimeUnit unit);
    public AsyncDataState getAsyncDataState();
}
