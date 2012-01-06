/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.*;

/**
 *
 * @author Kelemen Attila
 */
public final class MultiAsyncDataState implements AsyncDataState {
    private final AsyncDataState[] subStates;

    public MultiAsyncDataState(AsyncDataState... states) {
        this.subStates = states.clone();
    }

    public MultiAsyncDataState(List<? extends AsyncDataState> states) {
        this.subStates = states.toArray(new AsyncDataState[states.size()]);
    }

    // read by LinkedAsyncDataLink
    AsyncDataState[] getSubStates() {
        return subStates;
    }

    public int getSubStateCount() {
        return subStates.length;
    }

    public AsyncDataState getSubState(int index) {
        return subStates[index];
    }

    public double getSubProgress(int index) {
        AsyncDataState state = subStates[index];
        return state != null ? state.getProgress() : 0.0;
    }

    @Override
    public double getProgress() {
        double result = 0.0;
        for (AsyncDataState state: subStates) {
            if (state != null) {
                result = result + state.getProgress();
            }
        }

        result = result / (double)subStates.length;
        return result;
    }

    @Override
    public String toString() {
        return "MultiAsyncDataState " + Arrays.toString(subStates);
    }
}
