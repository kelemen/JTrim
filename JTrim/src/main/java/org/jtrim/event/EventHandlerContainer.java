/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.event;

import java.util.Collection;

/**
 *
 * @author Kelemen Attila
 */
public interface EventHandlerContainer<ListenerType> {
    public void registerListener(ListenerType listener);
    public void removeListener(ListenerType listener);
    public Collection<ListenerType> getListeners();
    public int getListenerCount();

    public void onEvent(EventDispatcher<ListenerType> eventDispatcher);
}
