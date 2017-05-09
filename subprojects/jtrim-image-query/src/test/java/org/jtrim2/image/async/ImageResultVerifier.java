package org.jtrim2.image.async;

import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.image.ImageResult;

public interface ImageResultVerifier {
    public void verifyImageResult(long numberOfImages, ImageResult lastResult, AsyncReport report);
}
