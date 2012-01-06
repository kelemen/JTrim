/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import org.jtrim.concurrent.async.AsyncDataState;

/**
 *
 * @author Kelemen Attila
 */
public interface RenderingFuture {
    public boolean hasPainted();
    public boolean isRenderingDone();
    public long getRenderingTime();
    public AsyncDataState getAsyncDataState();
    public void cancel();
}
