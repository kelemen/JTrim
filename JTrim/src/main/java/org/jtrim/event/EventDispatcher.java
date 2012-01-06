/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface EventDispatcher<EventListenerType> {
    void onEvent(EventListenerType eventListener);
}
