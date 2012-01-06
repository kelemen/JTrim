/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public final class DataWithUid<DataType> {
    private final DataType data;
    private final Object id;

    public DataWithUid(DataType data) {
        this(data, data);
    }

    public DataWithUid(DataType data, Object id) {
        this.data = data;
        this.id = id;
    }

    public DataType getData() {
        return data;
    }

    public Object getID() {
        return id;
    }

    @Override
    public String toString() {
        return "ID=" + id + ", Data=" + data;
    }
}
