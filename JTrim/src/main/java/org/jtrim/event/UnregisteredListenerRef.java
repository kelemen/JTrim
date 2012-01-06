/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public final class UnregisteredListenerRef<ListenerType>
implements
        ListenerRef<ListenerType> {

    private final ListenerType listener;

    public UnregisteredListenerRef(ListenerType listener) {
        this.listener = listener;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public void unregister() {
    }

    @Override
    public ListenerType getListener() {
        return listener;
    }
}
