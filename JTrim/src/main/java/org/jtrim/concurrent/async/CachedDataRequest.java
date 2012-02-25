/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;

/**
 *
 * @author Kelemen Attila
 */
public final class CachedDataRequest<QueryArgType> {
    private static final int DEFAULT_TIMEOUT = 10 * 1000; // ms

    private final QueryArgType queryArg;
    private final ReferenceType refType;
    private final ObjectCache objectCache;
    private final long dataCancelTimeout;

    public CachedDataRequest(QueryArgType queryArg) {
        this(queryArg, ReferenceType.WeakRefType);
    }

    public CachedDataRequest(QueryArgType queryArg, ReferenceType refType) {
        this(queryArg, refType, JavaRefObjectCache.INSTANCE);
    }

    public CachedDataRequest(QueryArgType queryArg, ReferenceType refType, ObjectCache objectCache) {
        this(queryArg, refType, objectCache, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public CachedDataRequest(QueryArgType queryArg,
            ReferenceType refType, ObjectCache objectCache,
            long dataCancelTimeout, TimeUnit timeunit) {

        this.queryArg = queryArg;
        this.refType = refType;
        this.objectCache = objectCache;
        this.dataCancelTimeout = timeunit.toNanos(dataCancelTimeout);
    }

    public long getDataCancelTimeout(TimeUnit timeunit) {
        return timeunit.convert(dataCancelTimeout, TimeUnit.NANOSECONDS);
    }

    public ObjectCache getObjectCache() {
        return objectCache;
    }

    public QueryArgType getQueryArg() {
        return queryArg;
    }

    public ReferenceType getRefType() {
        return refType;
    }

    @Override
    public String toString() {
        return "CachedDataRequest{"
                + "Arg=" + queryArg
                + ", RefCreator=" + objectCache
                + ", RefType=" + refType
                + ", TimeOut=" + getDataCancelTimeout(TimeUnit.MILLISECONDS)
                + " ms}";
    }
}
