package org.jtrim2.concurrent.query;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.*;

public class CachedLinkRequestTest {
    @Test
    public void testCompleteConstructor() {
        Object queryArg = new Object();
        long cancelTimeout = 3;
        TimeUnit timeunit = TimeUnit.SECONDS;

        CachedLinkRequest<Object> request = new CachedLinkRequest<>(queryArg, cancelTimeout, timeunit);

        assertSame(queryArg, request.getQueryArg());
        assertEquals(cancelTimeout, request.getCacheExpire(TimeUnit.SECONDS));

        assertNotNull(request.toString());
    }

    @Test
    public void testArg1Constructor() {
        Object queryArg = new Object();

        CachedLinkRequest<Object> request = new CachedLinkRequest<>(queryArg);

        assertSame(queryArg, request.getQueryArg());
        assertEquals(1, request.getCacheExpire(TimeUnit.HOURS));

        assertNotNull(request.toString());
    }

    @Test
    public void testNullArg() {
        CachedLinkRequest<Object> request = new CachedLinkRequest<>(null);
        assertNull(request.getQueryArg());
    }

    private static <T> CachedLinkRequest<T> create(T queryArg, long cacheExpire, TimeUnit timeunit) {
        return new CachedLinkRequest<>(queryArg, cacheExpire, timeunit);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor1() {
        create(new Object(), -1L, TimeUnit.NANOSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(new Object(), 1L, null);
    }
}
