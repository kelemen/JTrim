package org.jtrim2.concurrent.query;

import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cache.VolatileReference;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RefCachedDataTest {
    @SuppressWarnings("unchecked")
    private static <T> VolatileReference<T> mockVolatileReference() {
        return mock(VolatileReference.class);
    }

    @Test
    public void testConstructor1() {
        Object data = new Object();
        VolatileReference<Object> ref = mockVolatileReference();

        when(ref.get()).thenReturn(data);

        RefCachedData<Object> refCachedData = new RefCachedData<>(data, ref);
        assertSame(data, refCachedData.getData());
        assertSame(ref, refCachedData.getDataRef());

        assertNotNull(refCachedData.toString());
    }

    @Test
    public void testConstructor2() {
        Object data = new Object();
        ObjectCache cache = mock(ObjectCache.class);
        VolatileReference<Object> ref = mockVolatileReference();
        ReferenceType refType = ReferenceType.SoftRefType;

        when(cache.getReference(any(), any(ReferenceType.class))).thenReturn(ref);

        RefCachedData<Object> refCachedData = new RefCachedData<>(data, cache, refType);
        assertSame(data, refCachedData.getData());
        assertSame(ref, refCachedData.getDataRef());

        assertNotNull(refCachedData.toString());
    }

    @Test
    public void testConstructor2NullCache() {
        Object data = new Object();
        ReferenceType refType = ReferenceType.HardRefType;

        RefCachedData<Object> refCachedData = new RefCachedData<>(data, null, refType);
        assertSame(data, refCachedData.getData());
        assertSame(data, refCachedData.getDataRef().get());

        assertNotNull(refCachedData.toString());
    }
}
