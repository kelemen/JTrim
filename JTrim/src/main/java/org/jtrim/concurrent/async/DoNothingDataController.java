/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public enum DoNothingDataController implements AsyncDataController {
    INSTANCE;

    @Override
    public void controlData(Object controlArg) {
    }

    @Override
    public void cancel() {
    }

    @Override
    public AsyncDataState getDataState() {
        return null;
    }

    @Override
    public String toString() {
        return "DoNothingController";
    }
}
