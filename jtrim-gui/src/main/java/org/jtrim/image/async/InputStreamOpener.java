package org.jtrim.image.async;

import java.io.IOException;
import java.io.InputStream;
import org.jtrim.cancel.CancellationToken;

/**
 *
 * @author Kelemen Attila
 */
public interface InputStreamOpener {
    public InputStream openStream(CancellationToken cancelToken) throws IOException;
}
