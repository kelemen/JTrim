/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public final class LinkedDataControl {
    public final Object mainControlData;
    public final Object secondaryControlData;

    public LinkedDataControl(Object mainControlData, Object secondaryControlData) {
        this.mainControlData = mainControlData;
        this.secondaryControlData = secondaryControlData;
    }

    public Object getMainControlData() {
        return mainControlData;
    }

    public Object getSecondaryControlData() {
        return secondaryControlData;
    }

    @Override
    public String toString() {
        return "LinkedDataControl{"
                + "MainControlData=" + mainControlData
                + ", SecondaryControlData=" + secondaryControlData + '}';
    }
}
