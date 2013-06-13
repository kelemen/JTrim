package org.jtrim.image.async;

import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.image.ImageResult;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageResultVerifier {
    public void verifyImageResult(long numberOfImages, ImageResult lastResult, AsyncReport report);
}
