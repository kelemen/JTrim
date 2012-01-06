/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import org.jtrim.cache.*;

/**
 *
 * @author Kelemen Attila
 */
public final class RefCachedData<DataType> {
    private final DataType data;
    private final VolatileReference<DataType> dataRef;

    public RefCachedData(DataType data, VolatileReference<DataType> dataRef) {
        // dataRef.get() == data should hold but we do not check it for
        // the sake of efficiency.
        this.data = data;
        this.dataRef = dataRef;
    }

    public RefCachedData(DataType data, ObjectCache cache, ReferenceType refType) {
        this.data = data;
        this.dataRef = cache != null
                ? cache.getReference(data, refType)
                : JavaRefObjectCache.INSTANCE.getReference(data, refType);
    }

    public DataType getData() {
        return data;
    }

    public VolatileReference<DataType> getDataRef() {
        return dataRef;
    }

    @Override
    public String toString() {
        return "RefCachedData{" + "Data=" + data + ", DataRef=" + dataRef + '}';
    }
}
