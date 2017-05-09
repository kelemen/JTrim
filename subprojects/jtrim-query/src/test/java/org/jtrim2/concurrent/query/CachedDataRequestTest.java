package org.jtrim2.concurrent.query;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cache.JavaRefObjectCache;
import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CachedDataRequestTest {
    private static final long DEFAULT_TIMEOUT_SEC = 5;

    @Test
    public void testCompleteConstructor() {
        Object queryArg = new Object();
        ReferenceType refType = ReferenceType.SoftRefType;
        ObjectCache cache = mock(ObjectCache.class);
        long cancelTimeout = 3;
        TimeUnit timeunit = TimeUnit.SECONDS;

        CachedDataRequest<Object> request = new CachedDataRequest<>(
                queryArg, refType, cache, cancelTimeout, timeunit);

        assertSame(queryArg, request.getQueryArg());
        assertSame(refType, request.getRefType());
        assertSame(cache, request.getObjectCache());
        assertEquals(cancelTimeout, request.getDataCancelTimeout(TimeUnit.SECONDS));

        assertNotNull(request.toString());
    }

    @Test
    public void testArg1Constructor() {
        Object queryArg = new Object();

        CachedDataRequest<Object> request = new CachedDataRequest<>(queryArg);

        assertSame(queryArg, request.getQueryArg());
        assertSame(ReferenceType.WeakRefType, request.getRefType());
        assertSame(JavaRefObjectCache.INSTANCE, request.getObjectCache());
        assertEquals(DEFAULT_TIMEOUT_SEC, request.getDataCancelTimeout(TimeUnit.SECONDS));

        assertNotNull(request.toString());
    }

    @Test
    public void testArg2Constructor() {
        Object queryArg = new Object();
        ReferenceType refType = ReferenceType.SoftRefType;

        CachedDataRequest<Object> request = new CachedDataRequest<>(queryArg, refType);

        assertSame(queryArg, request.getQueryArg());
        assertSame(refType, request.getRefType());
        assertSame(JavaRefObjectCache.INSTANCE, request.getObjectCache());
        assertEquals(DEFAULT_TIMEOUT_SEC, request.getDataCancelTimeout(TimeUnit.SECONDS));

        assertNotNull(request.toString());
    }

    @Test
    public void testArg3Constructor() {
        Object queryArg = new Object();
        ReferenceType refType = ReferenceType.SoftRefType;
        ObjectCache cache = mock(ObjectCache.class);

        CachedDataRequest<Object> request = new CachedDataRequest<>(queryArg, refType, cache);

        assertSame(queryArg, request.getQueryArg());
        assertSame(refType, request.getRefType());
        assertSame(cache, request.getObjectCache());
        assertEquals(DEFAULT_TIMEOUT_SEC, request.getDataCancelTimeout(TimeUnit.SECONDS));

        assertNotNull(request.toString());
    }

    @Test
    public void testNullCacheConstructor() {
        Object queryArg = new Object();
        ReferenceType refType = ReferenceType.SoftRefType;

        CachedDataRequest<Object> request = new CachedDataRequest<>(queryArg, refType, null);

        assertSame(queryArg, request.getQueryArg());
        assertSame(refType, request.getRefType());
        assertSame(JavaRefObjectCache.INSTANCE, request.getObjectCache());
        assertEquals(DEFAULT_TIMEOUT_SEC, request.getDataCancelTimeout(TimeUnit.SECONDS));

        assertNotNull(request.toString());
    }
}
