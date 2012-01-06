/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import java.awt.Component;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingRight {
    private final Component component;
    private final Object subRight;

    public SwingRight(Component component) {
        this(component, null);
    }

    public SwingRight(Component component, Object subRight) {
        this.component = component;
        this.subRight = subRight;
    }

    public Component getComponent() {
        return component;
    }

    public Object getSubRight() {
        return subRight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SwingRight other = (SwingRight) obj;
        if (this.component != other.component && (this.component == null || !this.component.equals(other.component))) {
            return false;
        }
        if (this.subRight != other.subRight && (this.subRight == null || !this.subRight.equals(other.subRight))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.component != null ? this.component.hashCode() : 0);
        hash = 89 * hash + (this.subRight != null ? this.subRight.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "SwingRight{"
                + "Component=" + component
                + ", SubRight=" + subRight + '}';
    }

}
