/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import org.jtrim.concurrent.async.AsyncDataState;

/**
 * @deprecated Used by the deprecated {@link AsyncComponentRenderer}.
 *
 * @author Kelemen Attila
 */
@Deprecated
public interface RenderingFuture {
    public boolean hasPainted();
    public boolean isRenderingDone();
    public long getRenderingTime();
    public AsyncDataState getAsyncDataState();
    public void cancel();
}
