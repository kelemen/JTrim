package org.jtrim2.concurrent.async;

import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cache.VolatileReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RefCachedDataTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @SuppressWarnings("unchecked")
    private static <T> VolatileReference<T> mockVolatileReference() {
        return mock(VolatileReference.class);
    }

    @Test
    public void testConstructor1() {
        Object data = new Object();
        VolatileReference<Object> ref = mockVolatileReference();

        stub(ref.get()).toReturn(data);

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

        stub(cache.getReference(any(), any(ReferenceType.class))).toReturn(ref);

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
