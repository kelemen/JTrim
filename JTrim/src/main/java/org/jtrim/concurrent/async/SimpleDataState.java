/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.text.DecimalFormat;

/**
 *
 * @author Kelemen Attila
 */
public final class SimpleDataState implements AsyncDataState {
    private final String state;
    private final double progress;

    public SimpleDataState(String state, double progress) {
        this.state = state;
        this.progress = progress;
    }

    public String getState() {
        return state;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public String toString() {
        return new DecimalFormat("#.##").format(100.0 * progress)
                + "%: " + state;
    }
}
