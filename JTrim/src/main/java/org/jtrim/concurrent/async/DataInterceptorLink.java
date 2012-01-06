/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public abstract class DataInterceptorLink<DataType>
        implements AsyncDataLink<DataType> {

    private final AsyncDataLink<? extends DataType> wrappedLink;

    public DataInterceptorLink(AsyncDataLink<? extends DataType> wrappedLink) {
        this.wrappedLink = wrappedLink;
    }

    protected boolean onDataArrive(DataType newData) {
        return true;
    }

    protected boolean onDoneReceive(AsyncReport report) {
        return true;
    }

    @Override
    public final AsyncDataController getData(final AsyncDataListener<? super DataType> dataListener) {
        return wrappedLink.getData(new AsyncDataListener<DataType>(){
            @Override
            public boolean requireData() {
                return dataListener.requireData();
            }

            @Override
            public void onDataArrive(DataType newData) {
                if (DataInterceptorLink.this.onDataArrive(newData)) {
                    dataListener.onDataArrive(newData);
                }
            }

            @Override
            public void onDoneReceive(AsyncReport report) {
                if (DataInterceptorLink.this.onDoneReceive(report)) {
                    dataListener.onDoneReceive(report);
                }
            }
        });
    }
}
