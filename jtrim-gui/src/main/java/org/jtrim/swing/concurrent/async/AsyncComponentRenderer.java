/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.Component;

/**
 * @deprecated Use {@link AsyncRenderer} instead.
 *
 * @author Kelemen Attila
 */
@Deprecated
public interface AsyncComponentRenderer {
    public static final int IDLE_PRIORITY = 0;
    public static final int LOW_PRIORITY = 50;
    public static final int DEFAULT_PRIORITY = 100;
    public static final int HIGH_PRIORITY = 1000;

    public abstract RenderingFuture renderComponent(
            int priority,
            Component component,
            ComponentRenderer renderer,
            RenderingParameters renderingParams,
            DrawingConnector drawingConnector);

    public abstract void shutdown();
}
