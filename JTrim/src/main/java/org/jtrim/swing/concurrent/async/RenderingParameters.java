/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import org.jtrim.concurrent.async.*;

/**
 *
 * @author Kelemen Attila
 */
public final class RenderingParameters {
    private final Object userDefinedParams;
    private final AsyncDataLink<?> dataLink;

    public RenderingParameters(Object userDefinedParams) {
        this(userDefinedParams, null);
    }

    public RenderingParameters(Object userDefinedParams, AsyncDataLink<?> dataLink) {
        this.userDefinedParams = userDefinedParams;
        this.dataLink = dataLink;
    }

    public Object getUserDefinedParams() {
        return userDefinedParams;
    }

    public boolean hasBlockingData() {
        return dataLink != null;
    }

    public AsyncDataController getAsyncBlockingData(AsyncDataListener<Object> dataListener) {
        if (dataLink != null) {
            return dataLink.getData(dataListener);
        }
        else {
            return null;
        }
    }
}
