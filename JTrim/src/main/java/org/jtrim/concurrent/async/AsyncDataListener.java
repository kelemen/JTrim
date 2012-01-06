package org.jtrim.concurrent.async;

public interface AsyncDataListener<DataType> {
    public boolean requireData();
    public void onDataArrive(DataType data);
    public void onDoneReceive(AsyncReport report);
}
