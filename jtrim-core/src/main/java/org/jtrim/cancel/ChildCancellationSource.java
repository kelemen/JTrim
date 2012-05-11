package org.jtrim.cancel;

/**
 *
 * @author Kelemen Attila
 */
public interface ChildCancellationSource extends CancellationSource {
    public CancellationToken getParentToken();
    public void detachFromParent();
}
