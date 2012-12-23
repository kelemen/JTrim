package org.jtrim.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class CachedLinkRequestTest {

    public CachedLinkRequestTest() {
    }

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