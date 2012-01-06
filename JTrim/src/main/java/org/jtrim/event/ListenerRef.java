/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface ListenerRef<ListenerType> {
    public boolean isRegistered();
    public void unregister();

    public ListenerType getListener();
}
