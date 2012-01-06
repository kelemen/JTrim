/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;


/**
 *
 * @author Kelemen Attila
 */
public class AbstractDataController implements AsyncDataController {
    private volatile AsyncDataState state;

    public AbstractDataController() {
        this(null);
    }

    public AbstractDataController(AsyncDataState firstState) {
        this.state = firstState;
    }

    public void setDataState(AsyncDataState newState) {
        this.state = newState;
    }

    @Override
    public void controlData(Object controlArg) {
    }

    @Override
    public void cancel() {
    }

    @Override
    public AsyncDataState getDataState() {
        return state;
    }
}
