/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.Objects;

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
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataWithUid<?> other = (DataWithUid<?>)obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return 469 + Objects.hashCode(this.id);
    }

    @Override
    public String toString() {
        return "ID=" + id + ", Data=" + data;
    }
}
