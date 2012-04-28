/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import java.awt.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessState;
import org.jtrim.access.AccessStateListener;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ComponentAccessListener implements AccessStateListener<SwingRight> {

    private final ConcurrentMap<IdentityWrapper, AccessStateListener<SwingRight>> controllers;
    private volatile AccessStateListener<SwingRight> defaultController;

    public ComponentAccessListener() {
        this.controllers = new ConcurrentHashMap<>();
        this.defaultController = AutoComponentDisabler.INSTANCE;
    }

    public void setDefaultController(AccessStateListener<SwingRight> controller) {
        ExceptionHelper.checkNotNullArgument(controller, "controller");

        this.defaultController = controller;
    }

    public void ignoreComponent(Component component) {
        setController(component, DoNothingAccessListener.INSTANCE);
    }

    public void setController(Component component,
            AccessStateListener<SwingRight> controller) {

        ExceptionHelper.checkNotNullArgument(controller, "controller");

        controllers.put(new IdentityWrapper(component), controller);
    }

    public void removeController(Component component) {
        controllers.remove(new IdentityWrapper(component));
    }

    private AccessStateListener<SwingRight> getController(Component component) {
        AccessStateListener<SwingRight> controller;
        controller = controllers.get(new IdentityWrapper(component));
        if (controller == null) {
            controller = defaultController;
        }

        return controller;
    }

    @Override
    public void onEnterState(AccessManager<?, SwingRight> accessManager,
            SwingRight right, AccessState state) {
        getController(right.getComponent()).onEnterState(accessManager, right, state);
    }

    private static class IdentityWrapper {
        private final Component component;

        public IdentityWrapper(Component component) {
            this.component = component;
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (getClass() != obj.getClass()) {
                return false;
            }
            final IdentityWrapper other = (IdentityWrapper)obj;

            return other.component == component;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(component);
        }
    }
}
