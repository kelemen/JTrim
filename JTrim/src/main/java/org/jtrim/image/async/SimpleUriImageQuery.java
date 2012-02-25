/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.async;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.image.ImageData;

/**
 *
 * @author Kelemen Attila
 */
public final class SimpleUriImageQuery
implements
        AsyncDataQuery<URI, ImageData> {

    private final ExecutorService executor;
    private final long minUpdateTime; // nanoseconds

    public SimpleUriImageQuery(ExecutorService executor, long minUpdateTime) {
        this.executor = executor;
        this.minUpdateTime = minUpdateTime;
    }

    @Override
    public AsyncDataLink<ImageData> createDataLink(URI arg) {
        return new SimpleUriImageLink(arg, executor, minUpdateTime);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Query images by URI. MinUpdateTime: ");
        result.append(TimeUnit.NANOSECONDS.toMillis(minUpdateTime));
        result.append(" ms\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);

        return result.toString();
    }


}
