package org.jtrim.access;

/**
 *
 * @author Kelemen Attila
 */
public interface AccessChangeAction {
    public void onChangeAccess(AccessManager<?, ?> accessManager, boolean available);
}
