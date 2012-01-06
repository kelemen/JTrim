package org.jtrim.concurrent.async;

public interface AsyncDataController {
    public void controlData(Object controlArg);
    public void cancel();
    public AsyncDataState getDataState();
}
