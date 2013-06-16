package org.jtrim.concurrent.async;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.jtrim.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class CachedAsyncDataQueryTest {
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

    private static <QueryArgType, DataType> CachedAsyncDataQuery<QueryArgType, DataType> create(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery,
            int maxCacheSize) {
        return new CachedAsyncDataQuery<>(wrappedQuery, maxCacheSize);
    }

    private static AsyncDataQuery<Object, Object> createDummyMockQuery() {
        AsyncDataQuery<Object, Object> query = mockQuery();

        stub(query.createDataLink(any())).toAnswer(new Answer<AsyncDataLink<Object>>() {
            @Override
            public AsyncDataLink<Object> answer(InvocationOnMock invocation) throws Throwable {
                return mockLink();
            }
        });
        return query;
    }

    @Test
    public void testTooManyLinksPromoteLastUsed() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 2);

        Object input1 = new Object();
        Object input2 = new Object();
        Object input3 = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input1));
        query.createDataLink(new CachedLinkRequest<>(input2));
        query.createDataLink(new CachedLinkRequest<>(input1));
        query.createDataLink(new CachedLinkRequest<>(input3));
        AsyncDataLink<Object> link4 = query.createDataLink(new CachedLinkRequest<>(input1));

        assertSame(link1, link4);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(3)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input1, input2, input3}, inputs.getAllValues().toArray());
    }

    @Test
    public void testTooManyLinks() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 2);

        Object input1 = new Object();
        Object input2 = new Object();
        Object input3 = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input1));
        query.createDataLink(new CachedLinkRequest<>(input2));
        query.createDataLink(new CachedLinkRequest<>(input3));
        AsyncDataLink<Object> link4 = query.createDataLink(new CachedLinkRequest<>(input1));

        assertNotSame(link1, link4);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(4)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input1, input2, input3, input1}, inputs.getAllValues().toArray());
    }

    @Test
    public void testUpdateExpireTime() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input));
        query.createDataLink(new CachedLinkRequest<>(input, 0L, TimeUnit.NANOSECONDS));
        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(input));

        assertNotSame(link1, link2);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(2)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input, input}, inputs.getAllValues().toArray());
    }

    @Test
    public void testExpire() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(
                new CachedLinkRequest<>(input, 0L, TimeUnit.NANOSECONDS));
        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(input));

        assertNotSame(link1, link2);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(2)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input, input}, inputs.getAllValues().toArray());
    }

    @Test
    public void testClearCache() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input));
        query.clearCache();
        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(input));

        assertNotSame(link1, link2);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(2)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input, input}, inputs.getAllValues().toArray());
    }

    @Test
    public void testRemovedFromCache() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input));
        query.removeFromCache(input);
        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(input));

        assertNotSame(link1, link2);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(2)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input, input}, inputs.getAllValues().toArray());
    }

    @Test
    public void testNotCached() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input1 = new Object();
        Object input2 = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input1));
        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(input2));

        assertNotSame(link1, link2);

        ArgumentCaptor<Object> inputs = ArgumentCaptor.forClass(Object.class);
        verify(wrappedQuery, times(2)).createDataLink(inputs.capture());
        verifyNoMoreInteractions(wrappedQuery);

        assertArrayEquals(new Object[]{input1, input2}, inputs.getAllValues().toArray());
    }

    private void testConcurrentCache(int concurrency) {
        assert concurrency > 0;

        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        final CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        final Object input = new Object();
        final Collection<AsyncDataLink<Object>> links = new ConcurrentLinkedQueue<>();

        final CountDownLatch syncLatch = new CountDownLatch(concurrency);
        Thread[] threads = new Thread[concurrency];
        try {
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        syncLatch.countDown();
                        try {
                            syncLatch.await();
                        } catch (InterruptedException ex) {
                        }
                        links.add(query.createDataLink(new CachedLinkRequest<>(input)));
                    }
                });
                threads[i].start();
            }
        } finally {
            Throwable toThrow = null;
            for (Thread thread: threads) {
                try {
                    if (thread != null) {
                        thread.join();
                    }
                } catch (Throwable ex) {
                    toThrow = ex;
                }
            }

            ExceptionHelper.rethrowIfNotNull(toThrow);
        }

        Object[] receivedLinks = links.toArray();
        assertEquals(concurrency, receivedLinks.length);
        for (Object link: receivedLinks) {
            assertSame(receivedLinks[0], link);
        }

        verify(wrappedQuery, atLeastOnce()).createDataLink(same(input));
        verifyNoMoreInteractions(wrappedQuery);
    }

    @Test(timeout = 20000)
    public void testConcurrentCache() {
        int concurrency = Runtime.getRuntime().availableProcessors() * 2;
        for (int i = 0; i < 100; i++) {
            testConcurrentCache(concurrency);
        }
    }

    @Test
    public void testSingleCacheLargeExpireTime() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(
                new CachedLinkRequest<>(input, Long.MAX_VALUE, TimeUnit.DAYS));
        AsyncDataLink<Object> link2 = query.createDataLink(
                new CachedLinkRequest<>(input, Long.MAX_VALUE, TimeUnit.DAYS));

        assertSame(link1, link2);

        verify(wrappedQuery).createDataLink(same(input));
        verifyNoMoreInteractions(wrappedQuery);
    }

    @Test
    public void testSingleCache() {
        AsyncDataQuery<Object, Object> wrappedQuery = createDummyMockQuery();
        CachedAsyncDataQuery<Object, Object> query = create(wrappedQuery, 100);

        Object input = new Object();
        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(input));
        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(input));

        assertSame(link1, link2);

        verify(wrappedQuery).createDataLink(same(input));
        verifyNoMoreInteractions(wrappedQuery);
    }

    /**
     * Test of toString method, of class CachedAsyncDataQuery.
     */
    @Test
    public void testToString() {
        CachedAsyncDataQuery<Object, Object> query = create(mockQuery(), 100);
        assertNotNull(query.toString());
    }
}
