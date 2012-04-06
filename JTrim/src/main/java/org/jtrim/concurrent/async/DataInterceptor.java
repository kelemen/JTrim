package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public interface DataInterceptor<DataType> {
    public boolean onDataArrive(DataType newData);
    public void onDoneReceive(AsyncReport report);
}
