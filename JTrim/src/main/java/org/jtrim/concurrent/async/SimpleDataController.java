package org.jtrim.concurrent.async;


/**
 *
 * @author Kelemen Attila
 */
public final class SimpleDataController implements AsyncDataController {
    private volatile AsyncDataState state;

    public SimpleDataController() {
        this(null);
    }

    public SimpleDataController(AsyncDataState firstState) {
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
