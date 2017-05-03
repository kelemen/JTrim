package org.jtrim2.image.async;

import org.jtrim2.concurrent.async.AsyncReport;
import org.jtrim2.image.ImageResult;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageResultVerifier {
    public void verifyImageResult(long numberOfImages, ImageResult lastResult, AsyncReport report);
}
